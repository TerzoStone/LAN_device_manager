import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class LanDeviceViewer extends JFrame {
    private static final String CSV_PATH = "data/lan_devices.csv";
    private final List<Device> devices = new ArrayList<>();
    private final JPanel mainPanel;
    private final JTextArea logArea;

    public LanDeviceViewer() {
        super("局域网设备管理系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 820);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(1200, 120));

        mainPanel.add(logScroll, BorderLayout.SOUTH);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton reloadButton = new JButton("刷新");
        reloadButton.addActionListener(e -> refreshDevices());

        JButton addButton = new JButton("新增");
        addButton.addActionListener(e -> showAddDialog());

        JButton deleteButton = new JButton("删除");
        deleteButton.addActionListener(e -> showDeleteDialog());

        JButton updateButton = new JButton("修改");
        updateButton.addActionListener(e -> showUpdateDialog());

        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> showQueryDialog());

        toolbar.add(reloadButton);
        toolbar.add(addButton);
        toolbar.add(deleteButton);
        toolbar.add(updateButton);
        toolbar.add(queryButton);
        mainPanel.add(toolbar, BorderLayout.NORTH);

        setContentPane(mainPanel);
        refreshDevices();
    }

    private void refreshDevices() {
        devices.clear();
        List<Device> loaded = Device.loadFromCsv(CSV_PATH);
        devices.addAll(loaded);
        logArea.setText("已加载 " + devices.size() + " 台设备。");
        repaint();
    }

    private void showAddDialog() {
        DeviceDialog dialog = new DeviceDialog(this, "新增设备", null);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Device device = dialog.getDevice();
            String result = DeviceManager.addDevice(device, CSV_PATH);
            logArea.setText(result);
            refreshDevices();
        }
    }

    private void showDeleteDialog() {
        String deviceId = JOptionPane.showInputDialog(this, "请输入要删除的设备ID:");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return;
        }
        String result = DeviceManager.deleteDevice(deviceId.trim(), CSV_PATH);
        logArea.setText(result);
        refreshDevices();
    }

    private void showUpdateDialog() {
        String deviceId = JOptionPane.showInputDialog(this, "请输入要修改的设备ID:");
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return;
        }
        Device existing = DeviceManager.findDevice(deviceId.trim(), devices);
        if (existing == null) {
            logArea.setText("未找到该设备。");
            return;
        }
        DeviceDialog dialog = new DeviceDialog(this, "修改设备", existing);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            Device updated = dialog.getDevice();
            String result = DeviceManager.updateDevice(deviceId.trim(), updated, CSV_PATH);
            logArea.setText(result);
            refreshDevices();
        }
    }

    private void showQueryDialog() {
        String keyword = JOptionPane.showInputDialog(this, "请输入查询关键字:");
        if (keyword == null) {
            return;
        }
        List<Device> result = DeviceManager.searchDevices(keyword, devices);
        if (result.isEmpty()) {
            logArea.setText("没有找到匹配设备。");
            return;
        }
        devices.clear();
        devices.addAll(result);
        logArea.setText("查询到 " + result.size() + " 台设备。");
        repaint();
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawTable(g);
        drawCharts(g);
        g.dispose();
    }

    private void drawTable(Graphics2D g) {
        int x = 30;
        int y = 130;
        int rowHeight = 30;
        int colWidth[] = {100, 140, 110, 130, 170, 90, 120};
        String[] headers = {"设备ID", "名称", "类型", "IP", "MAC", "状态", "位置"};

        g.setColor(Color.BLACK);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        for (int i = 0; i <= headers.length; i++) {
            int xPos = x;
            for (int j = 0; j < headers.length; j++) {
                if (i == 0) {
                    g.drawRect(xPos, y, colWidth[j], rowHeight);
                    g.drawString(headers[j], xPos + 10, y + 20);
                    xPos += colWidth[j];
                }
            }
        }

        for (int row = 0; row < Math.min(devices.size(), 12); row++) {
            Device device = devices.get(row);
            int currentX = x;
            String[] values = {
                device.getDeviceId(),
                device.getName(),
                device.getDeviceType(),
                device.getIp(),
                device.getMac(),
                device.getStatus(),
                device.getLocation()
            };
            for (int col = 0; col < values.length; col++) {
                g.drawRect(currentX, y + (row + 1) * rowHeight, colWidth[col], rowHeight);
                g.drawString(values[col], currentX + 8, y + (row + 1) * rowHeight + 20);
                currentX += colWidth[col];
            }
        }
    }

    private void drawCharts(Graphics2D g) {
        drawStatusChart(g, 670, 150, 420, 220);
        drawTypeChart(g, 670, 430, 420, 220);
    }

    private void drawStatusChart(Graphics2D g, int x, int y, int width, int height) {
        Map<String, Integer> counts = new HashMap<>();
        for (Device device : devices) {
            counts.put(device.getStatus(), counts.getOrDefault(device.getStatus(), 0) + 1);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        g.drawString("设备状态分布", x, y - 20);

        int barWidth = 65;
        int startX = x + 30;
        int maxCount = 1;
        for (Integer count : counts.values()) {
            maxCount = Math.max(maxCount, count);
        }

        int index = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int value = entry.getValue();
            int barHeight = (value * 120) / Math.max(maxCount, 1);
            int px = startX + index * 120;
            g.setColor(getColor(index));
            g.fillRect(px, y + 120 - barHeight, barWidth, barHeight);
            g.setColor(Color.BLACK);
            g.drawRect(px, y + 120 - barHeight, barWidth, barHeight);
            g.drawString(entry.getKey(), px, y + 145);
            g.drawString(String.valueOf(value), px + 15, y + 110 - barHeight);
            index++;
        }
    }

    private void drawTypeChart(Graphics2D g, int x, int y, int width, int height) {
        Map<String, Integer> counts = new HashMap<>();
        for (Device device : devices) {
            counts.put(device.getDeviceType(), counts.getOrDefault(device.getDeviceType(), 0) + 1);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
        g.drawString("设备类型分布", x, y - 20);

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        int currentAngle = 0;
        int index = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int angle = (int) Math.round(entry.getValue() * 360.0 / Math.max(total, 1));
            g.setColor(getColor(index));
            g.fillArc(x, y, width, height, currentAngle, angle);
            g.setColor(Color.BLACK);
            g.drawString(entry.getKey(), x + 20, y + height + 20 + index * 20);
            currentAngle += angle;
            index++;
        }
    }

    private Color getColor(int index) {
        Color[] palette = {
            new Color(52, 152, 219),
            new Color(46, 204, 113),
            new Color(155, 89, 182),
            new Color(241, 196, 15),
            new Color(231, 76, 60),
            new Color(52, 73, 94)
        };
        return palette[index % palette.length];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LanDeviceViewer viewer = new LanDeviceViewer();
            viewer.setVisible(true);
        });
    }
}

