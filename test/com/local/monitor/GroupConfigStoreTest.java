package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GroupConfigStoreTest {
    public static void main(String[] args) throws Exception {
        savesAndLoadsPointGroups();
        missingConfigReturnsDefaultGroup();
        rejectsRulesThatRequireMoreBackupsThanConfigured();
        System.out.println("GroupConfigStoreTest PASS");
    }

    private static void savesAndLoadsPointGroups() throws Exception {
        Path configPath = Files.createTempDirectory("group-config-test").resolve("group-config.properties");
        GroupConfigStore store = new GroupConfigStore(configPath);
        PointGroupDefinition source = group("group-001", 3, 5);

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
                () -> store.save(List.of(group("invalid-group", 5, 5))),
                "rule requiring more backups than configured should be rejected");
    }

    private static PointGroupDefinition group(String id, int minBackupAvailable, int durationMinutes) {
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
                new GroupAlertRule(true, true, minBackupAvailable, durationMinutes));
    }

    private static final class TestSupport {
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
