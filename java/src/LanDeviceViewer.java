import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Java 图形界面主类：读取 CSV 文件中的设备数据，并用手工绘图方式展示原始表格和统计图
public class LanDeviceViewer extends JFrame {
    // 保存从 CSV 中解析出的设备数据记录（使用面向对象的 Device 类）
    private final List<Device> records;

    public LanDeviceViewer() {
        super("局域网设备管理系统 - 数据可视化");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900);
        setLocationRelativeTo(null);

        // 读取数据文件，路径与 Python 端保持一致
        File dataFile = new File("data/lan_devices.csv");
        records = readRecords(dataFile);
        setContentPane(new DevicePanel(records));
    }

    // 读取 CSV 文件并将每一行转换为 Device 对象
    private static List<Device> readRecords(File dataFile) {
        List<Device> list = new ArrayList<>();
        if (!dataFile.exists()) {
            return list;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String header = reader.readLine();
            if (header == null) {
                return list;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() < 9) {
                    continue;
                }
                Device device = Device.fromCsv(values);
                list.add(device);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 解析一行 CSV 数据，兼容字段中包含逗号或双引号的情况
    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }

    // 设备类：将 CSV 的记录表示成一个面向对象的实体，包含属性和常用行为
    private static class Device {
        private String deviceId;
        private String name;
        private String deviceType;
        private String ip;
        private String mac;
        private String location;
        private String status;
        private String owner;
        private String purchaseDate;

        // 无参构造器（用于手动构造）
        Device() {}

        // 全参构造器
        Device(String deviceId, String name, String deviceType, String ip, String mac, String location, String status, String owner, String purchaseDate) {
            this.deviceId = deviceId;
            this.name = name;
            this.deviceType = deviceType;
            this.ip = ip;
            this.mac = mac;
            this.location = location;
            this.status = status;
            this.owner = owner;
            this.purchaseDate = purchaseDate;
        }

        // 从 CSV 字段列表创建 Device 实例（工厂方法）
        static Device fromCsv(List<String> values) {
            Device d = new Device();
            d.deviceId = values.get(0).trim();
            d.name = values.get(1).trim();
            d.deviceType = values.get(2).trim();
            d.ip = values.get(3).trim();
            d.mac = values.get(4).trim();
            d.location = values.get(5).trim();
            d.status = values.get(6).trim();
            d.owner = values.get(7).trim();
            d.purchaseDate = values.get(8).trim();
            return d;
        }

        // 将对象序列化为 CSV 行（如果需要写回文件）
        String toCsvLine() {
            // 简单拼接，不处理复杂转义（数据中已假定不含换行）
            return String.join(",",
                    escape(deviceId), escape(name), escape(deviceType), escape(ip), escape(mac), escape(location), escape(status), escape(owner), escape(purchaseDate)
            );
        }

        private String escape(String s) {
            if (s == null) return "";
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                return "\"" + s.replace("\"", "\"\"") + "\"";
            }
            return s;
        }

        // 示例行为：判断是否在线
        boolean isOnline() {
            return "在线".equalsIgnoreCase(status);
        }

        // 简单的字段匹配方法，用于查询
        boolean matchesKeyword(String keyword) {
            if (keyword == null || keyword.isEmpty()) return false;
            String k = keyword.toLowerCase();
            return contains(deviceId, k) || contains(name, k) || contains(deviceType, k) || contains(ip, k) || contains(mac, k) || contains(location, k) || contains(status, k) || contains(owner, k) || contains(purchaseDate, k);
        }

        boolean matchesField(String field, String keyword) {
            if (field == null || keyword == null) return false;
            String k = keyword.toLowerCase();
            switch (field) {
                case "device_id": return contains(deviceId, k);
                case "name": return contains(name, k);
                case "device_type": return contains(deviceType, k);
                case "ip": return contains(ip, k);
                case "mac": return contains(mac, k);
                case "location": return contains(location, k);
                case "status": return contains(status, k);
                case "owner": return contains(owner, k);
                case "purchase_date": return contains(purchaseDate, k);
                default: return false;
            }
        }

        private boolean contains(String fieldValue, String keywordLower) {
            return fieldValue != null && fieldValue.toLowerCase().contains(keywordLower);
        }

        // 访问器（Getter）示例
        String getDeviceId() { return deviceId; }
        String getName() { return name; }
        String getDeviceType() { return deviceType; }
        String getIp() { return ip; }
        String getMac() { return mac; }
        String getLocation() { return location; }
        String getStatus() { return status; }
        String getOwner() { return owner; }
        String getPurchaseDate() { return purchaseDate; }
    }

    // 自定义绘图面板：利用 Graphics2D 直接绘制表格和统计图
    private static class DevicePanel extends JPanel {
        private final List<Device> records;
        private final Color[] palette = {new Color(72, 128, 255), new Color(94, 196, 110), new Color(255, 174, 66), new Color(255, 110, 110), new Color(157, 122, 255)};

        DevicePanel(List<Device> records) {
            this.records = records;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 按顺序绘制：表格、状态柱状图、类型饼图
            drawTable(g2, 40, 40, 1120, 260);
            drawStatusChart(g2, 80, 360, 400, 220);
            drawTypeChart(g2, 620, 360, 400, 220);
            g2.dispose();
        }

        // 使用 drawRect/drawString 直接手工绘制表格，避免使用 JTable 等组件
        private void drawTable(Graphics2D g2, int x, int y, int width, int height) {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 16));
            g2.drawString("设备原始数据表", x, y - 10);

            int rowHeight = 30;
            int colWidth = width / 6;
            String[] headers = {"编号", "名称", "类型", "IP", "状态", "位置"};

            g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
            for (int i = 0; i < headers.length; i++) {
                int headerX = x + i * colWidth;
                g2.drawRect(headerX, y, colWidth, rowHeight);
                g2.drawString(headers[i], headerX + 12, y + 20);
            }

            int startY = y + rowHeight;
            g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            for (int rowIndex = 0; rowIndex < records.size() && rowIndex < 12; rowIndex++) {
                Device record = records.get(rowIndex);
                String[] rowData = {
                        record.getDeviceId(),
                        record.getName(),
                        record.getDeviceType(),
                        record.getIp(),
                        record.getStatus(),
                        record.getLocation(),
                };
                for (int col = 0; col < rowData.length; col++) {
                    int cellX = x + col * colWidth;
                    g2.drawRect(cellX, startY + rowIndex * rowHeight, colWidth, rowHeight);
                    g2.drawString(truncate(rowData[col], 12), cellX + 6, startY + rowIndex * rowHeight + 18);
                }
            }
        }

        // 统计并绘制设备状态分布柱状图，例如在线、离线、维护中数量
        private void drawStatusChart(Graphics2D g2, int x, int y, int width, int height) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("在线", 0);
            counts.put("离线", 0);
            counts.put("维护中", 0);
            for (Device record : records) {
                String status = record.getStatus();
                counts.put(status, counts.getOrDefault(status, 0) + 1);
            }

            String title = "设备状态分布";
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
            g2.drawString(title, x, y - 10);

            int barWidth = 70;
            int gap = 30;
            int maxValue = 0;
            for (Integer value : counts.values()) {
                maxValue = Math.max(maxValue, value);
            }
            maxValue = Math.max(1, maxValue);

            int index = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int barHeight = (int) ((entry.getValue() * 1.0 / maxValue) * 150);
                int barX = x + index * (barWidth + gap);
                int barY = y + 150 - barHeight;
                g2.setColor(palette[index % palette.length]);
                g2.fillRect(barX, barY, barWidth, barHeight);
                g2.setColor(Color.BLACK);
                g2.drawRect(barX, barY, barWidth, barHeight);
                g2.drawString(entry.getKey(), barX + 10, y + 170);
                g2.drawString(String.valueOf(entry.getValue()), barX + 18, barY - 10);
                index++;
            }
        }

        // 统计并绘制设备类型分布饼图，展示不同设备类型的占比
        private void drawTypeChart(Graphics2D g2, int x, int y, int width, int height) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (Device record : records) {
                counts.put(record.getDeviceType(), counts.getOrDefault(record.getDeviceType(), 0) + 1);
            }

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));
            g2.drawString("设备类型分布", x, y - 10);

            int centerX = x + 120;
            int centerY = y + 110;
            int radius = 80;
            double total = 0;
            for (Integer value : counts.values()) {
                total += value;
            }
            if (total == 0) {
                return;
            }

            double startAngle = 0;
            int colorIndex = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                double angle = 360 * (entry.getValue() / total);
                g2.setColor(palette[colorIndex % palette.length]);
                g2.fillArc(centerX - radius, centerY - radius, radius * 2, radius * 2, (int) startAngle, (int) angle);
                g2.setColor(Color.BLACK);
                g2.drawArc(centerX - radius, centerY - radius, radius * 2, radius * 2, (int) startAngle, (int) angle);

                double textAngle = startAngle + angle / 2;
                double labelX = centerX + Math.cos(Math.toRadians(textAngle)) * (radius + 18);
                double labelY = centerY + Math.sin(Math.toRadians(textAngle)) * (radius + 18);
                g2.drawString(entry.getKey(), (int) labelX, (int) labelY);
                startAngle += angle;
                colorIndex++;
            }
        }

        // 截断过长字符串，保证表格单元格整齐排列
        private String truncate(String text, int maxLength) {
            if (text == null) {
                return "";
            }
            if (text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength - 1) + "…";
        }
    }

    public static void main(String[] args) {
        // 通过 SwingUtilities.invokeLater 保证 UI 在事件分发线程中创建和显示
        SwingUtilities.invokeLater(() -> {
            LanDeviceViewer viewer = new LanDeviceViewer();
            viewer.setVisible(true);
        });
    }
}]}]}] }]}]}]