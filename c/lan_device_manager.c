#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <locale.h>
#include <windows.h>

#define MAX_DEVICES 200
#define MAX_LINE 256

struct Device {
    char device_id[32];
    char name[64];
    char device_type[32];
    char ip[32];
    char mac[32];
    char status[32];
    char location[64];
};

static void set_utf8_console(void) {
    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);
    setlocale(LC_ALL, ".65001");
}

static int load_devices(struct Device devices[], const char *path, int *count) {
    FILE *fp = fopen(path, "r");
    char line[MAX_LINE];
    char *token;
    int index = 0;

    if (fp == NULL) {
        fp = fopen(path, "w+");
        if (fp == NULL) {
            printf("无法创建数据文件。\n");
            return 0;
        }
        fprintf(fp, "device_id,name,device_type,ip,mac,status,location\n");
        fclose(fp);
        fp = fopen(path, "r");
    }

    fgets(line, sizeof(line), fp);
    while (fgets(line, sizeof(line), fp) != NULL && index < MAX_DEVICES) {
        char buffer[MAX_LINE];
        strncpy(buffer, line, sizeof(buffer) - 1);
        buffer[sizeof(buffer) - 1] = '\0';

        token = strtok(buffer, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].device_id, sizeof(devices[index].device_id), "%s", token);

        token = strtok(NULL, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].name, sizeof(devices[index].name), "%s", token);

        token = strtok(NULL, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].device_type, sizeof(devices[index].device_type), "%s", token);

        token = strtok(NULL, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].ip, sizeof(devices[index].ip), "%s", token);

        token = strtok(NULL, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].mac, sizeof(devices[index].mac), "%s", token);

        token = strtok(NULL, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].status, sizeof(devices[index].status), "%s", token);

        token = strtok(NULL, ",\r\n");
        if (token == NULL) continue;
        snprintf(devices[index].location, sizeof(devices[index].location), "%s", token);

        index++;
    }

    fclose(fp);
    *count = index;
    return 1;
}

static int save_devices(struct Device devices[], int count, const char *path) {
    FILE *fp = fopen(path, "w");
    if (fp == NULL) {
        printf("无法写入数据文件。\n");
        return 0;
    }

    fprintf(fp, "device_id,name,device_type,ip,mac,status,location\n");
    for (int i = 0; i < count; i++) {
        fprintf(fp, "%s,%s,%s,%s,%s,%s,%s\n",
                devices[i].device_id,
                devices[i].name,
                devices[i].device_type,
                devices[i].ip,
                devices[i].mac,
                devices[i].status,
                devices[i].location);
    }

    fclose(fp);
    return 1;
}

static int validate_ipv4(const char *ip) {
    int a, b, c, d;
    if (sscanf(ip, "%d.%d.%d.%d", &a, &b, &c, &d) != 4) return 0;
    if (a < 0 || a > 255 || b < 0 || b > 255 || c < 0 || c > 255 || d < 0 || d > 255) return 0;
    return 1;
}

static int validate_mac(const char *mac) {
    int len = (int)strlen(mac);
    if (len != 17) return 0;
    for (int i = 0; i < len; i++) {
        if (i % 3 == 2) {
            if (mac[i] != '-') return 0;
        } else if (!((mac[i] >= '0' && mac[i] <= '9') || (mac[i] >= 'A' && mac[i] <= 'F') || (mac[i] >= 'a' && mac[i] <= 'f'))) {
            return 0;
        }
    }
    return 1;
}

static int has_ip_conflict(struct Device devices[], int count, const char *ip, int skip_index) {
    for (int i = 0; i < count; i++) {
        if (i == skip_index) continue;
        if (strcmp(devices[i].ip, ip) == 0) return 1;
    }
    return 0;
}

static int has_mac_conflict(struct Device devices[], int count, const char *mac, int skip_index) {
    for (int i = 0; i < count; i++) {
        if (i == skip_index) continue;
        if (strcmp(devices[i].mac, mac) == 0) return 1;
    }
    return 0;
}

static void add_device(struct Device devices[], int *count) {
    struct Device new_device;
    printf("请输入设备ID: ");
    scanf(" %31s", new_device.device_id);
    printf("请输入名称: ");
    scanf(" %63s", new_device.name);
    printf("请输入类型: ");
    scanf(" %31s", new_device.device_type);
    printf("请输入IP地址: ");
    scanf(" %31s", new_device.ip);
    printf("请输入MAC地址(XX-XX-XX-XX-XX-XX): ");
    scanf(" %31s", new_device.mac);
    printf("请输入状态: ");
    scanf(" %31s", new_device.status);
    printf("请输入位置: ");
    scanf(" %63s", new_device.location);

    if (!validate_ipv4(new_device.ip)) {
        printf("IP地址格式不正确。\n");
        return;
    }
    if (!validate_mac(new_device.mac)) {
        printf("MAC地址格式不正确。\n");
        return;
    }
    if (has_ip_conflict(devices, *count, new_device.ip, -1)) {
        printf("IP地址冲突：该 IP 已被其他设备使用。\n");
        return;
    }
    if (has_mac_conflict(devices, *count, new_device.mac, -1)) {
        printf("MAC地址冲突：该 MAC 已被其他设备使用。\n");
        return;
    }

    devices[*count] = new_device;
    (*count)++;
    printf("添加成功。\n");
}

