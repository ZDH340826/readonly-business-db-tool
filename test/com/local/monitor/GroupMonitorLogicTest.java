package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class GroupMonitorLogicTest {
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 7, 3, 9, 0);

    public static void main(String[] args) {
        healthyGroupCreatesNormalEvaluation();
        firstMatchingRuleStartsPendingWithZeroElapsedSeconds();
        matchingRuleStaysPendingAfterFourRealMinutes();
        matchingRuleCreatesActiveAlertAfterFiveRealMinutes();
        activeDialogIsShownOnlyOnceDuringSameShortage();
        acknowledgedAlertStaysSuppressedUntilRecovery();
        recoveredGroupClearsRuntimeState();
        disabledPointsAreMappedButNotCounted();
        backupThresholdCanBeIgnoredWhenRuleDisablesParticipation();
        pointStatusesAreImmutableSnapshots();
        runtimeStateRoundsElapsedTimeAndHandlesClockSkew();
        queryFailureClearsShortageTimingAndDoesNotAlert();
        recoveredShortageAfterQueryFailureStartsTimingAgain();
        recoveredHealthyGroupAfterQueryFailureReturnsNormal();
        multipleUsePointsAllAvailableDoNotMatchUseCondition();
        anyUnavailableUsePointMatchesUseCondition();
        missingUsePointMatchesUseCondition();
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
                record("BACKUP_POINT_004", "", 1, 0)), state, BASE_TIME);

        TestSupport.assertEquals(GroupAlertStatus.NORMAL, evaluation.status(), "healthy group should be normal");
        TestSupport.assertFalse(evaluation.ruleMatched(), "healthy group should not match alert rule");
        TestSupport.assertEquals(0, evaluation.continuousMatchedSeconds(), "healthy group should not count duration");
        TestSupport.assertEquals(0, evaluation.continuousMatchedMinutes(), "healthy group should not count compatible minutes");
        TestSupport.assertEquals(300, evaluation.alertDurationSeconds(), "rule duration should be exposed in seconds");
        TestSupport.assertFalse(evaluation.shouldShowDialog(), "healthy group should not show dialog");
        TestSupport.assertEquals(BASE_TIME, state.lastCheckedAt(), "healthy check should update last check time");
    }

    private static void firstMatchingRuleStartsPendingWithZeroElapsedSeconds() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);

        TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, evaluation.status(),
                "first matched check should be pending");
        TestSupport.assertTrue(evaluation.ruleMatched(), "shortage should match alert rule");
        TestSupport.assertEquals(0, evaluation.continuousMatchedSeconds(),
                "first matched check should have zero elapsed seconds");
        TestSupport.assertEquals(0, evaluation.continuousMatchedMinutes(),
                "first matched check should have zero compatible minutes");
        TestSupport.assertEquals(BASE_TIME, state.conditionFirstMatchedAt(),
                "state should keep first matched time");
        TestSupport.assertContains(evaluation.message(), "观察中", "message should use Chinese status text");
        TestSupport.assertNotContains(evaluation.message(), "PENDING_ALERT", "message should not leak enum name");
        TestSupport.assertNotContains(evaluation.message(), "useEmpty=", "message should not leak debug fields");
    }

    private static void matchingRuleStaysPendingAfterFourRealMinutes() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(4));

        TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, evaluation.status(),
                "matched group should stay pending before five real minutes");
        TestSupport.assertEquals(240, evaluation.continuousMatchedSeconds(),
                "duration should use elapsed wall-clock seconds");
        TestSupport.assertEquals(4, evaluation.continuousMatchedMinutes(),
                "compatible minutes should be derived from elapsed seconds");
        TestSupport.assertFalse(evaluation.shouldShowDialog(), "pending group should not show dialog");
    }

    private static void matchingRuleCreatesActiveAlertAfterFiveRealMinutes() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(5));

        TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, evaluation.status(),
                "matched group should alert after five real minutes");
        TestSupport.assertEquals(300, evaluation.continuousMatchedSeconds(), "duration should reach threshold seconds");
        TestSupport.assertEquals(5, evaluation.continuousMatchedMinutes(), "duration should reach threshold minutes");
        TestSupport.assertTrue(evaluation.shouldShowDialog(), "first active alert should show dialog");
        TestSupport.assertContains(evaluation.message(), "需关注", "message should use Chinese active status text");
        TestSupport.assertContains(evaluation.message(), "使用位无料", "message should include use point state");
        TestSupport.assertContains(evaluation.message(), "备用位有料 2/4", "message should include backup count");
    }

    private static void activeDialogIsShownOnlyOnceDuringSameShortage() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupEvaluation firstActive = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(5));
        GroupEvaluation stillActive = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(6));

        TestSupport.assertTrue(firstActive.shouldShowDialog(), "first active alert should show dialog");
        TestSupport.assertFalse(stillActive.shouldShowDialog(),
                "same active shortage should not show dialog repeatedly");
    }

    private static void acknowledgedAlertStaysSuppressedUntilRecovery() {
        PointGroupDefinition group = defaultGroup(3, 2);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupEvaluation active = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(2));
        TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, active.status(), "second check should alert");
        TestSupport.assertTrue(active.shouldShowDialog(), "active alert should show once before acknowledgement");

        state.acknowledge();
        GroupEvaluation acknowledged = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(3));
        TestSupport.assertEquals(GroupAlertStatus.ACKED_ALERT, acknowledged.status(),
                "acknowledged alert should be suppressed");
        TestSupport.assertFalse(acknowledged.shouldShowDialog(), "acknowledged alert should not show dialog");

        GroupEvaluation stillAcknowledged = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(4));
        TestSupport.assertEquals(GroupAlertStatus.ACKED_ALERT, stillAcknowledged.status(),
                "same shortage should stay acknowledged");
    }

    private static void recoveredGroupClearsRuntimeState() {
        PointGroupDefinition group = defaultGroup(3, 2);
        GroupRuntimeState state = new GroupRuntimeState();
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME.plusMinutes(2));
        state.acknowledge();

        GroupEvaluation recovered = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "SHELF_BACKUP_003", 1, 0),
                record("BACKUP_POINT_004", "", 1, 0)), state, BASE_TIME.plusMinutes(3));

        TestSupport.assertEquals(GroupAlertStatus.NORMAL, recovered.status(), "recovered group should return to normal");
        TestSupport.assertEquals(0, recovered.continuousMatchedSeconds(), "recovery should reset duration seconds");
        TestSupport.assertEquals(0, recovered.continuousMatchedMinutes(), "recovery should reset compatible minutes");
        TestSupport.assertFalse(state.isAcknowledged(), "recovery should clear acknowledgement");
        TestSupport.assertFalse(state.activeDialogShown(), "recovery should clear active dialog state");

        GroupEvaluation shortageAgain = GroupMonitorLogic.evaluate(
                group, shortageRecords(), state, BASE_TIME.plusMinutes(4));
        TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, shortageAgain.status(),
                "new shortage should start a new pending period");
        TestSupport.assertEquals(0, shortageAgain.continuousMatchedSeconds(),
                "new shortage should restart elapsed seconds");
    }

    private static void disabledPointsAreMappedButNotCounted() {
        PointGroupDefinition group = groupWithDisabledBackup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "", 1, 0),
                record("BACKUP_POINT_004", "SHELF_DISABLED", 1, 0)), state, BASE_TIME);

        TestSupport.assertEquals(5, evaluation.pointStatuses().size(), "all configured points should be mapped");
        TestSupport.assertEquals(3, evaluation.backupTotal(), "disabled backup should not be counted");
        TestSupport.assertEquals(2, evaluation.backupAvailableCount(), "disabled backup shelf should not count available");
        TestSupport.assertTrue(evaluation.usePointEmpty(), "empty enabled use point should be detected");
    }

    private static void backupThresholdCanBeIgnoredWhenRuleDisablesParticipation() {
        PointGroupDefinition group = defaultGroup(
                5,
                5,
                new GroupAlertRule(true, true, 5, 5, false));
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "SHELF_BACKUP_003", 1, 0),
                record("BACKUP_POINT_004", "SHELF_BACKUP_004", 1, 0)), state, BASE_TIME);

        TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, evaluation.status(),
                "backup threshold should not block rule match when disabled");
        TestSupport.assertTrue(evaluation.ruleMatched(), "rule should match by use condition alone");
        TestSupport.assertEquals(4, evaluation.backupAvailableCount(), "available backup count should still be reported");
    }

    private static void pointStatusesAreImmutableSnapshots() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);

        TestSupport.assertEquals(5, evaluation.pointStatuses().size(), "evaluation should include point statuses");
        TestSupport.assertEquals(PointMaterialStatus.EMPTY, evaluation.pointStatuses().get(0).status(),
                "use point status should be included");
        TestSupport.assertThrows(UnsupportedOperationException.class,
                () -> evaluation.pointStatuses().add(evaluation.pointStatuses().get(0)),
                "point statuses should be immutable");
    }

    private static void runtimeStateRoundsElapsedTimeAndHandlesClockSkew() {
        GroupRuntimeState state = new GroupRuntimeState();
        state.markMatched(BASE_TIME);

        TestSupport.assertEquals(0, state.continuousMatchedSeconds(BASE_TIME.minusSeconds(1)),
                "negative elapsed seconds should be clamped");
        TestSupport.assertEquals(0, state.continuousMatchedMinutes(BASE_TIME.minusSeconds(1)),
                "negative elapsed minutes should be clamped");
        TestSupport.assertEquals(1, state.continuousMatchedMinutes(BASE_TIME.plusSeconds(1)),
                "non-zero seconds should round up to one minute");
        TestSupport.assertEquals(2, state.continuousMatchedMinutes(BASE_TIME.plusSeconds(61)),
                "minutes should round up");
    }

    private static void queryFailureClearsShortageTimingAndDoesNotAlert() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME.plusMinutes(4));

        GroupEvaluation failed = GroupMonitorLogic.queryFailed(
                group,
                state,
                BASE_TIME.plusMinutes(5),
                "查询失败：连接超时");

        TestSupport.assertEquals(GroupAlertStatus.QUERY_FAILED, failed.status(),
                "query failure should have an independent status");
        TestSupport.assertFalse(failed.ruleMatched(), "query failure must not match shortage rule");
        TestSupport.assertFalse(failed.shouldShowDialog(), "query failure must not show shortage dialog");
        TestSupport.assertEquals(0, failed.continuousMatchedSeconds(),
                "query failure should not keep previous shortage seconds");
        TestSupport.assertEquals(null, state.conditionFirstMatchedAt(),
                "query failure should clear previous shortage start time");
        TestSupport.assertEquals(BASE_TIME.plusMinutes(5), state.lastCheckedAt(),
                "query failure should still update last checked time");
        TestSupport.assertContains(failed.message(), "查询失败", "failure message should be operator visible");
        TestSupport.assertNotContains(failed.message(), "EMPTY", "failure message must not look like material state");
        TestSupport.assertNotContains(failed.message(), "ACTIVE_ALERT", "failure message must not leak alert enum");
    }

    private static void recoveredShortageAfterQueryFailureStartsTimingAgain() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME.plusMinutes(4));
        GroupMonitorLogic.queryFailed(group, state, BASE_TIME.plusMinutes(5), "查询失败：连接超时");

        GroupEvaluation recoveredButStillShortage = GroupMonitorLogic.evaluate(
                group,
                shortageRecords(),
                state,
                BASE_TIME.plusMinutes(6));

        TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, recoveredButStillShortage.status(),
                "shortage after query recovery should start pending again");
        TestSupport.assertEquals(0, recoveredButStillShortage.continuousMatchedSeconds(),
                "shortage timing must restart after query recovery");
        TestSupport.assertFalse(recoveredButStillShortage.shouldShowDialog(),
                "recovered first shortage check must not alert immediately");
    }

    private static void recoveredHealthyGroupAfterQueryFailureReturnsNormal() {
        PointGroupDefinition group = defaultGroup(3, 5);
        GroupRuntimeState state = new GroupRuntimeState();
        GroupMonitorLogic.evaluate(group, shortageRecords(), state, BASE_TIME);
        GroupMonitorLogic.queryFailed(group, state, BASE_TIME.plusMinutes(1), "查询失败：连接超时");

        GroupEvaluation healthy = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
                record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
                record("BACKUP_POINT_003", "SHELF_BACKUP_003", 1, 0),
                record("BACKUP_POINT_004", "", 1, 0)), state, BASE_TIME.plusMinutes(2));

        TestSupport.assertEquals(GroupAlertStatus.NORMAL, healthy.status(),
                "healthy query recovery should return normal");
        TestSupport.assertEquals(0, healthy.continuousMatchedSeconds(),
                "healthy recovery should not have shortage timing");
        TestSupport.assertFalse(healthy.shouldShowDialog(), "healthy recovery should not show dialog");
    }

    private static void multipleUsePointsAllAvailableDoNotMatchUseCondition() {
        PointGroupDefinition group = multipleUsePointGroup();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("USE_POINT_002", "SHELF_USE_002", 1, 0),
                record("BACKUP_POINT_001", "", 1, 0)), new GroupRuntimeState(), BASE_TIME);

        TestSupport.assertFalse(evaluation.usePointEmpty(),
                "all available use points should keep the use condition false");
        TestSupport.assertFalse(evaluation.ruleMatched(),
                "backup shortage alone should not match when use points are available");
    }

    private static void anyUnavailableUsePointMatchesUseCondition() {
        PointGroupDefinition group = multipleUsePointGroup();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("USE_POINT_002", "", 1, 0),
                record("BACKUP_POINT_001", "", 1, 0)), new GroupRuntimeState(), BASE_TIME);

        TestSupport.assertTrue(evaluation.usePointEmpty(),
                "one unavailable use point should match the use condition");
        TestSupport.assertEquals(2, evaluation.usePointTotal(),
                "evaluation should expose the enabled use point total");
        TestSupport.assertEquals(1, evaluation.usePointAvailableCount(),
                "evaluation should expose the available use point count");
        TestSupport.assertEquals(1, evaluation.unavailableUsePoints().size(),
                "evaluation should identify each unavailable use point");
        TestSupport.assertEquals("USE_POINT_002", evaluation.unavailableUsePoints().get(0).pointCode(),
                "unavailable use point details should identify the land code");
        TestSupport.assertContains(evaluation.message(), "使用位有料 1/2",
                "summary should show the multi-use availability ratio");
        TestSupport.assertContains(evaluation.message(), "任一使用位无料：是",
                "summary should explain the any-use alarm condition");
        TestSupport.assertTrue(evaluation.ruleMatched(),
                "one unavailable use point plus backup shortage should match the full rule");
    }

    private static void missingUsePointMatchesUseCondition() {
        PointGroupDefinition group = multipleUsePointGroup();

        GroupEvaluation evaluation = GroupMonitorLogic.evaluate(group, List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "", 1, 0)), new GroupRuntimeState(), BASE_TIME);

        TestSupport.assertTrue(evaluation.usePointEmpty(),
                "a use point missing from the query result should match the use condition");
        TestSupport.assertTrue(evaluation.ruleMatched(),
                "a missing use point plus backup shortage should match the full rule");
    }

    private static PointGroupDefinition defaultGroup(int minBackupAvailable, int durationMinutes) {
        return defaultGroup(minBackupAvailable, durationMinutes,
                new GroupAlertRule(true, true, minBackupAvailable, durationMinutes));
    }

    private static PointGroupDefinition defaultGroup(
            int minBackupAvailable,
            int durationMinutes,
            GroupAlertRule rule) {
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
                rule);
    }

    private static PointGroupDefinition groupWithDisabledBackup(int minBackupAvailable, int durationMinutes) {
        return new PointGroupDefinition(
                "group-disabled-backup",
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
                        new GroupMonitorPoint("backup-004", "BACKUP_POINT_004", "Backup disabled", PointRole.BACKUP, false, 5)),
                new GroupAlertRule(true, true, minBackupAvailable, durationMinutes));
    }

    private static PointGroupDefinition multipleUsePointGroup() {
        return new PointGroupDefinition(
                "group-multiple-use",
                "Area A",
                "Two Feed Ports",
                "Material A",
                true,
                60,
                List.of(
                        new GroupMonitorPoint("use-001", "USE_POINT_001", "Use 1", PointRole.USE, true, 1),
                        new GroupMonitorPoint("use-002", "USE_POINT_002", "Use 2", PointRole.USE, true, 2),
                        new GroupMonitorPoint("backup-001", "BACKUP_POINT_001", "Backup 1", PointRole.BACKUP, true, 3)),
                new GroupAlertRule(true, true, 1, 5, true));
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

        static void assertNotContains(String text, String needle, String message) {
            if (text != null && text.contains(needle)) {
                throw new AssertionError(message + " text=" + text + " needle=" + needle);
            }
        }

        static void assertThrows(Class<? extends Throwable> expected, ThrowingRunnable runnable, String message) {
            try {
                runnable.run();
            } catch (Throwable actual) {
                if (expected.isInstance(actual)) {
                    return;
                }
                throw new AssertionError(message + " expected=" + expected.getName()
                        + " actual=" + actual.getClass().getName());
            }
            throw new AssertionError(message + " expected exception " + expected.getName());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
