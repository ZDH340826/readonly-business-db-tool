package com.local.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public final class MonitoringSessionRaceTest {
    public static void main(String[] args) throws Exception {
        stoppedSessionCannotRecordQueryFailureAfterFetchReturns();
        switchedConnectionCannotBeOverwrittenByOldTask();
        staleConnectionTestResultCannotOverwriteNewConnection();
        System.out.println("MonitoringSessionRaceTest PASS");
    }

    private static void stoppedSessionCannotRecordQueryFailureAfterFetchReturns() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        ShelfPointMonitorApp.MonitoringSession[] sessionRef = new ShelfPointMonitorApp.MonitoringSession[1];
        Path logDir = Files.createTempDirectory("stopped-session-logs");
        PointGroupDefinition group = group("stopped-session-group");
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            ShelfPointMonitorApp.MonitoringSession session = session(Files.createTempDirectory("stopped-session").resolve("db"), 1L);
            sessionRef[0] = session;
            setField(app, "monitoringSession", session);
            setField(app, "monitoringGeneration", session.generation());
            setField(app, "pointGroups", new ArrayList<>(List.of(group)));
            setField(app, "groupLogWriter", new GroupLogWriter(logDir));
            setField(app, "logPath", logDir.resolve("monitor.log"));
        });

        CountDownLatch fetchEntered = new CountDownLatch(1);
        CountDownLatch allowFailure = new CountDownLatch(1);
        ResultHolder result = new ResultHolder();
        Thread worker = new Thread(() -> {
            try {
                result.value = appRef[0].checkGroupsWithFetcher(
                        sessionRef[0],
                        List.of(group),
                        LocalDateTime.of(2026, 7, 10, 10, 0),
                        "自动检测",
                        ignored -> {
                            fetchEntered.countDown();
                            await(allowFailure);
                            throw new IllegalStateException("连接超时");
                        });
            } catch (Throwable throwable) {
                result.failure = throwable;
            }
        }, "stale-monitor-task");
        worker.start();
        await(fetchEntered);
        runOnEdtAndWait(() -> invoke(appRef[0], "stopMonitoring", new Class<?>[0]));
        allowFailure.countDown();
        worker.join(5_000L);
        CountDownLatch ioDrained = new CountDownLatch(1);
        appRef[0].runIoInBackground(ioDrained::countDown);
        await(ioDrained);

        try {
            TestSupport.assertFalse(worker.isAlive(), "stale monitor task should finish");
            TestSupport.assertEquals(null, result.failure, "stale monitor task should not fail outward");
            TestSupport.assertTrue(sessionRef[0].isClosed(), "stop monitoring should close the session");
            TestSupport.assertEquals(0, result.value.evaluations().size(),
                    "closed session must not produce query failure evaluation");
            TestSupport.assertEquals(0, mapField(appRef[0], "lastGroupStatuses").size(),
                    "closed session must not write alert status");
            TestSupport.assertEquals(0, mapField(appRef[0], "lastGroupEvaluations").size(),
                    "closed session must not write latest evaluation");
            TestSupport.assertFalse(Files.exists(logDir.resolve("check-log.csv")),
                    "closed session must not write a check log row");
            TestSupport.assertFalse(Files.exists(logDir.resolve("event-log.csv")),
                    "closed session must not write a query failure event");
            TestSupport.assertFalse(Files.exists(logDir.resolve("monitor.log")),
                    "closed session must not append monitor status output");
        } finally {
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void switchedConnectionCannotBeOverwrittenByOldTask() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        ShelfPointMonitorApp.MonitoringSession[] sessionRef = new ShelfPointMonitorApp.MonitoringSession[1];
        PointGroupDefinition group = group("switched-session-group");
        Path oldDb = Files.createTempDirectory("old-session").resolve("db");
        Path newDb = Files.createTempDirectory("new-session").resolve("db");
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            ShelfPointMonitorApp.MonitoringSession session = session(oldDb, 2L);
            sessionRef[0] = session;
            setField(app, "monitoringSession", session);
            setField(app, "monitoringGeneration", session.generation());
            setField(app, "currentProfile", profile("old", oldDb));
            setField(app, "pointGroups", new ArrayList<>(List.of(group)));
        });

        CountDownLatch fetchEntered = new CountDownLatch(1);
        CountDownLatch allowResult = new CountDownLatch(1);
        ResultHolder result = new ResultHolder();
        Thread worker = new Thread(() -> {
            try {
                result.value = appRef[0].checkGroupsWithFetcher(
                        sessionRef[0],
                        List.of(group),
                        LocalDateTime.of(2026, 7, 10, 10, 5),
                        "自动检测",
                        ignored -> {
                            fetchEntered.countDown();
                            await(allowResult);
                            return List.of();
                        });
            } catch (Throwable throwable) {
                result.failure = throwable;
            }
        }, "stale-switch-task");
        worker.start();
        await(fetchEntered);
        runOnEdtAndWait(() -> invoke(appRef[0], "applyCurrentConnection",
                new Class<?>[] {ConnectionProfile.class, char[].class, String.class},
                profile("new", newDb), "NewPassword".toCharArray(), "测试成功并正在使用"));
        allowResult.countDown();
        worker.join(5_000L);

        try {
            TestSupport.assertFalse(worker.isAlive(), "old task should finish after connection switch");
            TestSupport.assertEquals(null, result.failure, "old task should not fail outward");
            TestSupport.assertTrue(sessionRef[0].isClosed(), "connection switch should close old session");
            TestSupport.assertEquals(0, result.value.evaluations().size(),
                    "old task must not produce business evaluation after switch");
            ConnectionProfile activeProfile = fieldValue(appRef[0], "currentProfile", ConnectionProfile.class);
            TestSupport.assertEquals("new", activeProfile.id(), "old task must not overwrite new connection");
            TestSupport.assertEquals(0, mapField(appRef[0], "lastGroupStatuses").size(),
                    "old task must not write status after switch");
        } finally {
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void staleConnectionTestResultCannotOverwriteNewConnection() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        Path activeDb = Files.createTempDirectory("active-connection").resolve("db");
        Path staleDb = Files.createTempDirectory("stale-connection").resolve("db");
        char[] stalePassword = "StalePassword".toCharArray();
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            invoke(app, "applyCurrentConnection",
                    new Class<?>[] {ConnectionProfile.class, char[].class, String.class},
                    profile("active", activeDb), "ActivePassword".toCharArray(), "已选中当前连接");
            setField(app, "connectionOperationGeneration", 7L);
            invoke(app, "stopMonitoring", new Class<?>[0]);
            invoke(app, "applyTestConnectionSuccess",
                    new Class<?>[] {long.class, ConnectionProfile.class, char[].class, String.class},
                    7L,
                    profile("stale", staleDb),
                    stalePassword,
                    "database=local-test, user=stale-user");
        });

        try {
            ConnectionProfile activeProfile = fieldValue(appRef[0], "currentProfile", ConnectionProfile.class);
            TestSupport.assertEquals("active", activeProfile.id(),
                    "a completed old connection test must not replace the newer current connection");
            assertAllZero(stalePassword,
                    "a discarded connection-test password snapshot must be cleared before returning to the UI");
        } finally {
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static ShelfPointMonitorApp.MonitoringSession session(Path localDb, long generation) {
        return new ShelfPointMonitorApp.MonitoringSession(
                DbConfig.localTest(localDb.toString(), 60), "SnapshotPassword".toCharArray(), generation);
    }

    private static ConnectionProfile profile(String id, Path localDb) {
        return new ConnectionProfile(id, id, "h2", "local", 1, "local-test", "public", "sa", "disable",
                localDb.toString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> mapField(Object target, String name) throws Exception {
        return fieldValue(target, name, Map.class);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T fieldValue(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw exception;
        }
    }

    private static PointGroupDefinition group(String id) {
        return new PointGroupDefinition(
                id,
                "区域",
                "点位组",
                "物料",
                true,
                60,
                List.of(
                        new GroupMonitorPoint(id + "-use", "USE_" + id, "使用位", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup", "BACKUP_" + id, "备用位", PointRole.BACKUP, true, 2)),
                new GroupAlertRule(true, true, 0, 1, true));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted", exception);
        }
    }

    private static void assertAllZero(char[] value, String message) {
        for (char character : value) {
            if (character != '\0') {
                throw new AssertionError(message + " expected all zero chars");
            }
        }
    }

    private static void runOnEdtAndWait(ThrowingRunnable runnable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception) {
                throw (Exception) cause.getCause();
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw exception;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class ResultHolder {
        private ShelfPointMonitorApp.GroupCheckRunResult value;
        private Throwable failure;
    }

    private static final class TestSupport {
        private static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        private static void assertFalse(boolean condition, String message) {
            assertTrue(!condition, message);
        }

        private static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }
    }
}
