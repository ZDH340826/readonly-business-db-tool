package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GroupConfigStoreTest {
    public static void main(String[] args) throws Exception {
        savesAndLoadsPointGroups();
        missingConfigReturnsDefaultGroup();
        rejectsRulesThatRequireMoreBackupsThanConfigured();
        loadsOldConfigWithBackupThresholdParticipationEnabled();
        savesAndLoadsBackupThresholdParticipation();
        System.out.println("GroupConfigStoreTest PASS");
    }

    private static void savesAndLoadsPointGroups() throws Exception {
        Path configPath = Files.createTempDirectory("group-config-test").resolve("group-config.properties");
        GroupConfigStore store = new GroupConfigStore(configPath);
        PointGroupDefinition source = group("group-001", 3, 5, true);

        store.save(List.of(source));

        List<PointGroupDefinition> loaded = store.load();
        TestSupport.assertEquals(1, loaded.size(), "one group should load");
        PointGroupDefinition group = loaded.get(0);
        TestSupport.assertEquals("group-001", group.id(), "group id should round-trip");
        TestSupport.assertEquals("Area A", group.areaName(), "area should round-trip");
        TestSupport.assertEquals("Rear Panel", group.groupName(), "group name should round-trip");
        TestSupport.assertEquals("Material A", group.materialName(), "material should round-trip");
        TestSupport.assertEquals(60, group.checkIntervalSeconds(), "check interval should round-trip");
        TestSupport.assertEquals(5, group.points().size(), "points should round-trip");
        TestSupport.assertEquals(PointRole.USE, group.points().get(0).role(), "use point role should load");
        TestSupport.assertEquals(3, group.rule().minBackupAvailable(), "rule threshold should round-trip");
        TestSupport.assertEquals(5, group.rule().durationMinutes(), "duration should round-trip");

        String raw = Files.readString(configPath);
        TestSupport.assertFalse(raw.toLowerCase().contains("password"), "group config must not store password");
    }

    private static void missingConfigReturnsDefaultGroup() throws Exception {
        Path configPath = Files.createTempDirectory("group-config-default-test").resolve("group-config.properties");

        List<PointGroupDefinition> loaded = new GroupConfigStore(configPath).load();

        TestSupport.assertEquals(1, loaded.size(), "missing config should provide one sample group");
        TestSupport.assertEquals("sample-group-001", loaded.get(0).id(), "default group should be stable");
        TestSupport.assertEquals(5, loaded.get(0).points().size(), "default group should contain one use and four backups");
    }

    private static void rejectsRulesThatRequireMoreBackupsThanConfigured() throws Exception {
        Path configPath = Files.createTempDirectory("group-config-invalid-test").resolve("group-config.properties");
        GroupConfigStore store = new GroupConfigStore(configPath);

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> store.save(List.of(group("invalid-group", 5, 5, true))),
                "rule requiring more backups than configured should be rejected");
    }

    private static void loadsOldConfigWithBackupThresholdParticipationEnabled() throws Exception {
        Path configPath = Files.createTempFile("old-group-config", ".properties");
        Files.writeString(configPath, String.join(System.lineSeparator(),
                "group.count=1",
                "group.0.id=old-group",
                "group.0.areaName=区域A",
                "group.0.groupName=后围板组",
                "group.0.materialName=后围板总成",
                "group.0.enabled=true",
                "group.0.checkIntervalSeconds=60",
                "group.0.rule.enabled=true",
                "group.0.rule.requireUsePointEmpty=true",
                "group.0.rule.minBackupAvailable=1",
                "group.0.rule.durationMinutes=5",
                "group.0.point.count=2",
                "group.0.point.0.id=use",
                "group.0.point.0.code=USE_POINT_001",
                "group.0.point.0.alias=使用位",
                "group.0.point.0.role=USE",
                "group.0.point.0.enabled=true",
                "group.0.point.0.sortOrder=1",
                "group.0.point.1.id=backup",
                "group.0.point.1.code=BACKUP_POINT_001",
                "group.0.point.1.alias=备用位1",
                "group.0.point.1.role=BACKUP",
                "group.0.point.1.enabled=true",
                "group.0.point.1.sortOrder=2",
                ""));

        List<PointGroupDefinition> loaded = new GroupConfigStore(configPath).load();
        TestSupport.assertTrue(loaded.get(0).rule().backupThresholdParticipates(),
                "old config should default backup threshold participation to true");
    }

    private static void savesAndLoadsBackupThresholdParticipation() throws Exception {
        Path configPath = Files.createTempFile("new-group-config", ".properties");
        PointGroupDefinition group = group("group-v040", 1, 5, false);
        GroupConfigStore store = new GroupConfigStore(configPath);
        store.save(List.of(group));

        List<PointGroupDefinition> loaded = store.load();
        TestSupport.assertTrue(!loaded.get(0).rule().backupThresholdParticipates(),
                "new config should persist disabled backup threshold participation");
    }

    private static PointGroupDefinition group(
            String id,
            int minBackupAvailable,
            int durationMinutes,
            boolean backupThresholdParticipates) {
        return new PointGroupDefinition(
                id,
                "Area A",
                "Rear Panel",
                "Material A",
                true,
                60,
                List.of(
                        new GroupMonitorPoint(id + "-use", "USE_POINT_001", "Use", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup-1", "BACKUP_POINT_001", "Backup 1", PointRole.BACKUP, true, 2),
                        new GroupMonitorPoint(id + "-backup-2", "BACKUP_POINT_002", "Backup 2", PointRole.BACKUP, true, 3),
                        new GroupMonitorPoint(id + "-backup-3", "BACKUP_POINT_003", "Backup 3", PointRole.BACKUP, true, 4),
                        new GroupMonitorPoint(id + "-backup-4", "BACKUP_POINT_004", "Backup 4", PointRole.BACKUP, true, 5)),
                new GroupAlertRule(true, true, minBackupAvailable, durationMinutes, backupThresholdParticipates));
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertFalse(boolean condition, String message) {
            if (condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        static void assertThrows(Class<? extends Throwable> type, ThrowingRunnable action, String message) {
            try {
                action.run();
            } catch (Throwable t) {
                if (type.isInstance(t)) {
                    return;
                }
                throw new AssertionError(message + " wrong exception=" + t);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
