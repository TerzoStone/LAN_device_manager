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

public class LanDeviceViewer extends JFrame {
    private final List<DeviceRecord> records;

    public LanDeviceViewer() {
        super("局域网设备管理系统 - 数据可视化");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 900);
        setLocationRelativeTo(null);

        File dataFile = new File("data/lan_devices.csv");
        records = readRecords(dataFile);
        setContentPane(new DevicePanel(records));
    }

    private static List<DeviceRecord> readRecords(File dataFile) {
        List<DeviceRecord> list = new ArrayList<>();
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
                DeviceRecord record = new DeviceRecord();
                record.deviceId = values.get(0).trim();
                record.name = values.get(1).trim();
                record.deviceType = values.get(2).trim();
                record.ip = values.get(3).trim();
                record.mac = values.get(4).trim();
                record.location = values.get(5).trim();
                record.status = values.get(6).trim();
                record.owner = values.get(7).trim();
                record.purchaseDate = values.get(8).trim();
                list.add(record);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

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

    private static class DeviceRecord {
        String deviceId;
        String name;
        String deviceType;
        String ip;
        String mac;
        String location;
        String status;
        String owner;
        String purchaseDate;
    }

    private static class DevicePanel extends JPanel {
        private final List<DeviceRecord> records;
        private final Color[] palette = {new Color(72, 128, 255), new Color(94, 196, 110), new Color(255, 174, 66), new Color(255, 110, 110), new Color(157, 122, 255)};

        DevicePanel(List<DeviceRecord> records) {
            this.records = records;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawTable(g2, 40, 40, 1120, 260);
            drawStatusChart(g2, 80, 360, 400, 220);
            drawTypeChart(g2, 620, 360, 400, 220);
            g2.dispose();
        }

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
                DeviceRecord record = records.get(rowIndex);
                String[] rowData = {
                        record.deviceId,
                        record.name,
                        record.deviceType,
                        record.ip,
                        record.status,
                        record.location,
                };
                for (int col = 0; col < rowData.length; col++) {
                    int cellX = x + col * colWidth;
                    g2.drawRect(cellX, startY + rowIndex * rowHeight, colWidth, rowHeight);
                    g2.drawString(truncate(rowData[col], 12), cellX + 6, startY + rowIndex * rowHeight + 18);
                }
            }
        }

        private void drawStatusChart(Graphics2D g2, int x, int y, int width, int height) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("在线", 0);
            counts.put("离线", 0);
            counts.put("维护中", 0);
            for (DeviceRecord record : records) {
                String status = record.status;
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

        private void drawTypeChart(Graphics2D g2, int x, int y, int width, int height) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (DeviceRecord record : records) {
                counts.put(record.deviceType, counts.getOrDefault(record.deviceType, 0) + 1);
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
        SwingUtilities.invokeLater(() -> {
            LanDeviceViewer viewer = new LanDeviceViewer();
            viewer.setVisible(true);
        });
    }
}
