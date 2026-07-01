package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class MonitorLogicTest {
    public static void main(String[] args) {
        missingPodCreatesAlert();
        healthyPointsCreateNoAlert();
        acknowledgedAlertIsSuppressedUntilRecovery();
        acknowledgedPointDoesNotHideNewPointAlert();
        sqlBuilderOnlyBuildsWhitelistedSelect();
        System.out.println("MonitorLogicTest PASS");
    }

    private static void missingPodCreatesAlert() {
        List<PointDefinition> points = List.of(
                new PointDefinition("USE_POINT_001", "使用位"),
                new PointDefinition("BACKUP_POINT_001", "备用位"));
        List<PointRecord> records = List.of(
                record("USE_POINT_001", null, 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0));

        AlertState state = new AlertState();
        MonitorEvaluation evaluation = MonitorLogic.evaluate(points, records, state);

        TestSupport.assertTrue(evaluation.hasActiveAlert(), "missing pod should create active alert");
        TestSupport.assertEquals(1, evaluation.alerts().size(), "only the missing point should alert");
        TestSupport.assertEquals("使用位", evaluation.alerts().get(0).alias(), "alert keeps configured alias");
        TestSupport.assertContains(evaluation.alerts().get(0).message(), "无货架", "alert explains missing pod");
    }

    private static void healthyPointsCreateNoAlert() {
        List<PointDefinition> points = List.of(
                new PointDefinition("USE_POINT_001", "使用位"),
                new PointDefinition("BACKUP_POINT_001", "备用位"));
        List<PointRecord> records = List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0));

        MonitorEvaluation evaluation = MonitorLogic.evaluate(points, records, new AlertState());

        TestSupport.assertFalse(evaluation.hasActiveAlert(), "healthy points should not alert");
        TestSupport.assertEquals(0, evaluation.alerts().size(), "healthy points should have empty alert list");
    }

    private static void acknowledgedAlertIsSuppressedUntilRecovery() {
        List<PointDefinition> points = List.of(new PointDefinition("USE_POINT_001", "使用位"));
        List<PointRecord> missing = List.of(record("USE_POINT_001", "", 1, 0));
        List<PointRecord> healthy = List.of(record("USE_POINT_001", "SHELF_USE_001", 1, 0));
        AlertState state = new AlertState();

        MonitorEvaluation first = MonitorLogic.evaluate(points, missing, state);
        TestSupport.assertTrue(first.hasActiveAlert(), "first missing pod should alert");

        state.acknowledge(first.alertKey());
        MonitorEvaluation suppressed = MonitorLogic.evaluate(points, missing, state);
        TestSupport.assertFalse(suppressed.hasActiveAlert(), "acknowledged unchanged alert should be suppressed");

        MonitorEvaluation recovered = MonitorLogic.evaluate(points, healthy, state);
        TestSupport.assertFalse(recovered.hasActiveAlert(), "recovered point should not alert");

        MonitorEvaluation second = MonitorLogic.evaluate(points, missing, state);
        TestSupport.assertTrue(second.hasActiveAlert(), "same alert after recovery should alert again");
    }

    private static void acknowledgedPointDoesNotHideNewPointAlert() {
        List<PointDefinition> points = List.of(
                new PointDefinition("USE_POINT_001", "use"),
                new PointDefinition("BACKUP_POINT_001", "backup"));
        List<PointRecord> missingUse = List.of(
                record("USE_POINT_001", "", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0));
        List<PointRecord> missingBoth = List.of(
                record("USE_POINT_001", "", 1, 0),
                record("BACKUP_POINT_001", "", 1, 0));
        AlertState state = new AlertState();

        MonitorEvaluation first = MonitorLogic.evaluate(points, missingUse, state);
        state.acknowledge(first.alertKey());

        MonitorEvaluation second = MonitorLogic.evaluate(points, missingBoth, state);

        TestSupport.assertTrue(second.hasActiveAlert(), "new point alert should still be active");
        TestSupport.assertEquals(1, second.alerts().size(), "acknowledged point should be filtered out");
        TestSupport.assertEquals("BACKUP_POINT_001", second.alerts().get(0).code(), "new point should be reported");
    }

    private static void sqlBuilderOnlyBuildsWhitelistedSelect() {
        String sql = PointQuery.buildSelectSql("public", 2);
        TestSupport.assertTrue(sql.toLowerCase().startsWith("select "), "query must be SELECT");
        TestSupport.assertContains(sql, "map_data_code as point_code", "query maps site point code");
        TestSupport.assertContains(sql, "pod_code as shelf_code", "query maps site shelf code");
        TestSupport.assertContains(sql, "ind_lock as lock_state", "query maps site lock state");
        TestSupport.assertContains(sql, "date_chg as updated_at", "query maps site update time");
        TestSupport.assertContains(sql, "date_cr as marked_at", "query maps site created time");
        TestSupport.assertContains(sql, "from public.tcs_map_data", "query targets site point table only");
        TestSupport.assertFalse(sql.toLowerCase().contains("update "), "query must not contain UPDATE");
        TestSupport.assertEquals(2, TestSupport.count(sql, "?"), "query should have one placeholder per point");
        TestSupport.assertThrows(IllegalArgumentException.class, () -> PointQuery.buildSelectSql("public;drop table x", 1),
                "invalid schema must be rejected");
    }

    private static PointRecord record(String code, String podCode, int status, int lock) {
        return new PointRecord(
                code,
                podCode,
                "0",
                status,
                lock,
                "AREA_USE",
                "AREA_BUFFER",
                LocalDateTime.of(2026, 6, 28, 16, 5),
                LocalDateTime.of(2026, 6, 28, 16, 4));
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

        static void assertThrows(Class<? extends Throwable> type, Runnable action, String message) {
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

        static int count(String text, String needle) {
            int count = 0;
            int index = 0;
            while ((index = text.indexOf(needle, index)) >= 0) {
                count++;
                index += needle.length();
            }
            return count;
        }
    }
}


