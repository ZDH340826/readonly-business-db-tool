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
        System.out.println("LocalTestDatabaseTest PASS");
    }

    private static void seededDatabaseSupportsGroupShortageScenario() throws Exception {
        Path dir = Files.createTempDirectory("shelf-monitor-h2-test");
        DbConfig config = DbConfig.localTest(dir.resolve("monitor-test").toString(), 30);
        LocalTestDatabase.reset(config);

        PointGroupDefinition group = GroupConfigStore.defaultGroups().get(0);
        PointRepository repository = new PointRepository();
        List<PointRecord> records = repository.fetch(config, new char[0], pointDefinitions(group));
        TestSupport.assertEquals(5, records.size(), "local test database should seed five point records");
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
    }
}
