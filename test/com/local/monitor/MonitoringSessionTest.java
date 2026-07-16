package com.local.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class MonitoringSessionTest {
    public static void main(String[] args) throws Exception {
        monitoringSessionCopiesClosesAndProtectsTaskPassword();
        startMonitoringUsesImmutablePasswordSnapshot();
        switchingConnectionStopsSessionAndClearsPassword();
        System.out.println("MonitoringSessionTest PASS");
    }

    private static void monitoringSessionCopiesClosesAndProtectsTaskPassword() throws Exception {
        DbConfig config = DbConfig.localTest(Files.createTempDirectory("session-copy").resolve("db").toString(), 60);
        char[] password = "StartSecret".toCharArray();
        ShelfPointMonitorApp.MonitoringSession session =
                new ShelfPointMonitorApp.MonitoringSession(config, password, 9L);
        password[0] = 'X';
        char[] taskPassword = session.copyPasswordForTask();
        assertEquals('S', taskPassword[0], "session should copy the supplied password");
        taskPassword[0] = 'Y';
        assertEquals('S', session.copyPasswordForTask()[0],
                "each task must receive an independent password copy");
        ShelfPointMonitorApp.MonitoringSession.clearTaskPassword(taskPassword);
        assertAllZero(taskPassword, "task-level password copy must be cleared after the task completes");
        session.close();
        assertTrue(session.isClosed(), "closed session must report closed state");
        assertEquals(0, session.copyPasswordForTask().length,
                "closed session must not issue new task password copies");
        assertEquals('X', password[0], "caller password remains independently owned");
    }

    private static void startMonitoringUsesImmutablePasswordSnapshot() throws Exception {
        CapturingScheduledExecutor monitorExecutor = new CapturingScheduledExecutor();
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        Path localDb = Files.createTempDirectory("session-start").resolve("db");
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            setField(app, "monitorExecutor", monitorExecutor);
            setField(app, "currentProfile", localProfile(localDb));
            setField(app, "currentPassword", "Snapshot-123".toCharArray());
            PointGroupDefinition group = group("session-group");
            setField(app, "pointGroups", new ArrayList<>(List.of(group)));
            invoke(app, "refreshGroupList", new Class<?>[] {String.class}, group.id());
            invoke(app, "populateSelectedGroup", new Class<?>[0]);
            invoke(app, "startMonitoring", new Class<?>[0]);
            setField(app, "currentPassword", "Changed-456".toCharArray());
        });

        try {
            assertEquals(1, monitorExecutor.scheduledCount(), "start monitoring should schedule one monitor task");
            ShelfPointMonitorApp.MonitoringSession session =
                    fieldValue(appRef[0], "monitoringSession", ShelfPointMonitorApp.MonitoringSession.class);
            assertEquals("Snapshot-123", new String(session.copyPasswordForTask()),
                    "background session must keep the password captured at start");
        } finally {
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void switchingConnectionStopsSessionAndClearsPassword() throws Exception {
        CapturingScheduledExecutor monitorExecutor = new CapturingScheduledExecutor();
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        ShelfPointMonitorApp.MonitoringSession[] oldSession = new ShelfPointMonitorApp.MonitoringSession[1];
        Path localDb = Files.createTempDirectory("session-switch").resolve("db");
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            setField(app, "monitorExecutor", monitorExecutor);
            setField(app, "currentProfile", localProfile(localDb));
            setField(app, "currentPassword", "OldPassword".toCharArray());
            PointGroupDefinition group = group("switch-group");
            setField(app, "pointGroups", new ArrayList<>(List.of(group)));
            invoke(app, "refreshGroupList", new Class<?>[] {String.class}, group.id());
            invoke(app, "populateSelectedGroup", new Class<?>[0]);
            invoke(app, "startMonitoring", new Class<?>[0]);
            oldSession[0] = fieldValue(app, "monitoringSession", ShelfPointMonitorApp.MonitoringSession.class);

            setText(fieldValue(app, "profileIdField", JTextField.class), "new-local");
            setText(fieldValue(app, "profileNameField", JTextField.class), "New Local");
            setText(fieldValue(app, "profileHostField", JTextField.class), "local");
            setText(fieldValue(app, "profileDatabaseField", JTextField.class), "local-test");
            setText(fieldValue(app, "profileSchemaField", JTextField.class), "public");
            setText(fieldValue(app, "profileUserField", JTextField.class), "sa");
            setText(fieldValue(app, "profileLocalPathField", JTextField.class), localDb.resolve("new").toString());
            fieldValue(app, "profilePasswordField", JPasswordField.class).setText("NewPassword");
            invoke(app, "useProfileWithoutTest", new Class<?>[0]);
        });

        try {
            assertTrue(oldSession[0].isClosed(), "old session should be closed after connection switch");
            assertEquals(0, oldSession[0].copyPasswordForTask().length,
                    "closed old session should not issue a password to a later task");
            Object activeSession = fieldValue(appRef[0], "monitoringSession", Object.class);
            assertEquals(null, activeSession, "connection switch should stop the old monitoring session");
            assertTrue(monitorExecutor.cancelled(), "connection switch should cancel the old scheduled task");
        } finally {
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static ConnectionProfile localProfile(Path localPath) {
        return new ConnectionProfile("local", "Local", "h2", "local", 1, "local-test", "public", "sa", "disable",
                localPath.toString());
    }

    private static PointGroupDefinition group(String id) {
        return new PointGroupDefinition(
                id,
                "A区",
                "物料组",
                "物料",
                true,
                60,
                List.of(
                        new GroupMonitorPoint(id + "-use", "USE_" + id, "使用位", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup", "BACKUP_" + id, "备用位", PointRole.BACKUP, true, 2)),
                new GroupAlertRule(true, true, 1, 1, true));
    }

    private static void setText(JTextField field, String value) {
        field.setText(value);
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

    private static void runOnEdtAndWait(ThrowingRunnable runnable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception) {
                throw (Exception) cause.getCause();
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw ex;
        }
    }

    private static void assertAllZero(char[] value, String message) {
        for (char c : value) {
            if (c != '\0') {
                throw new AssertionError(message + " expected all zero chars");
            }
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class CapturingScheduledExecutor extends AbstractExecutorService
            implements ScheduledExecutorService {
        private int scheduledCount;
        private boolean shutdown;
        private boolean cancelled;

        @Override
        public void execute(Runnable command) {
        }

        int scheduledCount() {
            return scheduledCount;
        }

        boolean cancelled() {
            return cancelled;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            scheduledCount++;
            return new ScheduledFuture<Object>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    cancelled = true;
                    return true;
                }

                @Override
                public boolean isCancelled() {
                    return cancelled;
                }

                @Override
                public boolean isDone() {
                    return cancelled;
                }

                @Override
                public Object get() {
                    return null;
                }

                @Override
                public Object get(long timeout, TimeUnit unit) {
                    return null;
                }

                @Override
                public long getDelay(TimeUnit unit) {
                    return 0L;
                }

                @Override
                public int compareTo(java.util.concurrent.Delayed other) {
                    return 0;
                }
            };
        }
    }
}
