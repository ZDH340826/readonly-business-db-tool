package com.local.monitor;

import java.util.List;

public final class LocalDemoCatalog {
    private static final List<PointGroupDefinition> GROUPS = List.of(
            group(
                    "sample-group-001",
                    "本地演示 A 区",
                    "场景1：双口任一无料",
                    "演示物料 A",
                    3,
                    List.of(
                            use("demo-001-use-1", "USE_POINT_001", "上料口 1", true, 1),
                            use("demo-001-use-2", "USE_POINT_002", "上料口 2", true, 2),
                            backup("demo-001-backup-1", "BACKUP_POINT_001", "备用位 1", true, 3),
                            backup("demo-001-backup-2", "BACKUP_POINT_002", "备用位 2", true, 4),
                            backup("demo-001-backup-3", "BACKUP_POINT_003", "备用位 3", true, 5),
                            backup("demo-001-backup-4", "BACKUP_POINT_004", "备用位 4", true, 6))),
            group(
                    "sample-group-002",
                    "本地演示 A 区",
                    "场景2：使用位全部有料",
                    "演示物料 B",
                    2,
                    List.of(
                            use("demo-002-use-1", "USE_POINT_011", "上料口 1", true, 1),
                            use("demo-002-use-2", "USE_POINT_012", "上料口 2", true, 2),
                            backup("demo-002-backup-1", "BACKUP_POINT_011", "备用位 1", true, 3),
                            backup("demo-002-backup-2", "BACKUP_POINT_012", "备用位 2", true, 4),
                            backup("demo-002-backup-3", "BACKUP_POINT_013", "备用位 3", true, 5))),
            group(
                    "sample-group-003",
                    "本地演示 B 区",
                    "场景3：备用位数量充足",
                    "演示物料 C",
                    3,
                    List.of(
                            use("demo-003-use-1", "USE_POINT_021", "上料口", true, 1),
                            backup("demo-003-backup-1", "BACKUP_POINT_021", "备用位 1", true, 2),
                            backup("demo-003-backup-2", "BACKUP_POINT_022", "备用位 2", true, 3),
                            backup("demo-003-backup-3", "BACKUP_POINT_023", "备用位 3", true, 4))),
            group(
                    "sample-group-004",
                    "本地演示 B 区",
                    "场景4：状态异常与锁定",
                    "演示物料 D",
                    2,
                    List.of(
                            use("demo-004-use-1", "USE_POINT_031", "状态异常上料口", true, 1),
                            use("demo-004-use-2", "USE_POINT_032", "正常上料口", true, 2),
                            backup("demo-004-backup-1", "BACKUP_POINT_031", "已锁定备用位", true, 3),
                            backup("demo-004-backup-2", "BACKUP_POINT_032", "正常备用位", true, 4))),
            group(
                    "sample-group-005",
                    "本地演示 C 区",
                    "场景5：含停用点位",
                    "演示物料 E",
                    2,
                    List.of(
                            use("demo-005-use-1", "USE_POINT_041", "启用上料口", true, 1),
                            use("demo-005-use-2", "USE_POINT_042", "停用上料口", false, 2),
                            backup("demo-005-backup-1", "BACKUP_POINT_041", "备用位 1", true, 3),
                            backup("demo-005-backup-2", "BACKUP_POINT_042", "备用位 2", true, 4),
                            backup("demo-005-backup-3", "BACKUP_POINT_043", "停用备用位", false, 5))),
            group(
                    "sample-group-006",
                    "本地演示 C 区",
                    "场景6：数据库未查到地码",
                    "演示物料 F",
                    2,
                    List.of(
                            use("demo-006-use-1", "USE_POINT_051", "未查到上料口", true, 1),
                            backup("demo-006-backup-1", "BACKUP_POINT_051", "无料备用位", true, 2),
                            backup("demo-006-backup-2", "BACKUP_POINT_052", "有料备用位", true, 3))),
            group(
                    "sample-group-007",
                    "本地演示 D 区",
                    "场景7：全部正常",
                    "演示物料 G",
                    1,
                    List.of(
                            use("demo-007-use-1", "USE_POINT_061", "正常上料口", true, 1),
                            backup("demo-007-backup-1", "BACKUP_POINT_061", "备用位 1", true, 2),
                            backup("demo-007-backup-2", "BACKUP_POINT_062", "备用位 2", true, 3))),
            group(
                    "sample-group-008",
                    "本地演示 D 区",
                    "场景8：刚好达到备用下限",
                    "演示物料 H",
                    2,
                    List.of(
                            use("demo-008-use-1", "USE_POINT_071", "无料上料口", true, 1),
                            backup("demo-008-backup-1", "BACKUP_POINT_071", "备用位 1", true, 2),
                            backup("demo-008-backup-2", "BACKUP_POINT_072", "备用位 2", true, 3),
                            backup("demo-008-backup-3", "BACKUP_POINT_073", "备用位 3", true, 4))));

