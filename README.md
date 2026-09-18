# 局域网设备管理系统

这是一个面向课程设计的局域网设备管理系统，包含三种语言实现：

- Python 命令行版：使用 CSV 文件作为“数据库”，实现设备的增删改查与显示
- Java GUI 版：读取同一份 CSV 数据，以手绘表格和统计图展示设备信息
- C 语言版：与 Python 实现功能一致，适合课程中的 C 语言数据库管理系统设计

## 项目结构

- `data/lan_devices.csv`：共享数据文件，实际保存设备记录
- `python/lan_device_manager.py`：Python 版本主程序
- `java/src/LanDeviceViewer.java`：Java 图形界面程序
- `c/lan_device_manager.c`：C 语言版本
- `run_menu.bat`：Windows 双击启动 Python 菜单
- `run_menu.ps1`：PowerShell 启动脚本
- `run_java.bat`：Windows 双击启动 Java GUI
- `run_c.bat`：Windows 双击启动 C 版本

## 功能概述

### Python / C 版
- 设备新增
- 设备删除
- 设备修改
- 设备查询
- 设备列表展示
- IP 冲突检测
- MAC 格式校验与冲突检测
- 文件持久化保存到 CSV

### Java 版
- 读取共享 CSV 文件
- 以手工绘制的方式展示设备表格（不使用组件表格）
- 绘制折线图、柱状图和饼状图等统计示意图
- 支持新增、删除、修改、查询等设备操作
- 数据保存回 CSV 文件

## 运行方式

### Python

方式 1：双击启动
- `run_menu.bat`

方式 2：命令行
```bash
python python/lan_device_manager.py
```

### Java

先安装 JDK 21，然后运行：

```bash
javac -d out java/src/LanDeviceViewer.java
java -cp out LanDeviceViewer
```

或双击：
- `run_java.bat`

### C

在 Windows 下可直接运行：
- `run_c.bat`

或使用 GCC 编译：
```bash
gcc -o c/lan_device_manager c/lan_device_manager.c
c/lan_device_manager.exe
```

## 数据文件

设备数据保存在：

- `data/lan_devices.csv`

表头包括：
- device_id
- name
- device_type
- ip
- mac
- status
- location

## 设计说明

本项目采用“共享 CSV 文件作为数据库”的思路，保证 Python、Java、C 三个版本之间使用同一份数据源，便于对比学习和课程演示。

- Python/C：负责 CRUD 操作与数据存储
- Java：负责图形展示与可视化分析
- 统一数据：保证三部分逻辑保持一致

## 运行环境

- Python 3.x
- JDK 21+
- GCC / MinGW（可选，运行 C 版本）
- Windows 10/11（脚本为主）

## 注意事项

- Windows 控制台中文输出可能受编码影响，需要使用 UTF-8 控制台或启动脚本设置编码
- Java GUI 使用 `Graphics2D` 手工绘制，不依赖组件表格或现成图表控件，符合课程要求

## 版权与说明

该项目仅用于课程学习与演示，欢迎在遵循开源要求的前提下进行二次开发与学习。

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
