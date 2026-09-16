#!/usr/bin/env python3
"""局域网设备管理系统（Python 命令行版）"""

import argparse
import csv
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

FIELDNAMES = [
    "device_id",
    "name",
    "device_type",
    "ip",
    "mac",
    "location",
    "status",
    "owner",
    "purchase_date",
]

DATA_FILE = Path(__file__).resolve().parent.parent / "data" / "lan_devices.csv"


class DeviceManager:
    def __init__(self, file_path: Path):
        self.file_path = file_path
        self.file_path.parent.mkdir(parents=True, exist_ok=True)
        if not self.file_path.exists():
            self._write_csv([])
        self.records = self.load_records()

    def load_records(self):
        try:
            with self.file_path.open("r", encoding="utf-8", newline="") as csvfile:
                reader = csv.DictReader(csvfile)
                rows = []
                for row in reader:
                    if row is None:
                        continue
                    clean = {field: (row.get(field, "") or "").strip() for field in FIELDNAMES}
                    if not any(clean.values()):
                        continue
                    rows.append(clean)
            return rows
        except FileNotFoundError:
            self._write_csv([])
            return []
        except csv.Error as exc:
            raise ValueError(f"CSV 数据格式错误: {exc}") from exc

    def save_records(self):
        self._write_csv(self.records)

    def _write_csv(self, rows):
        with self.file_path.open("w", encoding="utf-8", newline="") as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=FIELDNAMES)
            writer.writeheader()
            writer.writerows(rows)

    def add_device(self, device):
        device_id = device.get("device_id", "").strip()
        if not device_id:
            raise ValueError("设备编号不能为空")
        if any(item.get("device_id") == device_id for item in self.records):
            raise ValueError(f"设备编号 {device_id} 已存在")
        for field in FIELDNAMES:
            if field not in device:
                device[field] = ""
        clean = {field: str(device.get(field, "")).strip() for field in FIELDNAMES}
        self.records.append(clean)
        self.save_records()
        return clean

    def delete_device(self, device_id):
        before = len(self.records)
        self.records = [item for item in self.records if item.get("device_id") != device_id]
        if len(self.records) == before:
            raise ValueError(f"未找到设备编号: {device_id}")
        self.save_records()

    def update_device(self, device_id, update_map):
        for item in self.records:
            if item.get("device_id") == device_id:
                for key, value in update_map.items():
                    if key in FIELDNAMES:
                        item[key] = str(value).strip()
                    else:
                        raise ValueError(f"不支持的字段: {key}")
                self.save_records()
                return item
        raise ValueError(f"未找到设备编号: {device_id}")

    def query_records(self, keyword=None, field=None):
        if keyword is None:
            raise ValueError("查询关键字不能为空")
        keyword = str(keyword).strip()
        if not keyword:
            raise ValueError("查询关键字不能为空")

        results = self.records
        if field is not None:
            if field not in FIELDNAMES:
                raise ValueError(f"字段名错误，允许字段为: {', '.join(FIELDNAMES)}")
            results = [
                item for item in results if keyword.lower() in str(item.get(field, "")).lower()
            ]
        else:
            results = [
                item
                for item in results
                if any(keyword.lower() in str(item.get(field_name, "")).lower() for field_name in FIELDNAMES)
            ]
        return results

    def print_table(self, records):
        if not records:
            print("无记录")
            return

        headers = [
            ("device_id", 10),
            ("name", 12),
            ("device_type", 12),
            ("ip", 16),
            ("status", 10),
            ("location", 12),
        ]
        selected = [[str(item.get(name, ""))[:width].ljust(width) for name, width in headers] for item in records]

        print("-" * 100)
        print("| " + " | ".join(name.ljust(width) for name, width in headers) + " |")
        print("-" * 100)
        for row in selected:
            print("| " + " | ".join(row) + " |")
        print("-" * 100)


def build_parser():
    parser = argparse.ArgumentParser(description="局域网设备管理系统（命令行版）")
    subparsers = parser.add_subparsers(dest="command", required=False)

    list_parser = subparsers.add_parser("list", help="显示全部设备信息")
    list_parser.set_defaults(func=lambda args, manager: manager.print_table(manager.records))

    add_parser = subparsers.add_parser("add", help="添加设备")
    add_parser.add_argument("--id", dest="device_id", required=True)
    add_parser.add_argument("--name", required=True)
    add_parser.add_argument("--type", dest="device_type", required=True)
    add_parser.add_argument("--ip", required=True)
    add_parser.add_argument("--mac", required=True)
    add_parser.add_argument("--location", required=True)
    add_parser.add_argument("--status", required=True)
    add_parser.add_argument("--owner", required=True)
    add_parser.add_argument("--date", dest="purchase_date", required=True)
    add_parser.set_defaults(
        func=lambda args, manager: print(manager.add_device({
            "device_id": args.device_id,
            "name": args.name,
            "device_type": args.device_type,
            "ip": args.ip,
            "mac": args.mac,
            "location": args.location,
            "status": args.status,
            "owner": args.owner,
            "purchase_date": args.purchase_date,
        }))
    )

    delete_parser = subparsers.add_parser("delete", help="删除设备")
    delete_parser.add_argument("--id", dest="device_id", required=True)
    delete_parser.set_defaults(func=lambda args, manager: manager.delete_device(args.device_id))

    update_parser = subparsers.add_parser("update", help="修改设备信息")
    update_parser.add_argument("--id", dest="device_id", required=True)
    update_parser.add_argument("--name")
    update_parser.add_argument("--type", dest="device_type")
    update_parser.add_argument("--ip")
    update_parser.add_argument("--mac")
    update_parser.add_argument("--location")
    update_parser.add_argument("--status")
    update_parser.add_argument("--owner")
    update_parser.add_argument("--date", dest="purchase_date")
    update_parser.set_defaults(
        func=lambda args, manager: print(
            manager.update_device(args.device_id, {key: value for key, value in {
                "name": args.name,
                "device_type": args.device_type,
                "ip": args.ip,
                "mac": args.mac,
                "location": args.location,
                "status": args.status,
                "owner": args.owner,
                "purchase_date": args.purchase_date,
            }.items() if value is not None})
        )
    )

    query_parser = subparsers.add_parser("query", help="按关键字查询设备")
    query_parser.add_argument("--keyword", required=True)
    query_parser.add_argument("--field", choices=FIELDNAMES, default=None)
    query_parser.set_defaults(func=lambda args, manager: manager.print_table(manager.query_records(args.keyword, args.field)))

    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()
    if not hasattr(args, "func"):
        parser.print_help()
        return 0

    manager = DeviceManager(DATA_FILE)
    try:
        args.func(args, manager)
        return 0
    except ValueError as exc:
        print(f"错误: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