    private static final List<DatabaseRow> DATABASE_ROWS = List.of(
            row("USE_POINT_001", "SHELF_USE_001", 1, 0, "AREA_USE", "AREA_BUFFER"),
            row("USE_POINT_002", null, 1, 0, "AREA_USE_SECONDARY", "AREA_BUFFER"),
            row("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0, "AREA_BUFFER", "AREA_NEXT"),
            row("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0, "AREA_BUFFER", "AREA_NEXT"),
            row("BACKUP_POINT_003", null, 1, 0, "AREA_BUFFER", "AREA_NEXT"),
            row("BACKUP_POINT_004", null, 1, 0, "AREA_BUFFER", "AREA_NEXT"),

            row("USE_POINT_011", "SHELF_USE_011", 1, 0, "DEMO_AREA_A", "DEMO_BUFFER_A"),
            row("USE_POINT_012", "SHELF_USE_012", 1, 0, "DEMO_AREA_A", "DEMO_BUFFER_A"),
            row("BACKUP_POINT_011", "SHELF_BACKUP_011", 1, 0, "DEMO_BUFFER_A", "DEMO_NEXT_A"),
            row("BACKUP_POINT_012", null, 1, 0, "DEMO_BUFFER_A", "DEMO_NEXT_A"),
            row("BACKUP_POINT_013", null, 1, 0, "DEMO_BUFFER_A", "DEMO_NEXT_A"),

            row("USE_POINT_021", null, 1, 0, "DEMO_AREA_B", "DEMO_BUFFER_B"),
            row("BACKUP_POINT_021", "SHELF_BACKUP_021", 1, 0, "DEMO_BUFFER_B", "DEMO_NEXT_B"),
            row("BACKUP_POINT_022", "SHELF_BACKUP_022", 1, 0, "DEMO_BUFFER_B", "DEMO_NEXT_B"),
            row("BACKUP_POINT_023", "SHELF_BACKUP_023", 1, 0, "DEMO_BUFFER_B", "DEMO_NEXT_B"),

            row("USE_POINT_031", "SHELF_USE_031", 0, 0, "DEMO_AREA_B", "DEMO_BUFFER_B"),
            row("USE_POINT_032", "SHELF_USE_032", 1, 0, "DEMO_AREA_B", "DEMO_BUFFER_B"),
            row("BACKUP_POINT_031", "SHELF_BACKUP_031", 1, 1, "DEMO_BUFFER_B", "DEMO_NEXT_B"),
            row("BACKUP_POINT_032", "SHELF_BACKUP_032", 1, 0, "DEMO_BUFFER_B", "DEMO_NEXT_B"),

            row("USE_POINT_041", "SHELF_USE_041", 1, 0, "DEMO_AREA_C", "DEMO_BUFFER_C"),
            row("USE_POINT_042", "SHELF_USE_042", 1, 0, "DEMO_AREA_C", "DEMO_BUFFER_C"),
            row("BACKUP_POINT_041", "SHELF_BACKUP_041", 1, 0, "DEMO_BUFFER_C", "DEMO_NEXT_C"),
            row("BACKUP_POINT_042", "SHELF_BACKUP_042", 1, 0, "DEMO_BUFFER_C", "DEMO_NEXT_C"),
            row("BACKUP_POINT_043", "SHELF_BACKUP_043", 1, 0, "DEMO_BUFFER_C", "DEMO_NEXT_C"),

            row("BACKUP_POINT_051", null, 1, 0, "DEMO_BUFFER_C", "DEMO_NEXT_C"),
            row("BACKUP_POINT_052", "SHELF_BACKUP_052", 1, 0, "DEMO_BUFFER_C", "DEMO_NEXT_C"),

            row("USE_POINT_061", "SHELF_USE_061", 1, 0, "DEMO_AREA_D", "DEMO_BUFFER_D"),
            row("BACKUP_POINT_061", "SHELF_BACKUP_061", 1, 0, "DEMO_BUFFER_D", "DEMO_NEXT_D"),
            row("BACKUP_POINT_062", "SHELF_BACKUP_062", 1, 0, "DEMO_BUFFER_D", "DEMO_NEXT_D"),

            row("USE_POINT_071", null, 1, 0, "DEMO_AREA_D", "DEMO_BUFFER_D"),
            row("BACKUP_POINT_071", "SHELF_BACKUP_071", 1, 0, "DEMO_BUFFER_D", "DEMO_NEXT_D"),
            row("BACKUP_POINT_072", "SHELF_BACKUP_072", 1, 0, "DEMO_BUFFER_D", "DEMO_NEXT_D"),
            row("BACKUP_POINT_073", null, 1, 0, "DEMO_BUFFER_D", "DEMO_NEXT_D"));

    private LocalDemoCatalog() {
    }

    public static List<PointGroupDefinition> groups() {
        return GROUPS;
    }

    static List<DatabaseRow> databaseRows() {
        return DATABASE_ROWS;
    }

    private static PointGroupDefinition group(
            String id,
            String area,
            String name,
            String material,
            int minBackupAvailable,
            List<GroupMonitorPoint> points) {
        return new PointGroupDefinition(
                id,
                area,
                name,
                material,
                true,
                PointGroupDefinition.DEFAULT_CHECK_INTERVAL_SECONDS,
                points,
                new GroupAlertRule(true, true, minBackupAvailable, 1, true));
    }

    private static GroupMonitorPoint use(String id, String code, String alias, boolean enabled, int order) {
        return new GroupMonitorPoint(id, code, alias, PointRole.USE, enabled, order);
    }

    private static GroupMonitorPoint backup(String id, String code, String alias, boolean enabled, int order) {
        return new GroupMonitorPoint(id, code, alias, PointRole.BACKUP, enabled, order);
    }

    private static DatabaseRow row(
            String code,
            String podCode,
            int status,
            int lock,
            String areaCode,
            String relateAreaCode) {
        return new DatabaseRow(code, podCode, "0", status, lock, areaCode, relateAreaCode);
    }

    record DatabaseRow(
            String code,
            String podCode,
            String podStatus,
            int status,
            int lock,
            String areaCode,
            String relateAreaCode) {
    }
}