class Device {
    private final String deviceId;
    private final String name;
    private final String deviceType;
    private final String ip;
    private final String mac;
    private final String status;
    private final String location;

    public Device(String deviceId, String name, String deviceType, String ip, String mac, String status, String location) {
        this.deviceId = deviceId;
        this.name = name;
        this.deviceType = deviceType;
        this.ip = ip;
        this.mac = mac;
        this.status = status;
        this.location = location;
    }

    public static List<Device> loadFromCsv(String csvPath) {
        List<Device> devices = new ArrayList<>();
        Path path = Paths.get(csvPath);
        if (!Files.exists(path)) {
            return devices;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    devices.add(new Device(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return devices;
    }

    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getDeviceType() { return deviceType; }
    public String getIp() { return ip; }
    public String getMac() { return mac; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }

    public boolean matchesKeyword(String keyword) {
        String key = keyword.toLowerCase();
        return deviceId.toLowerCase().contains(key)
            || name.toLowerCase().contains(key)
            || deviceType.toLowerCase().contains(key)
            || ip.contains(key)
            || mac.toLowerCase().contains(key)
            || status.toLowerCase().contains(key)
            || location.toLowerCase().contains(key);
    }
}

class DeviceManager {
    public static Device findDevice(String deviceId, List<Device> devices) {
        for (Device device : devices) {
            if (device.getDeviceId().equalsIgnoreCase(deviceId.trim())) {
                return device;
            }
        }
        return null;
    }

    public static String addDevice(Device device, String csvPath) {
        List<Device> devices = Device.loadFromCsv(csvPath);
        for (Device existing : devices) {
            if (existing.getDeviceId().equalsIgnoreCase(device.getDeviceId())) {
                return "设备ID已存在。";
            }
            if (existing.getIp().equals(device.getIp())) {
                return "IP地址冲突：该 IP 已被使用。";
            }
            if (existing.getMac().equalsIgnoreCase(device.getMac())) {
                return "MAC地址冲突：该 MAC 已被使用。";
            }
        }
        devices.add(device);
        saveDevices(devices, csvPath);
        return "添加成功。";
    }

    public static String deleteDevice(String deviceId, String csvPath) {
        List<Device> devices = Device.loadFromCsv(csvPath);
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().equalsIgnoreCase(deviceId.trim())) {
                devices.remove(i);
                saveDevices(devices, csvPath);
                return "删除成功。";
            }
        }
        return "未找到该设备。";
    }

    public static String updateDevice(String deviceId, Device updated, String csvPath) {
        List<Device> devices = Device.loadFromCsv(csvPath);
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId().equalsIgnoreCase(deviceId.trim())) {
                for (Device other : devices) {
                    if (other == devices.get(i)) {
                        continue;
                    }
                    if (other.getIp().equals(updated.getIp())) {
                        return "IP地址冲突：该 IP 已被其他设备使用。";
                    }
                    if (other.getMac().equalsIgnoreCase(updated.getMac())) {
                        return "MAC地址冲突：该 MAC 已被其他设备使用。";
                    }
                }
                devices.set(i, updated);
                saveDevices(devices, csvPath);
                return "修改成功。";
            }
        }
        return "未找到该设备。";
    }

    public static List<Device> searchDevices(String keyword, List<Device> devices) {
        List<Device> result = new ArrayList<>();
        for (Device device : devices) {
            if (device.matchesKeyword(keyword)) {
                result.add(device);
            }
        }
        return result;
    }

    public static void saveDevices(List<Device> devices, String csvPath) {
        Path path = Paths.get(csvPath);
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("device_id,name,device_type,ip,mac,status,location\n");
            for (Device device : devices) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    device.getDeviceId(),
                    device.getName(),
                    device.getDeviceType(),
                    device.getIp(),
                    device.getMac(),
                    device.getStatus(),
                    device.getLocation()
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DeviceDialog extends JDialog {
    private final JTextField deviceIdField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField typeField = new JTextField();
    private final JTextField ipField = new JTextField();
    private final JTextField macField = new JTextField();
    private final JTextField statusField = new JTextField();
    private final JTextField locationField = new JTextField();
    private boolean confirmed = false;

    public DeviceDialog(Frame owner, String title, Device device) {
        super(owner, title, true);
        setLayout(new GridLayout(8, 2, 8, 8));
        setSize(420, 360);
        setLocationRelativeTo(owner);

        add(new JLabel("设备ID:"));
        add(deviceIdField);
        add(new JLabel("名称:"));
        add(nameField);
        add(new JLabel("类型:"));
        add(typeField);
        add(new JLabel("IP:"));
        add(ipField);
        add(new JLabel("MAC:"));
        add(macField);
        add(new JLabel("状态:"));
        add(statusField);
        add(new JLabel("位置:"));
        add(locationField);

        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());

        add(saveButton);
        add(cancelButton);

        if (device != null) {
            deviceIdField.setText(device.getDeviceId());
            nameField.setText(device.getName());
            typeField.setText(device.getDeviceType());
            ipField.setText(device.getIp());
            macField.setText(device.getMac());
            statusField.setText(device.getStatus());
            locationField.setText(device.getLocation());
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Device getDevice() {
        return new Device(
            deviceIdField.getText().trim(),
            nameField.getText().trim(),
            typeField.getText().trim(),
            ipField.getText().trim(),
            macField.getText().trim(),
            statusField.getText().trim(),
            locationField.getText().trim()
        );
    }
}
