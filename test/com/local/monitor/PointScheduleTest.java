package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class PointScheduleTest {
    public static void main(String[] args) {
        onlyDuePointsAreReturned();
        forceCheckReturnsAllPoints();
        defaultIntervalIsFiveMinutes();
        System.out.println("PointScheduleTest PASS");
    }

    private static void onlyDuePointsAreReturned() {
        PointDefinition fast = new PointDefinition("FAST", "fast", 1);
        PointDefinition slow = new PointDefinition("SLOW", "slow", 10);
        PointSchedule schedule = new PointSchedule();
        LocalDateTime start = LocalDateTime.of(2026, 6, 28, 17, 30);

        List<PointDefinition> first = schedule.duePoints(List.of(fast, slow), start);
        TestSupport.assertEquals(2, first.size(), "new points should be due immediately");
        schedule.markChecked(first, start);

        List<PointDefinition> afterOneMinute = schedule.duePoints(List.of(fast, slow), start.plusMinutes(1));
        TestSupport.assertEquals(1, afterOneMinute.size(), "only fast point should be due after one minute");
        TestSupport.assertEquals("FAST", afterOneMinute.get(0).code(), "fast point should be due");

        List<PointDefinition> afterTenMinutes = schedule.duePoints(List.of(fast, slow), start.plusMinutes(10));
        TestSupport.assertEquals(2, afterTenMinutes.size(), "both points should be due after ten minutes");
    }

    private static void forceCheckReturnsAllPoints() {
        PointDefinition fast = new PointDefinition("FAST", "fast", 1);
        PointDefinition slow = new PointDefinition("SLOW", "slow", 10);
        PointSchedule schedule = new PointSchedule();
        LocalDateTime start = LocalDateTime.of(2026, 6, 28, 17, 30);

        schedule.markChecked(List.of(fast, slow), start);
        List<PointDefinition> forced = schedule.forceAll(List.of(fast, slow));

        TestSupport.assertEquals(2, forced.size(), "force check should return all points");
    }

    private static void defaultIntervalIsFiveMinutes() {
        PointDefinition point = new PointDefinition("P1", "point");
        TestSupport.assertEquals(5, point.intervalMinutes(), "old two-argument point definition defaults to five minutes");
    }

    private static final class TestSupport {
        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }
    }
}


