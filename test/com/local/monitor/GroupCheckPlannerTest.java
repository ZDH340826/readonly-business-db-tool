package com.local.monitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GroupCheckPlannerTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 3, 10, 0);

    public static void main(String[] args) throws Exception {
        uncheckedEnabledGroupIsDue();
        disabledGroupIsNotDue();
        fiveMinuteIntervalIsNotDueAtFourMinutesFiftyNineSeconds();
        fiveMinuteIntervalIsDueAtBoundary();
        clockRollbackIsNotDue();
        System.out.println("GroupCheckPlannerTest PASS");
    }

    private static void uncheckedEnabledGroupIsDue() throws Exception {
        PointGroupDefinition group = group("group-001", true, 300);

        List<PointGroupDefinition> due = dueGroups(List.of(group), Map.of(), NOW);

        TestSupport.assertEquals(List.of(group), due, "unchecked enabled group should be due immediately");
    }

    private static void disabledGroupIsNotDue() throws Exception {
        PointGroupDefinition group = group("group-001", false, 300);

        List<PointGroupDefinition> due = dueGroups(List.of(group), Map.of(), NOW);

        TestSupport.assertTrue(due.isEmpty(), "disabled group should not be due");
    }

    private static void fiveMinuteIntervalIsNotDueAtFourMinutesFiftyNineSeconds() throws Exception {
        PointGroupDefinition group = group("group-001", true, 300);
        Map<String, GroupRuntimeState> states = new LinkedHashMap<>();
        states.put(group.id(), stateCheckedAt(NOW.minusSeconds(299)));

        List<PointGroupDefinition> due = dueGroups(List.of(group), states, NOW);

        TestSupport.assertTrue(due.isEmpty(), "five minute group should not be due at 4:59");
    }

    private static void fiveMinuteIntervalIsDueAtBoundary() throws Exception {
        PointGroupDefinition first = group("group-001", true, 300);
        PointGroupDefinition second = group("group-002", true, 300);
        Map<String, GroupRuntimeState> states = new LinkedHashMap<>();
        states.put(first.id(), stateCheckedAt(NOW.minusMinutes(5)));
        states.put(second.id(), stateCheckedAt(NOW.minusMinutes(5)));

        List<PointGroupDefinition> due = dueGroups(List.of(first, second), states, NOW);

        TestSupport.assertEquals(List.of(first, second), due, "due groups should keep input order at boundary");
    }

    private static void clockRollbackIsNotDue() throws Exception {
        PointGroupDefinition group = group("group-001", true, 300);
        Map<String, GroupRuntimeState> states = new LinkedHashMap<>();
        states.put(group.id(), stateCheckedAt(NOW.plusSeconds(1)));

        List<PointGroupDefinition> due = dueGroups(List.of(group), states, NOW);

        TestSupport.assertTrue(due.isEmpty(), "clock rollback should not make a group due");
    }

    @SuppressWarnings("unchecked")
    private static List<PointGroupDefinition> dueGroups(
            List<PointGroupDefinition> groups,
            Map<String, GroupRuntimeState> states,
            LocalDateTime now) throws Exception {
        try {
            Class<?> plannerClass = Class.forName("com.local.monitor.GroupCheckPlanner");
            Method method = plannerClass.getDeclaredMethod("dueGroups", List.class, Map.class, LocalDateTime.class);
            return (List<PointGroupDefinition>) method.invoke(null, groups, states, now);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("GroupCheckPlanner should exist", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw ex;
        }
    }

    private static GroupRuntimeState stateCheckedAt(LocalDateTime checkedAt) {
        GroupRuntimeState state = new GroupRuntimeState();
        state.markChecked(checkedAt);
        return state;
    }

    private static PointGroupDefinition group(String id, boolean enabled, int checkIntervalSeconds) {
        return new PointGroupDefinition(
                id,
                "Area A",
                "Rear Panel " + id,
                "Material A",
                enabled,
                checkIntervalSeconds,
                List.of(
                        new GroupMonitorPoint(id + "-use", id + "-USE", "Use", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup", id + "-BACKUP", "Backup", PointRole.BACKUP, true, 2)),
                new GroupAlertRule(true, true, 1, 5));
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }
    }
}
