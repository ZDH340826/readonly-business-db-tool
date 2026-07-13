package com.local.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public final class ExecutorSeparationTest {
    public static void main(String[] args) throws Exception {
        appendStatusQueuesFileWriteOnIoExecutor();
        logWriteFailureIsReportedWithoutRecursiveSubmission();
        closedIoExecutorDoesNotBreakStatusUpdate();
        logRefreshUsesIoExecutorOnly();
        healthRefreshUsesIoExecutorOnly();
        slowLogReadDoesNotOccupyMonitorExecutor();
        disposeStopsBothExecutors();
        System.out.println("ExecutorSeparationTest PASS");
    }

    private static void appendStatusQueuesFileWriteOnIoExecutor() throws Exception {
        CapturingScheduledExecutor ioExecutor = new CapturingScheduledExecutor();
        int[] countBeforeDispose = new int[1];
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                setField(app, "ioExecutor", ioExecutor);
                for (int i = 0; i < 100; i++) {
                    invoke(app, "appendStatus", new Class<?>[] {String.class}, "status " + i);
                }
                countBeforeDispose[0] = ioExecutor.commandCount();
            } finally {
                app.dispose();
            }
        });
        TestSupport.assertEquals(100, countBeforeDispose[0],
                "appendStatus must queue a file write on io executor instead of writing on the caller thread");
    }

    private static void logWriteFailureIsReportedWithoutRecursiveSubmission() throws Exception {
        DirectScheduledExecutor ioExecutor = new DirectScheduledExecutor();
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            setField(app, "ioExecutor", ioExecutor);
            setField(app, "logPath", Files.createTempDirectory("monitor-log-directory"));
            invoke(app, "appendStatus", new Class<?>[] {String.class}, "查询失败：日志路径不可写");
        });
        try {
            runOnEdtAndWait(() -> {
                JTextArea statusArea = fieldValue(appRef[0], "statusArea", JTextArea.class);
                TestSupport.assertTrue(statusArea.getText().contains("日志写入失败"),
                        "write failure should be visible without calling appendStatus recursively");
            });
            TestSupport.assertEquals(1, ioExecutor.commandCount(),
                    "a failed log write must not submit a recursive log write task");
        } finally {
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void closedIoExecutorDoesNotBreakStatusUpdate() throws Exception {
        CapturingScheduledExecutor ioExecutor = new CapturingScheduledExecutor();
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                setField(app, "ioExecutor", ioExecutor);
                ioExecutor.shutdownNow();
                invoke(app, "appendStatus", new Class<?>[] {String.class}, "关闭后状态更新");
            } finally {
                app.dispose();
            }
        });
        TestSupport.assertTrue(ioExecutor.isShutdown(), "closed IO executor should remain shut down");
    }

    private static void logRefreshUsesIoExecutorOnly() throws Exception {
        CapturingScheduledExecutor monitorExecutor = new CapturingScheduledExecutor();
        CapturingScheduledExecutor ioExecutor = new CapturingScheduledExecutor();
        int[] monitorCountBeforeDispose = new int[1];
        int[] ioCountBeforeDispose = new int[1];
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                setField(app, "monitorExecutor", monitorExecutor);
                setField(app, "ioExecutor", ioExecutor);
                invoke(app, "loadSystemLogs", new Class<?>[0]);
                monitorCountBeforeDispose[0] = monitorExecutor.commandCount();
                ioCountBeforeDispose[0] = ioExecutor.commandCount();
            } finally {
                app.dispose();
            }
        });
        TestSupport.assertEquals(0, monitorCountBeforeDispose[0],
                "log refresh must not use monitor executor");
        TestSupport.assertEquals(1, ioCountBeforeDispose[0],
                "log refresh should use io executor");
    }

    private static void healthRefreshUsesIoExecutorOnly() throws Exception {
        CapturingScheduledExecutor monitorExecutor = new CapturingScheduledExecutor();
        CapturingScheduledExecutor ioExecutor = new CapturingScheduledExecutor();
        int[] monitorCountBeforeDispose = new int[1];
        int[] ioCountBeforeDispose = new int[1];
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                setField(app, "monitorExecutor", monitorExecutor);
                setField(app, "ioExecutor", ioExecutor);
                invoke(app, "refreshSystemHealthStatus", new Class<?>[0]);
                monitorCountBeforeDispose[0] = monitorExecutor.commandCount();
                ioCountBeforeDispose[0] = ioExecutor.commandCount();
            } finally {
                app.dispose();
            }
        });
        TestSupport.assertEquals(0, monitorCountBeforeDispose[0],
                "health checks must not use monitor executor for file-system access");
        TestSupport.assertEquals(1, ioCountBeforeDispose[0],
                "health checks should be queued on io executor");
    }

    private static void slowLogReadDoesNotOccupyMonitorExecutor() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        CountDownLatch ioStarted = new CountDownLatch(1);
        CountDownLatch releaseIo = new CountDownLatch(1);
        CountDownLatch monitorRan = new CountDownLatch(1);
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            invoke(app, "runIoInBackground", new Class<?>[] {ShelfPointMonitorApp.CheckedRunnable.class},
                    (ShelfPointMonitorApp.CheckedRunnable) () -> {
                        ioStarted.countDown();
                        await(releaseIo);
                    });
        });
        try {
            await(ioStarted);
            appRef[0].runMonitorInBackground(monitorRan::countDown);
            await(monitorRan);
        } finally {
            releaseIo.countDown();
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void disposeStopsBothExecutors() throws Exception {
        CapturingScheduledExecutor monitorExecutor = new CapturingScheduledExecutor();
        CapturingScheduledExecutor ioExecutor = new CapturingScheduledExecutor();
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            setField(app, "monitorExecutor", monitorExecutor);
            setField(app, "ioExecutor", ioExecutor);
            app.dispose();
        });
        TestSupport.assertTrue(monitorExecutor.isShutdown(), "dispose should shutdown monitor executor");
        TestSupport.assertTrue(ioExecutor.isShutdown(), "dispose should shutdown io executor");
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

    private static void await(CountDownLatch latch) throws Exception {
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("timed out waiting for executor coordination");
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class CapturingScheduledExecutor extends AbstractExecutorService
            implements ScheduledExecutorService {
        private final List<Runnable> commands = new ArrayList<>();
        private boolean shutdown;

        @Override
        public void execute(Runnable command) {
            commands.add(command);
        }

        int commandCount() {
            return commands.size();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.copyOf(commands);
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
            throw new UnsupportedOperationException();
        }
    }

    private static final class DirectScheduledExecutor extends AbstractExecutorService
            implements ScheduledExecutorService {
        private int commandCount;
        private boolean shutdown;

        @Override
        public void execute(Runnable command) {
            commandCount++;
            command.run();
        }

        int commandCount() {
            return commandCount;
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
            throw new UnsupportedOperationException();
        }
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
