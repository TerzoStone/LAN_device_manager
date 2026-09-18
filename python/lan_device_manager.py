import csv
import os
import re
import sys
from typing import Dict, List, Optional


class DeviceManager:
    """Device manager using CSV as the persisted database."""

    def __init__(self, csv_path: str = "data/lan_devices.csv"):
        self.csv_path = csv_path
        self.ensure_file_exists()

    def ensure_file_exists(self):
        directory = os.path.dirname(self.csv_path)
        if directory and not os.path.exists(directory):
            os.makedirs(directory, exist_ok=True)
        if not os.path.exists(self.csv_path):
            with open(self.csv_path, "w", encoding="utf-8", newline="") as file:
                writer = csv.writer(file)
                writer.writerow([
                    "device_id",
                    "name",
                    "device_type",
                    "ip",
                    "mac",
                    "status",
                    "location",
                ])

    def load_devices(self) -> List[Dict[str, str]]:
        devices: List[Dict[str, str]] = []
        if not os.path.exists(self.csv_path):
            self.ensure_file_exists()

        try:
            with open(self.csv_path, "r", encoding="utf-8", newline="") as file:
                reader = csv.DictReader(file)
                for row in reader:
                    if not row:
                        continue
                    devices.append({
                        "device_id": (row.get("device_id") or "").strip(),
                        "name": (row.get("name") or "").strip(),
                        "device_type": (row.get("device_type") or "").strip(),
                        "ip": (row.get("ip") or "").strip(),
                        "mac": (row.get("mac") or "").strip(),
                        "status": (row.get("status") or "").strip(),
                        "location": (row.get("location") or "").strip(),
                    })
        except FileNotFoundError:
            self.ensure_file_exists()
        return devices

    def save_devices(self, devices: List[Dict[str, str]]):
        with open(self.csv_path, "w", encoding="utf-8", newline="") as file:
            writer = csv.writer(file)
            writer.writerow(["device_id", "name", "device_type", "ip", "mac", "status", "location"])
            for device in devices:
                writer.writerow([
                    device.get("device_id", ""),
                    device.get("name", ""),
                    device.get("device_type", ""),
                    device.get("ip", ""),
                    device.get("mac", ""),
                    device.get("status", ""),
                    device.get("location", ""),
                ])

    def validate_ipv4(self, ip: str) -> bool:
        pattern = r"^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$"
        return bool(re.fullmatch(pattern, ip))

    def normalize_mac(self, mac: str) -> str:
        sanitized = re.sub(r"[^0-9A-Fa-f]", "", mac)
        if len(sanitized) != 12:
            return ""
        chunks = [sanitized[i:i + 2] for i in range(0, 12, 2)]
        return "-".join(chunks).upper()

    def validate_mac(self, mac: str) -> bool:
        if not mac:
            return False
        normalized = self.normalize_mac(mac)
        return bool(normalized) and bool(re.fullmatch(r"(?:[0-9A-Fa-f]{2}-){5}[0-9A-Fa-f]{2}", normalized))

    def find_device(self, device_id: str) -> Optional[Dict[str, str]]:
        for device in self.load_devices():
            if device["device_id"].lower() == device_id.lower():
                return device
        return None

    def add_device(self, device: Dict[str, str]) -> str:
        devices = self.load_devices()
        if any(d["device_id"].lower() == device["device_id"].lower() for d in devices):
            return "设备ID已存在，不能重复添加。"
        if any(d["ip"] == device["ip"] for d in devices):
            return "IP地址冲突：该 IP 已被其他设备使用。"
        if any(d["mac"].upper() == device["mac"].upper() for d in devices):
            return "MAC地址冲突：该 MAC 已被其他设备使用。"
        if not self.validate_ipv4(device["ip"]):
            return "IP地址格式不正确，请输入合法 IPv4 地址。"
        if not self.validate_mac(device["mac"]):
            return "MAC地址格式不正确，请使用 XX-XX-XX-XX-XX-XX 格式。"
        devices.append(device)
        self.save_devices(devices)
        return "添加成功。"

    def delete_device(self, device_id: str) -> str:
        devices = self.load_devices()
        for index, device in enumerate(devices):
            if device["device_id"].lower() == device_id.lower():
                del devices[index]
                self.save_devices(devices)
                return f"设备 {device_id} 已删除。"
        return "未找到该设备。"

    def update_device(self, device_id: str, new_device: Dict[str, str]) -> str:
        devices = self.load_devices()
        for index, device in enumerate(devices):
            if device["device_id"].lower() == device_id.lower():
                if not self.validate_ipv4(new_device["ip"]):
                    return "IP地址格式不正确，请输入合法 IPv4 地址。"
                if not self.validate_mac(new_device["mac"]):
                    return "MAC地址格式不正确，请使用 XX-XX-XX-XX-XX-XX 格式。"
                for other in devices:
                    if other is device:
                        continue
                    if other["ip"] == new_device["ip"]:
                        return "IP地址冲突：该 IP 已被其他设备使用。"
                    if other["mac"].upper() == new_device["mac"].upper():
                        return "MAC地址冲突：该 MAC 已被其他设备使用。"
                devices[index] = new_device
                self.save_devices(devices)
                return "更新成功。"
        return "未找到该设备。"

    def query_device(self, keyword: str) -> List[Dict[str, str]]:
        devices = self.load_devices()
        result: List[Dict[str, str]] = []
        keyword_lower = keyword.lower()
        for device in devices:
            if (
                keyword_lower in device["device_id"].lower()
                or keyword_lower in device["name"].lower()
                or keyword_lower in device["device_type"].lower()
                or keyword_lower in device["ip"].lower()
                or keyword_lower in device["mac"].lower()
                or keyword_lower in device["status"].lower()
                or keyword_lower in device["location"].lower()
            ):
                result.append(device)
        return result

    def print_devices(self, devices: List[Dict[str, str]]):
        if not devices:
            print("没有找到设备记录。")
            return

        print("-" * 132)
        print(f"{'设备ID':<10} | {'名称':<12} | {'类型':<12} | {'IP':<16} | {'MAC':<20} | {'状态':<8} | {'位置':<12}")
        print("-" * 132)
        for d in devices:
            print(f"{d['device_id']:<10} | {d['name']:<12} | {d['device_type']:<12} | {d['ip']:<16} | {d['mac']:<20} | {d['status']:<8} | {d['location']:<12}")
        print("-" * 132)