static void delete_device(struct Device devices[], int *count) {
    char device_id[32];
    int index = -1;
    printf("请输入要删除的设备ID: ");
    scanf(" %31s", device_id);

    for (int i = 0; i < *count; i++) {
        if (strcmp(devices[i].device_id, device_id) == 0) {
            index = i;
            break;
        }
    }

    if (index == -1) {
        printf("未找到该设备。\n");
        return;
    }

    for (int i = index; i < *count - 1; i++) {
        devices[i] = devices[i + 1];
    }
    (*count)--;
    printf("删除成功。\n");
}

static void update_device(struct Device devices[], int count) {
    char device_id[32];
    int index = -1;
    printf("请输入要修改的设备ID: ");
    scanf(" %31s", device_id);

    for (int i = 0; i < count; i++) {
        if (strcmp(devices[i].device_id, device_id) == 0) {
            index = i;
            break;
        }
    }

    if (index == -1) {
        printf("未找到该设备。\n");
        return;
    }

    printf("请输入新的名称: ");
    scanf(" %63s", devices[index].name);
    printf("请输入新的类型: ");
    scanf(" %31s", devices[index].device_type);
    printf("请输入新的IP地址: ");
    scanf(" %31s", devices[index].ip);
    printf("请输入新的MAC地址(XX-XX-XX-XX-XX-XX): ");
    scanf(" %31s", devices[index].mac);
    printf("请输入新的状态: ");
    scanf(" %31s", devices[index].status);
    printf("请输入新的位置: ");
    scanf(" %63s", devices[index].location);

    if (!validate_ipv4(devices[index].ip)) {
        printf("IP地址格式不正确。\n");
        return;
    }
    if (!validate_mac(devices[index].mac)) {
        printf("MAC地址格式不正确。\n");
        return;
    }
    if (has_ip_conflict(devices, count, devices[index].ip, index)) {
        printf("IP地址冲突：该 IP 已被其他设备使用。\n");
        return;
    }
    if (has_mac_conflict(devices, count, devices[index].mac, index)) {
        printf("MAC地址冲突：该 MAC 已被其他设备使用。\n");
        return;
    }

    printf("修改成功。\n");
}

static void query_device(struct Device devices[], int count) {
    char keyword[64];
    printf("请输入查询关键字: ");
    scanf(" %63s", keyword);

    int found = 0;
    for (int i = 0; i < count; i++) {
        if (strstr(devices[i].device_id, keyword) != NULL ||
            strstr(devices[i].name, keyword) != NULL ||
            strstr(devices[i].device_type, keyword) != NULL ||
            strstr(devices[i].ip, keyword) != NULL ||
            strstr(devices[i].mac, keyword) != NULL ||
            strstr(devices[i].status, keyword) != NULL ||
            strstr(devices[i].location, keyword) != NULL) {
            printf("%s | %s | %s | %s | %s | %s | %s\n",
                   devices[i].device_id,
                   devices[i].name,
                   devices[i].device_type,
                   devices[i].ip,
                   devices[i].mac,
                   devices[i].status,
                   devices[i].location);
            found = 1;
        }
    }

    if (!found) {
        printf("没有找到匹配设备。\n");
    }
}

static void display_all_devices(struct Device devices[], int count) {
    printf("----------------------------------------------------------------------------------------------------\n");
    printf("| %-10s | %-12s | %-12s | %-16s | %-20s | %-10s | %-12s |\n",
           "device_id", "name", "device_type", "ip", "mac", "status", "location");
    printf("----------------------------------------------------------------------------------------------------\n");
    for (int i = 0; i < count; i++) {
        printf("| %-10s | %-12s | %-12s | %-16s | %-20s | %-10s | %-12s |\n",
               devices[i].device_id,
               devices[i].name,
               devices[i].device_type,
               devices[i].ip,
               devices[i].mac,
               devices[i].status,
               devices[i].location);
    }
    printf("----------------------------------------------------------------------------------------------------\n");
}

static void show_menu(void) {
    printf("\n===== 局域网设备管理系统 =====\n");
    printf("1. 添加设备\n");
    printf("2. 删除设备\n");
    printf("3. 修改设备\n");
    printf("4. 查询设备\n");
    printf("5. 显示全部设备\n");
    printf("0. 退出\n");
}

int main(void) {
    struct Device devices[MAX_DEVICES];
    int count = 0;
    int choice;
    const char *path = "data/lan_devices.csv";

    set_utf8_console();
    load_devices(devices, path, &count);

    while (1) {
        show_menu();
        printf("请选择功能：");
        if (scanf("%d", &choice) != 1) {
            printf("输入错误，请重新输入。\n");
            while (getchar() != '\n');
            continue;
        }

        switch (choice) {
            case 1:
                add_device(devices, &count);
                save_devices(devices, count, path);
                break;
            case 2:
                delete_device(devices, &count);
                save_devices(devices, count, path);
                break;
            case 3:
                update_device(devices, count);
                save_devices(devices, count, path);
                break;
            case 4:
                query_device(devices, count);
                break;
            case 5:
                display_all_devices(devices, count);
                break;
            case 0:
                printf("程序已退出。\n");
                return 0;
            default:
                printf("无效选项。\n");
                break;
        }
    }

    return 0;
}
