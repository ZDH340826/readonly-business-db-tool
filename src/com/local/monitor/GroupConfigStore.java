package com.local.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class GroupConfigStore {
    private final Path path;

    public GroupConfigStore(Path path) {
        this.path = path;
    }

    public List<PointGroupDefinition> load() {
        if (!Files.exists(path)) {
            return defaultGroups();
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException ignored) {
            return defaultGroups();
        }

        int groupCount = parseInt(p.getProperty("group.count"), 0);
        if (groupCount <= 0) {
            return defaultGroups();
        }

        List<PointGroupDefinition> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(loadGroup(p, "group." + i + "."));
        }
        validateGroups(groups);
        return groups;
    }

    public void save(List<PointGroupDefinition> groups) throws IOException {
        validateGroups(groups);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Properties p = new Properties();
        p.setProperty("group.count", String.valueOf(groups.size()));
        for (int i = 0; i < groups.size(); i++) {
            storeGroup(p, "group." + i + ".", groups.get(i));
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "Point group monitor config.");
        }
    }

    public static List<PointGroupDefinition> defaultGroups() {
        return List.of(new PointGroupDefinition(
                "sample-group-001",
                "Area A",
                "Sample Material Group",
                "Sample Material",
                true,
                PointGroupDefinition.DEFAULT_CHECK_INTERVAL_SECONDS,
                List.of(
                        new GroupMonitorPoint("sample-use-001", "USE_POINT_001", "Use", PointRole.USE, true, 1),
                        new GroupMonitorPoint("sample-backup-001", "BACKUP_POINT_001", "Backup 1", PointRole.BACKUP, true, 2),
                        new GroupMonitorPoint("sample-backup-002", "BACKUP_POINT_002", "Backup 2", PointRole.BACKUP, true, 3),
                        new GroupMonitorPoint("sample-backup-003", "BACKUP_POINT_003", "Backup 3", PointRole.BACKUP, true, 4),
                        new GroupMonitorPoint("sample-backup-004", "BACKUP_POINT_004", "Backup 4", PointRole.BACKUP, true, 5)),
                new GroupAlertRule(true, true, 3, 5)));
    }

    static void validateGroups(List<PointGroupDefinition> groups) {
        if (groups == null) {
            throw new IllegalArgumentException("groups are required");
        }
        Set<String> groupIds = new HashSet<>();
        for (PointGroupDefinition group : groups) {
            if (group.id() == null || group.id().isBlank()) {
                throw new IllegalArgumentException("点位组 ID 不能为空");
            }
            if (!groupIds.add(group.id())) {
                throw new IllegalArgumentException("点位组 ID 不能重复：" + group.id());
            }
            if (group.groupName() == null || group.groupName().isBlank()) {
                throw new IllegalArgumentException("组名称不能为空：" + group.id());
            }
            if (group.checkIntervalSeconds() < 60 || group.checkIntervalSeconds() > 86400) {
                throw new IllegalArgumentException("检测周期必须在 1 到 1440 分钟之间：" + group.id());
            }
            if (group.rule().durationMinutes() < 1 || group.rule().durationMinutes() > 1440) {
                throw new IllegalArgumentException("报警持续时间必须在 1 到 1440 分钟之间：" + group.id());
            }
            int backupCount = 0;
            int useCount = 0;
            Set<String> pointCodes = new HashSet<>();
            for (GroupMonitorPoint point : group.points()) {
                if (point.enabled() && point.role() == PointRole.USE) {
                    useCount++;
                }
                if (point.enabled() && point.role() == PointRole.BACKUP) {
                    backupCount++;
                }
                if (point.enabled()) {
                    if (point.code() == null || point.code().isBlank()) {
                        throw new IllegalArgumentException("启用点位编码不能为空：" + group.id());
                    }
                    if (!pointCodes.add(point.code())) {
                        throw new IllegalArgumentException("同组点位编码不能重复：" + group.id() + " / " + point.code());
                    }
                }
            }
            if (group.enabled() && useCount == 0) {
                throw new IllegalArgumentException("启用组至少需要一个启用的使用位：" + group.id());
            }
            if (group.rule().minBackupAvailable() > backupCount) {
                throw new IllegalArgumentException("备用位最小可用数量不得超过启用备用位数量：" + group.id());
            }
        }
    }

    private static PointGroupDefinition loadGroup(Properties p, String prefix) {
        int pointCount = parseInt(p.getProperty(prefix + "point.count"), 0);
        List<GroupMonitorPoint> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            points.add(loadPoint(p, prefix + "point." + i + "."));
        }

        GroupAlertRule rule = new GroupAlertRule(
                parseBoolean(p.getProperty(prefix + "rule.enabled"), true),
                parseBoolean(p.getProperty(prefix + "rule.requireUsePointEmpty"), true),
                parseInt(p.getProperty(prefix + "rule.minBackupAvailable"), 1),
                parseInt(p.getProperty(prefix + "rule.durationMinutes"), 5),
                parseBoolean(p.getProperty(prefix + "rule.backupThresholdParticipates"), true));

        return new PointGroupDefinition(
                p.getProperty(prefix + "id", "group-" + System.nanoTime()),
                p.getProperty(prefix + "areaName", "Area"),
                p.getProperty(prefix + "groupName", "Point Group"),
                p.getProperty(prefix + "materialName", "Material"),
                parseBoolean(p.getProperty(prefix + "enabled"), true),
                parseInt(p.getProperty(prefix + "checkIntervalSeconds"),
                        PointGroupDefinition.DEFAULT_CHECK_INTERVAL_SECONDS),
                points,
                rule);
    }

    private static GroupMonitorPoint loadPoint(Properties p, String prefix) {
        return new GroupMonitorPoint(
                p.getProperty(prefix + "id"),
                p.getProperty(prefix + "code"),
                p.getProperty(prefix + "alias"),
                PointRole.valueOf(p.getProperty(prefix + "role", PointRole.BACKUP.name())),
                parseBoolean(p.getProperty(prefix + "enabled"), true),
                parseInt(p.getProperty(prefix + "sortOrder"), 0));
    }

    private static void storeGroup(Properties p, String prefix, PointGroupDefinition group) {
        p.setProperty(prefix + "id", group.id());
        p.setProperty(prefix + "areaName", group.areaName());
        p.setProperty(prefix + "groupName", group.groupName());
        p.setProperty(prefix + "materialName", group.materialName());
        p.setProperty(prefix + "enabled", String.valueOf(group.enabled()));
        p.setProperty(prefix + "checkIntervalSeconds", String.valueOf(group.checkIntervalSeconds()));
        p.setProperty(prefix + "rule.enabled", String.valueOf(group.rule().enabled()));
        p.setProperty(prefix + "rule.requireUsePointEmpty", String.valueOf(group.rule().requireUsePointEmpty()));
        p.setProperty(prefix + "rule.minBackupAvailable", String.valueOf(group.rule().minBackupAvailable()));
        p.setProperty(prefix + "rule.durationMinutes", String.valueOf(group.rule().durationMinutes()));
        p.setProperty(prefix + "rule.backupThresholdParticipates",
                String.valueOf(group.rule().backupThresholdParticipates()));
        p.setProperty(prefix + "point.count", String.valueOf(group.points().size()));
        for (int i = 0; i < group.points().size(); i++) {
            storePoint(p, prefix + "point." + i + ".", group.points().get(i));
        }
    }

    private static void storePoint(Properties p, String prefix, GroupMonitorPoint point) {
        p.setProperty(prefix + "id", point.id());
        p.setProperty(prefix + "code", point.code());
        p.setProperty(prefix + "alias", point.alias());
        p.setProperty(prefix + "role", point.role().name());
        p.setProperty(prefix + "enabled", String.valueOf(point.enabled()));
        p.setProperty(prefix + "sortOrder", String.valueOf(point.sortOrder()));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }
}
