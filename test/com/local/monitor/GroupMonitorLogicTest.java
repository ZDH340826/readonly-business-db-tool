package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class GroupMonitorLogicTest {
    public static void main(String[] args) {
        healthyGroupCreatesNormalEvaluation();
        matchingRuleStaysPendingBeforeDuration();
        matchingRuleCreatesActiveAlertAfterDuration();
        acknowledgedAlertStaysSuppressedUntilRecovery();
        recoveredGroupClearsRuntimeState();
        System.out.println("GroupMonitorLogicTest PASS");
    }

    private static void healthyGroupCreatesNormalEvaluation() {
        PointGroupDefinition group = defaultGroup(2, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "", 1, 0),
                record("BACKUP_POINT_004", "", 1, 0)), state);

        TestSupport.assertEquals(GroupAlertStatus.NORMAL, evaluation.status(), "healthy group should be normal");
        TestSupport.assertFalse(evaluation.ruleMatched(), "healthy group should not match alert rule");
        TestSupport.assertEquals(0, evaluation.continuousMatchedMinutes(), "healthy group should not count duration");
        TestSupport.assertFalse(evaluation.shouldShowDialog(), "healthy group should not show dialog");
    }

    private static void matchingRuleStaysPendingBeforeDuration() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        for (int i = 1; i <= 4; i++) {
            GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
            TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, evaluation.status(),
                    "matched group should stay pending before duration");
            TestSupport.assertEquals(i, evaluation.continuousMatchedMinutes(), "duration count should advance");
            TestSupport.assertFalse(evaluation.shouldShowDialog(), "pending group should not show dialog");
        }
    }

    private static void matchingRuleCreatesActiveAlertAfterDuration() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation evaluation = null;
        for (int i = 0; i < 5; i++) {
            evaluation = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        }

        TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, evaluation.status(),
                "matched group should alert after duration");
        TestSupport.assertEquals(5, evaluation.continuousMatchedMinutes(), "duration should reach threshold");
        TestSupport.assertTrue(evaluation.shouldShowDialog(), "first active alert should show dialog");
        TestSupport.assertContains(evaluation.message(), "USE_POINT_001", "message should include use point");
        TestSupport.assertContains(evaluation.message(), "backupAvailable=2/4", "message should include backup count");
    }

    private static void acknowledgedAlertStaysSuppressedUntilRecovery() {
        PointGroupDefinition group = defaultGroup(3, 2);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation first = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        GroupEvaluation second = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, second.status(), "second check should alert");
        TestSupport.assertTrue(second.shouldShowDialog(), "active alert should show once before acknowledgement");

        state.acknowledge();
        GroupEvaluation acknowledged = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        TestSupport.assertEquals(GroupAlertStatus.ACKED_ALERT, acknowledged.status(), "acknowledged alert should be suppressed");
        TestSupport.assertFalse(acknowledged.shouldShowDialog(), "acknowledged alert should not show dialog");

        GroupEvaluation stillAcknowledged = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        TestSupport.assertEquals(GroupAlertStatus.ACKED_ALERT, stillAcknowledged.status(),
                "same shortage should stay acknowledged");
    }

    private static void recoveredGroupClearsRuntimeState() {
        PointGroupDefinition group = defaultGroup(3, 2);
        GroupRuntimeState state = new GroupRuntimeState();
        GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        state.acknowledge();

        GroupEvaluation recovered = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "SHELF_BACKUP_003", 1, 0),
                record("BACKUP_POINT_004", "", 1, 0)), state);

        TestSupport.assertEquals(GroupAlertStatus.NORMAL, recovered.status(), "recovered group should return to normal");
        TestSupport.assertEquals(0, recovered.continuousMatchedMinutes(), "recovery should reset duration");
        TestSupport.assertFalse(state.isAcknowledged(), "recovery should clear acknowledgement");

        GroupEvaluation shortageAgain = GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        GroupMonitorLogic.evaluate(group, shortageRecords(), state);
        TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, shortageAgain.status(),
                "new shortage should start a new pending period");
    }

    private static PointGroupDefinition defaultGroup(int minBackupAvailable, int durationMinutes) {
        return new PointGroupDefinition(
                "group-001",
                "Area A",
                "Rear Panel",
                "Material A",
                true,
                60,
                List.of(
                        new GroupMonitorPoint("use-001", "USE_POINT_001", "Use", PointRole.USE, true, 1),
                        new GroupMonitorPoint("backup-001", "BACKUP_POINT_001", "Backup 1", PointRole.BACKUP, true, 2),
                        new GroupMonitorPoint("backup-002", "BACKUP_POINT_002", "Backup 2", PointRole.BACKUP, true, 3),
                        new GroupMonitorPoint("backup-003", "BACKUP_POINT_003", "Backup 3", PointRole.BACKUP, true, 4),
                        new GroupMonitorPoint("backup-004", "BACKUP_POINT_004", "Backup 4", PointRole.BACKUP, true, 5)),
                new GroupAlertRule(true, true, minBackupAvailable, durationMinutes));
    }

    private static List<PointRecord> shortageRecords() {
        return List.of(
                record("USE_POINT_001", "", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "", 1, 0),
                record("BACKUP_POINT_004", "", 1, 0));
    }

    private static PointRecord record(String code, String shelfCode, int status, int lock) {
        return new PointRecord(
                code,
                shelfCode,
                "0",
                status,
                lock,
                "AREA_A",
                "AREA_BUFFER",
                LocalDateTime.of(2026, 6, 30, 9, 0),
                LocalDateTime.of(2026, 6, 30, 8, 59));
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
