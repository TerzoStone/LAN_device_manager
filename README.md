# 局域网设备管理系统

本项目分为两部分：

1. Python 命令行版：实现设备记录管理，数据以 CSV 文件保存，支持添加、删除、修改、查询和显示。
2. Java 图形界面版：读取同一份 CSV 文件，并使用 AWT/Swing 自行绘制表格和统计图表展示数据。

## 1. 项目结构

- `data/lan_devices.csv`：设备数据文件，实际保存的“数据库”。
- `python/lan_device_manager.py`：Python 命令行管理程序。
- `java/src/LanDeviceViewer.java`：Java 可视化界面程序。
- `README.md`：项目说明。

## 2. Python 部分：设备管理系统

### 2.1 功能说明

- 添加设备记录
- 删除设备记录
- 修改设备信息
- 按关键字查询
- 显示所有设备信息
- 数据保存到 CSV 文件，默认路径为 `data/lan_devices.csv`

### 2.2 运行方式

在项目根目录执行：

```bash
python python/lan_device_manager.py --help
```

常用示例：

```bash
python python/lan_device_manager.py list
python python/lan_device_manager.py add --id D009 --name "防火墙F1" --type "Firewall" --ip "192.168.7.1" --mac "00-11-22-33-44-DD" --location "机房C" --status "在线" --owner "安全组" --date 2024-06-11
python python/lan_device_manager.py update --id D009 --status "维护中" --location "机房D"
python python/lan_device_manager.py query --keyword "交换机"
python python/lan_device_manager.py delete --id D009
```

### 2.3 数据结构

每条设备记录包含：

- device_id：设备编号
- name：设备名称
- device_type：设备类型
- ip：IP 地址
- mac：MAC 地址
- location：安装位置
- status：状态（在线、离线、维护中等）
- owner：责任人
- purchase_date：采购日期

## 3. Java 部分：图形界面展示

Java 程序会读取 `data/lan_devices.csv` 文件，然后在窗口中手工绘制：

- 设备原始数据表格
- 设备状态分布柱状图
- 设备类型分布饼图

程序中没有使用任何现成表格组件，也没有使用预制图表组件，全部使用 `Graphics` / `Graphics2D` 的 `drawString`、`drawRect`、`fillRect`、`fillArc` 等方法手工绘制。

### 3.1 运行方式

在项目根目录执行：

```bash
javac -d out java/src/LanDeviceViewer.java
java -cp out LanDeviceViewer
```

注意：如果当前目录不是项目根目录，请先切到根目录后再执行上述命令。

## 4. 设计思路说明

- Python 负责“数据库操作”：新增、删除、修改、查询、保存。
- CSV 文件负责“数据持久化”。
- Java 负责“数据展示”：读取 CSV 文件并以图表形式展示设备数据。

这样设计符合“管理信息系统”的基本结构：

- 数据存储：CSV 文件
- 数据管理：Python 端 CRUD
- 数据展示：Java 端界面可视化

## 5. 适用场景

该项目适合用于学习：

- 文件型数据库的基本设计
- 记录的增删改查
- 模块化程序设计
- Java 图形界面绘制与数据显示
- 数据统计图表的自定义绘制

## 6. 说明

本项目以“模拟局域网设备管理系统”为主，重点体现信息管理系统的基本设计思想和工程实现方法，适合课程作业、课程设计或学习实践。
