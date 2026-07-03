package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class GroupLogWriterTest {
    public static void main(String[] args) throws Exception {
        writesCheckLogWithHeaderAndEscapedRows();
        writesEventLogWithHeaderOnlyOnce();
        System.out.println("GroupLogWriterTest PASS");
    }

    private static void writesCheckLogWithHeaderAndEscapedRows() throws Exception {
        Path logDir = Files.createTempDirectory("group-log-check-test");
        GroupLogWriter writer = new GroupLogWriter(logDir);

        writer.appendCheck(LocalDateTime.of(2026, 6, 30, 10, 15), evaluation("group,001", GroupAlertStatus.ACTIVE_ALERT));

        Path checkLog = logDir.resolve("check-log.csv");
        List<String> rows = Files.readAllLines(checkLog);
        TestSupport.assertEquals(2, rows.size(), "check log should contain header and one row");
        TestSupport.assertEquals(
                "checked_at,group_id,area_name,group_name,material_name,status,use_point_empty,backup_total,backup_available,backup_missing,rule_matched,continuous_minutes,message",
                rows.get(0),
                "check log header should be stable");
        TestSupport.assertContains(rows.get(1), "2026-06-30T10:15", "check row should include timestamp");
        TestSupport.assertContains(rows.get(1), "\"group,001\"", "check row should escape comma");
        TestSupport.assertContains(rows.get(1), "ACTIVE_ALERT", "check row should include status");
    }

    private static void writesEventLogWithHeaderOnlyOnce() throws Exception {
        Path logDir = Files.createTempDirectory("group-log-event-test");
        GroupLogWriter writer = new GroupLogWriter(logDir);

        writer.appendEvent(LocalDateTime.of(2026, 6, 30, 10, 16), "ALERT_OPEN", evaluation("group-001", GroupAlertStatus.ACTIVE_ALERT));
        writer.appendEvent(LocalDateTime.of(2026, 6, 30, 10, 18), "RECOVERED", evaluation("group-001", GroupAlertStatus.NORMAL));

        Path eventLog = logDir.resolve("event-log.csv");
        List<String> rows = Files.readAllLines(eventLog);
        TestSupport.assertEquals(3, rows.size(), "event log should contain header and two rows");
        TestSupport.assertEquals(
                "event_at,event_type,group_id,area_name,group_name,material_name,status,message",
                rows.get(0),
                "event log header should be stable");
        TestSupport.assertContains(rows.get(1), "ALERT_OPEN", "first event should be written");
        TestSupport.assertContains(rows.get(2), "RECOVERED", "second event should be appended");
    }

    private static GroupEvaluation evaluation(String groupId, GroupAlertStatus status) {
        return new GroupEvaluation(
                groupId,
                "Area A",
                "Rear, Panel",
                "Material A",
                status,
                true,
                4,
                2,
                2,
                status != GroupAlertStatus.NORMAL,
                status == GroupAlertStatus.NORMAL ? 0 : 300,
                300,
                List.of(),
                status == GroupAlertStatus.ACTIVE_ALERT,
                "usePoint=USE_POINT_001, backupAvailable=2/4");
    }

    private static final class TestSupport {
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