def print_menu():
    print("\n===== 局域网设备管理系统 =====")
    print("1. 添加设备")
    print("2. 删除设备")
    print("3. 修改设备")
    print("4. 查询设备")
    print("5. 显示全部设备")
    print("0. 退出")


def input_device_data() -> Dict[str, str]:
    device = {
        "device_id": input("请输入设备ID: ").strip(),
        "name": input("请输入设备名称: ").strip(),
        "device_type": input("请输入设备类型: ").strip(),
        "ip": input("请输入IP地址: ").strip(),
        "mac": input("请输入MAC地址(XX-XX-XX-XX-XX-XX): ").strip(),
        "status": input("请输入状态(在线/离线/维护中): ").strip(),
        "location": input("请输入位置: ").strip(),
    }
    return device


def main_menu():
    while True:
        print_menu()
        choice = input("请选择功能：").strip()
        manager = DeviceManager()

        if choice == "1":
            device = input_device_data()
            print(manager.add_device(device))
        elif choice == "2":
            device_id = input("请输入要删除的设备ID: ").strip()
            print(manager.delete_device(device_id))
        elif choice == "3":
            device_id = input("请输入要修改的设备ID: ").strip()
            if manager.find_device(device_id) is None:
                print("未找到该设备。")
                continue
            new_device = input_device_data()
            print(manager.update_device(device_id, new_device))
        elif choice == "4":
            keyword = input("请输入查询关键字: ").strip()
            result = manager.query_device(keyword)
            manager.print_devices(result)
        elif choice == "5":
            manager.print_devices(manager.load_devices())
        elif choice == "0":
            print("程序已退出。")
            exit(0)
        else:
            print("无效选项，请重试。")


if __name__ == "__main__":
    try:
        main_menu()
    except KeyboardInterrupt:
        print("\n程序已中断。")
        sys.exit(0)
