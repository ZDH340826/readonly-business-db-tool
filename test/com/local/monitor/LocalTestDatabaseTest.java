package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LocalTestDatabaseTest {
    public static void main(String[] args) throws Exception {
        relativeLocalPathIsConvertedToUsableDatabaseUrl();

        Path dir = Files.createTempDirectory("shelf-monitor-h2-test");
        DbConfig config = DbConfig.localTest(dir.resolve("monitor-test").toString(), 30);

        LocalTestDatabase.reset(config);
        List<PointDefinition> points = List.of(
                new PointDefinition("USE_POINT_001", "使用位"),
                new PointDefinition("BACKUP_POINT_001", "备用位"));

        PointRepository repository = new PointRepository();
        List<PointRecord> records = repository.fetch(config, new char[0], points);
        MonitorEvaluation evaluation = MonitorLogic.evaluate(points, records, new AlertState());

        TestSupport.assertEquals(2, records.size(), "local test database should seed two point records");
        TestSupport.assertTrue(evaluation.hasActiveAlert(), "seed data should make use point missing");
        TestSupport.assertEquals("使用位", evaluation.alerts().get(0).alias(), "missing use point should alert");
        TestSupport.assertContains(evaluation.alerts().get(0).message(), "无货架", "alert should explain missing material");

        LocalTestDatabase.setScenario(config, "normal");
        records = repository.fetch(config, new char[0], points);
        evaluation = MonitorLogic.evaluate(points, records, new AlertState());
        TestSupport.assertFalse(evaluation.hasActiveAlert(), "normal local scenario should not alert");

        System.out.println("LocalTestDatabaseTest PASS");
    }

    private static void relativeLocalPathIsConvertedToUsableDatabaseUrl() throws Exception {
        DbConfig config = DbConfig.localTest(Path.of("build", "relative-local-db-test").toString(), 30);
        LocalTestDatabase.reset(config);
        TestSupport.assertFalse(
                config.jdbcUrl().contains("jdbc:h2:file:data") || config.jdbcUrl().contains("jdbc:h2:file:build"),
                "local H2 database URL must not use an implicit relative path");
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

        static void assertContains(String text, String needle, String message) {
            if (text == null || !text.contains(needle)) {
                throw new AssertionError(message + " text=" + text + " needle=" + needle);
            }
        }
    }
}


