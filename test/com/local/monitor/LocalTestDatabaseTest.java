package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class LocalTestDatabaseTest {
    public static void main(String[] args) throws Exception {
        relativeLocalPathIsConvertedToUsableDatabaseUrl();
        seededDatabaseSupportsGroupShortageScenario();
        demoCatalogCoversRepresentativeFieldConditions();
        resetToolWritesMatchingGroupConfiguration();
        resetRejectsNonLocalDatabaseConfiguration();
        System.out.println("LocalTestDatabaseTest PASS");
    }

    private static void seededDatabaseSupportsGroupShortageScenario() throws Exception {
        Path dir = Files.createTempDirectory("shelf-monitor-h2-test");
        DbConfig config = DbConfig.localTest(dir.resolve("monitor-test").toString(), 30);
        LocalTestDatabase.reset(config);

        PointGroupDefinition group = GroupConfigStore.defaultGroups().get(0);
        PointRepository repository = new PointRepository();
        List<PointRecord> records = repository.fetch(config, new char[0], pointDefinitions(group));
        TestSupport.assertEquals(6, records.size(), "first demo scenario should return all six configured points");
        PointRecord backup = findRecord(records, "BACKUP_POINT_001");
        TestSupport.assertEquals("SHELF_BACKUP_001", backup.podCode(), "site table pod_code should map to shelf code");
        TestSupport.assertEquals(1, backup.status(), "site table status should map");
        TestSupport.assertEquals(0, backup.indLock(), "site table ind_lock should map");
        TestSupport.assertTrue(backup.dateChg() != null, "site table date_chg should map to update time");

        GroupRuntimeState state = new GroupRuntimeState();
        LocalDateTime start = LocalDateTime.of(2026, 7, 3, 9, 0);
        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, records, state, start);
        evaluation = GroupMonitorLogic.evaluate(
                group,
                records,
                state,
                start.plusMinutes(group.rule().durationMinutes()));
        TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, evaluation.status(),
                "seed data should make default group alert after duration");
        TestSupport.assertTrue(evaluation.shouldShowDialog(), "first active group alert should request dialog");

        LocalTestDatabase.setScenario(config, "normal");
        records = repository.fetch(config, new char[0], pointDefinitions(group));
        evaluation = GroupMonitorLogic.evaluate(
                group,
                records,
                state,
                start.plusMinutes(group.rule().durationMinutes() + 1));
        TestSupport.assertEquals(GroupAlertStatus.NORMAL, evaluation.status(), "normal local scenario should recover");
    }

    private static void demoCatalogCoversRepresentativeFieldConditions() throws Exception {
        Path dir = Files.createTempDirectory("shelf-monitor-demo-catalog-test");
        DbConfig config = DbConfig.localTest(dir.resolve("monitor-test").toString(), 30);
        LocalTestDatabase.reset(config);
        List<PointGroupDefinition> groups = GroupConfigStore.defaultGroups();

        TestSupport.assertEquals(8, groups.size(), "demo catalog should expose eight field scenarios");

        GroupEvaluation anyUseEmpty = evaluate(config, group(groups, "sample-group-001"));
        TestSupport.assertTrue(anyUseEmpty.ruleMatched(),
                "scenario 1 should match when one of two use points is unavailable and backups are short");
        TestSupport.assertEquals(1, anyUseEmpty.usePointAvailableCount(),
                "scenario 1 should keep one of two use points available");
        TestSupport.assertEquals(2, anyUseEmpty.usePointTotal(),
                "scenario 1 should contain two enabled use points");

        GroupEvaluation allUseAvailable = evaluate(config, group(groups, "sample-group-002"));
        TestSupport.assertFalse(allUseAvailable.ruleMatched(),
                "scenario 2 should not match when both use points are available");
        TestSupport.assertEquals(2, allUseAvailable.usePointAvailableCount(),
                "scenario 2 should show both use points available");

        GroupEvaluation backupsEnough = evaluate(config, group(groups, "sample-group-003"));
        TestSupport.assertTrue(backupsEnough.usePointEmpty(),
                "scenario 3 should make the use condition true");
        TestSupport.assertFalse(backupsEnough.ruleMatched(),
                "scenario 3 should stay normal because backups meet the threshold");

        GroupEvaluation abnormalAndLocked = evaluate(config, group(groups, "sample-group-004"));
        TestSupport.assertEquals(PointMaterialStatus.EMPTY,
                point(abnormalAndLocked, "USE_POINT_031").status(),
                "scenario 4 should expose an abnormal-status use point as unavailable");
        TestSupport.assertEquals(PointMaterialStatus.EMPTY,
                point(abnormalAndLocked, "BACKUP_POINT_031").status(),
                "scenario 4 should expose a locked backup as unavailable");

        GroupEvaluation disabledPoints = evaluate(config, group(groups, "sample-group-005"));
        TestSupport.assertEquals(PointMaterialStatus.DISABLED,
                point(disabledPoints, "USE_POINT_042").status(),
                "scenario 5 should visibly retain a disabled use point");
        TestSupport.assertEquals(1, disabledPoints.usePointTotal(),
                "disabled use points must not participate in totals");

        GroupEvaluation missingPoint = evaluate(config, group(groups, "sample-group-006"));
        TestSupport.assertEquals(PointMaterialStatus.MISSING,
                point(missingPoint, "USE_POINT_051").status(),
                "scenario 6 should expose a configured land code missing from the database");

        GroupEvaluation healthy = evaluate(config, group(groups, "sample-group-007"));
        TestSupport.assertEquals(GroupAlertStatus.NORMAL, healthy.status(),
                "scenario 7 should be fully healthy");

        GroupEvaluation thresholdEdge = evaluate(config, group(groups, "sample-group-008"));
        TestSupport.assertEquals(thresholdEdge.backupAvailableCount(),
                group(groups, "sample-group-008").rule().minBackupAvailable(),
                "scenario 8 should sit exactly at the backup threshold");
        TestSupport.assertFalse(thresholdEdge.ruleMatched(),
                "backup availability equal to the threshold should not match shortage");
    }

    private static void resetToolWritesMatchingGroupConfiguration() throws Exception {
        Path dir = Files.createTempDirectory("shelf-monitor-demo-tool-test");
        Path databasePath = dir.resolve("local-test-db");
        Path groupConfigPath = dir.resolve("group-config.properties");

        LocalTestDbTool.main(new String[] {"reset", databasePath.toString(), groupConfigPath.toString()});

        List<PointGroupDefinition> loaded = new GroupConfigStore(groupConfigPath).load();
        TestSupport.assertTrue(Files.isRegularFile(groupConfigPath),
                "reset tool should write the requested group configuration file");
        TestSupport.assertEquals(8, loaded.size(),
                "reset tool should write the same eight demo groups as the database catalog");
    }

    private static void resetRejectsNonLocalDatabaseConfiguration() {
        DbConfig siteConfig = new DbConfig(
                "example.invalid", 5432, "demo", "public", "demo", "disable", 30);

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> LocalTestDatabase.reset(siteConfig),
                "local demo reset must reject every non-H2 database configuration");
    }

    private static void relativeLocalPathIsConvertedToUsableDatabaseUrl() throws Exception {
        DbConfig config = DbConfig.localTest(Path.of("build", "relative-local-db-test").toString(), 30);
        LocalTestDatabase.reset(config);
        TestSupport.assertFalse(
                config.jdbcUrl().contains("jdbc:h2:file:data") || config.jdbcUrl().contains("jdbc:h2:file:build"),
                "local H2 database URL must not use an implicit relative path");
    }

    private static List<PointDefinition> pointDefinitions(PointGroupDefinition group) {
        List<PointDefinition> points = new ArrayList<>();
        for (GroupMonitorPoint point : group.points()) {
            if (point.enabled()) {
                points.add(new PointDefinition(point.code(), point.alias()));
            }
        }
        return points;
    }

    private static GroupEvaluation evaluate(DbConfig config, PointGroupDefinition group) throws Exception {
        List<PointRecord> records = new PointRepository().fetch(config, new char[0], pointDefinitions(group));
        return GroupMonitorLogic.evaluate(
                group,
                records,
                new GroupRuntimeState(),
                LocalDateTime.of(2026, 7, 16, 9, 0));
    }

    private static PointGroupDefinition group(List<PointGroupDefinition> groups, String id) {
        for (PointGroupDefinition group : groups) {
            if (id.equals(group.id())) {
                return group;
            }
        }
        throw new AssertionError("missing demo group " + id);
    }

    private static PointStatusView point(GroupEvaluation evaluation, String code) {
        for (PointStatusView point : evaluation.pointStatuses()) {
            if (code.equals(point.pointCode())) {
                return point;
            }
        }
        throw new AssertionError("missing point status " + code);
    }

    private static PointRecord findRecord(List<PointRecord> records, String code) {
        for (PointRecord record : records) {
            if (code.equals(record.mapDataCode())) {
                return record;
            }
        }
        throw new AssertionError("missing record " + code);
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertFalse(boolean condition, String message) {
            assertTrue(!condition, message);
        }

        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        static void assertThrows(
                Class<? extends Throwable> expected,
                ThrowingRunnable action,
                String message) {
            try {
                action.run();
            } catch (Throwable actual) {
                if (expected.isInstance(actual)) {
                    return;
                }
                throw new AssertionError(message + " wrong exception=" + actual, actual);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
