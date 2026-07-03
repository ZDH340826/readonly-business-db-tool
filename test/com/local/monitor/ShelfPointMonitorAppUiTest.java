package com.local.monitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;

public final class ShelfPointMonitorAppUiTest {
    public static void main(String[] args) throws Exception {
        appHasThreeNavigationPages();
        connectionPageUsesChineseOperatorLabels();
        alertPageContainsGroupPointTable();
        groupCheckIntervalFieldExists();
        updateSelectedGroupFromFormPreservesCustomInterval();
        populateSelectedGroupRoundsPartialMinutesUp();
        capturedMonitoredGroupsIgnoreLaterFormChanges();
        manualCheckUsesGroupSnapshotCapturedOnEdt();
        groupFetchFailureMarksCheckedAndContinues();
        gridBagPanelsDoNotOverlapCells();
        groupAlertTextsDoNotExposeTechnicalStatusNames();
        System.out.println("ShelfPointMonitorAppUiTest PASS");
    }

    private static void appHasThreeNavigationPages() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                JList<?> navigation = findFirst(app.getContentPane(), JList.class);
                TestSupport.assertTrue(navigation != null, "app should have a navigation list");
                TestSupport.assertEquals(3, navigation.getModel().getSize(), "app should have three navigation pages");
                TestSupport.assertEquals("连接管理", navigation.getModel().getElementAt(0), "first page should be connection management");
                TestSupport.assertEquals("数据库浏览器", navigation.getModel().getElementAt(1), "second page should be database browser");
                TestSupport.assertEquals("点位缺料报警", navigation.getModel().getElementAt(2), "third page should be point alert");
            } finally {
                app.dispose();
            }
        });
    }

    private static void gridBagPanelsDoNotOverlapCells() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                scanGridBagContainers(app.getContentPane());
            } finally {
                app.dispose();
            }
        });
    }

    private static void connectionPageUsesChineseOperatorLabels() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains("连接ID："), "connection page should show Chinese ID label");
                TestSupport.assertTrue(texts.contains("连接名称："), "connection page should show Chinese name label");
                TestSupport.assertTrue(texts.contains("数据库类型："), "connection page should show Chinese type label");
                TestSupport.assertTrue(texts.contains("服务器地址/IP："), "connection page should show Chinese host label");
                TestSupport.assertTrue(texts.contains("端口："), "connection page should show Chinese port label");
                TestSupport.assertTrue(texts.contains("数据库名："), "connection page should show Chinese database label");
                TestSupport.assertTrue(texts.contains("数据库空间/Schema："), "connection page should show Chinese schema label");
                TestSupport.assertTrue(texts.contains("用户名："), "connection page should show Chinese user label");
                TestSupport.assertTrue(texts.contains("密码："), "connection page should show Chinese password label");
                TestSupport.assertTrue(texts.contains("SSL模式："), "connection page should show Chinese SSL label");
                TestSupport.assertTrue(texts.contains("新建连接"), "connection page should show Chinese new button");
                TestSupport.assertTrue(texts.contains("测试连接并使用"), "connection page should show Chinese test button");
            } finally {
                app.dispose();
            }
        });
    }

    private static void alertPageContainsGroupPointTable() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                TestSupport.assertTrue(hasGroupPointTable(app.getContentPane()),
                        "alert page should contain the group point table");
            } finally {
                app.dispose();
            }
        });
    }

    private static void groupCheckIntervalFieldExists() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains("检测周期(分钟)："),
                        "alert page should show group check interval label");
                JSpinner spinner = fieldValue(app, "groupCheckIntervalMinutesSpinner", JSpinner.class);
                TestSupport.assertTrue(spinner != null, "group check interval spinner should exist");
            } finally {
                app.dispose();
            }
        });
    }

    private static void updateSelectedGroupFromFormPreservesCustomInterval() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                PointGroupDefinition source = group("group-001", 300);
                setField(app, "pointGroups", new ArrayList<>(List.of(source)));
                invoke(app, "refreshGroupList", new Class<?>[] {String.class}, source.id());
                invoke(app, "populateSelectedGroup", new Class<?>[0]);

                JSpinner spinner = fieldValue(app, "groupCheckIntervalMinutesSpinner", JSpinner.class);
                TestSupport.assertEquals(5, spinner.getValue(),
                        "populate should load group check interval minutes");

                spinner.setValue(10);
                invoke(app, "updateSelectedGroupFromForm", new Class<?>[0]);

                List<?> updatedGroups = fieldValue(app, "pointGroups", List.class);
                PointGroupDefinition updated = (PointGroupDefinition) updatedGroups.get(0);
                TestSupport.assertEquals(600, updated.checkIntervalSeconds(),
                        "group check interval should be saved from spinner minutes");
            } finally {
                app.dispose();
            }
        });
    }

    private static void populateSelectedGroupRoundsPartialMinutesUp() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                PointGroupDefinition source = group("group-001", 90);
                setField(app, "pointGroups", new ArrayList<>(List.of(source)));
                invoke(app, "refreshGroupList", new Class<?>[] {String.class}, source.id());
                invoke(app, "populateSelectedGroup", new Class<?>[0]);

                JSpinner spinner = fieldValue(app, "groupCheckIntervalMinutesSpinner", JSpinner.class);
                TestSupport.assertEquals(2, spinner.getValue(),
                        "populate should round partial minute intervals up");
            } finally {
                app.dispose();
            }
        });
    }

    private static void capturedMonitoredGroupsIgnoreLaterFormChanges() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                PointGroupDefinition source = group("group-001", 600);
                setField(app, "pointGroups", new ArrayList<>(List.of(source)));
                invoke(app, "refreshGroupList", new Class<?>[] {String.class}, source.id());
                invoke(app, "populateSelectedGroup", new Class<?>[0]);

                JSpinner spinner = fieldValue(app, "groupCheckIntervalMinutesSpinner", JSpinner.class);
                TestSupport.assertEquals(10, spinner.getValue(),
                        "test setup should start from a ten minute interval");
                invoke(app, "captureMonitoredGroupsForTest", new Class<?>[0]);

                spinner.setValue(1);
                invoke(app, "updateSelectedGroupFromForm", new Class<?>[0]);

                @SuppressWarnings("unchecked")
                List<PointGroupDefinition> monitoredGroups =
                        (List<PointGroupDefinition>) invoke(app, "monitoredGroupsSnapshotForTest", new Class<?>[0]);
                TestSupport.assertEquals(600, monitoredGroups.get(0).checkIntervalSeconds(),
                        "monitoring should keep the captured interval snapshot");

                List<?> currentGroups = fieldValue(app, "pointGroups", List.class);
                PointGroupDefinition current = (PointGroupDefinition) currentGroups.get(0);
                TestSupport.assertEquals(60, current.checkIntervalSeconds(),
                        "test setup should mutate the form-backed group after capture");
            } finally {
                app.dispose();
            }
        });
    }

    private static void manualCheckUsesGroupSnapshotCapturedOnEdt() throws Exception {
        CapturingScheduledExecutor executor = new CapturingScheduledExecutor();
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        Path localDb = Files.createTempDirectory("manual-check-db").resolve("local-test-db");

        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            PointGroupDefinition source = group("group-001", 600);
            setField(app, "executor", executor);
            setField(app, "currentProfile", new ConnectionProfile(
                    "local",
                    "Local",
                    "h2",
                    "local",
                    1,
                    "local-test",
                    "public",
                    "sa",
                    "disable",
                    localDb.toString()));
            setField(app, "pointGroups", new ArrayList<>(List.of(source)));
            invoke(app, "refreshGroupList", new Class<?>[] {String.class}, source.id());
            invoke(app, "populateSelectedGroup", new Class<?>[0]);

            JSpinner spinner = fieldValue(app, "groupCheckIntervalMinutesSpinner", JSpinner.class);
            TestSupport.assertEquals(10, spinner.getValue(), "test setup should start with ten minute group interval");
            JButton checkButton = fieldValue(app, "checkButton", JButton.class);
            checkButton.doClick();
            TestSupport.assertEquals(1, executor.capturedCount(), "manual check should schedule one background job");

            spinner.setValue(1);
        });

        try {
            executor.runCapturedOnWorker();
            runOnEdtAndWait(() -> {
                @SuppressWarnings("unchecked")
                List<PointGroupDefinition> currentGroups =
                        (List<PointGroupDefinition>) fieldValue(appRef[0], "pointGroups", List.class);
                TestSupport.assertEquals(600, currentGroups.get(0).checkIntervalSeconds(),
                        "manual background check must use the pre-click group snapshot, not later Swing form edits");
            });
        } finally {
            executor.shutdownNow();
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void groupFetchFailureMarksCheckedAndContinues() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        runOnEdtAndWait(() -> appRef[0] = new ShelfPointMonitorApp());
        try {
            ShelfPointMonitorApp app = appRef[0];
            LocalDateTime now = LocalDateTime.of(2026, 7, 3, 10, 15);
            PointGroupDefinition failingGroup = group("group-fail", 60);
            PointGroupDefinition healthyGroup = group("group-ok", 60);

            ShelfPointMonitorApp.GroupCheckRunResult result = app.checkGroupsWithFetcher(
                    List.of(failingGroup, healthyGroup),
                    now,
                    "test",
                    group -> {
                        if (group.id().equals(failingGroup.id())) {
                            throw new IllegalStateException("fetch failed");
                        }
                        return healthyRecords();
                    });

            @SuppressWarnings("unchecked")
            Map<String, GroupRuntimeState> states =
                    (Map<String, GroupRuntimeState>) fieldValue(app, "groupStates", Map.class);
            TestSupport.assertEquals(now, states.get(failingGroup.id()).lastCheckedAt(),
                    "failed group should be marked checked to avoid immediate retry loops");
            TestSupport.assertEquals(now, states.get(healthyGroup.id()).lastCheckedAt(),
                    "successful group should also be marked checked");
            TestSupport.assertEquals(1, result.checkedGroups(), "successful group should still be processed");
            TestSupport.assertEquals(1, result.failedGroups(), "failed group should be counted");
            TestSupport.assertEquals(1, result.evaluations().size(),
                    "failed fetch should not create a group evaluation");
            TestSupport.assertEquals(healthyGroup.id(), result.evaluations().get(0).groupId(),
                    "only the successful group should be evaluated");
            TestSupport.assertFalse(result.dialogRequested(), "failed fetch should not request a dialog");

            @SuppressWarnings("unchecked")
            Map<String, GroupAlertStatus> statuses =
                    (Map<String, GroupAlertStatus>) fieldValue(app, "lastGroupStatuses", Map.class);
            TestSupport.assertFalse(statuses.containsKey(failingGroup.id()),
                    "failed group should not update last alert status without an evaluation");
            TestSupport.assertEquals(GroupAlertStatus.NORMAL, statuses.get(healthyGroup.id()),
                    "successful healthy group should update its last status");
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void groupAlertTextsDoNotExposeTechnicalStatusNames() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Method formatResult = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "formatGroupCheckResult",
                        String.class,
                        List.class,
                        GroupEvaluation.class);
                formatResult.setAccessible(true);
                Method alertText = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "groupAlertText",
                        GroupEvaluation.class);
                alertText.setAccessible(true);

                String pendingRuntimeText = (String) formatResult.invoke(
                        app,
                        "手动检测",
                        List.of(),
                        groupEvaluation(GroupAlertStatus.PENDING_ALERT, "观察中：使用位无料，备用位有料 2/4。"));
                String activeDialogText = (String) alertText.invoke(
                        app,
                        groupEvaluation(GroupAlertStatus.ACTIVE_ALERT, "需关注：请现场确认使用位和备用位。"));
                String ackedDialogText = (String) alertText.invoke(
                        app,
                        groupEvaluation(GroupAlertStatus.ACKED_ALERT, "已关注：等待现场处理完成。"));

                TestSupport.assertContains(pendingRuntimeText, "观察中", "runtime text should use Chinese pending status");
                TestSupport.assertContains(activeDialogText, "状态：需关注", "dialog text should use Chinese active status");
                TestSupport.assertContains(ackedDialogText, "状态：已关注", "dialog text should use Chinese acknowledged status");
                assertNoTechnicalGroupText(pendingRuntimeText, "runtime text");
                assertNoTechnicalGroupText(activeDialogText, "active dialog text");
                assertNoTechnicalGroupText(ackedDialogText, "acknowledged dialog text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void scanGridBagContainers(Container container) {
        if (container.getLayout() instanceof GridBagLayout) {
            GridBagLayout layout = (GridBagLayout) container.getLayout();
            Set<String> occupied = new HashSet<>();
            for (Component component : container.getComponents()) {
                GridBagConstraints constraints = layout.getConstraints(component);
                int width = Math.max(1, constraints.gridwidth);
                int height = Math.max(1, constraints.gridheight);
                for (int x = constraints.gridx; x < constraints.gridx + width; x++) {
                    for (int y = constraints.gridy; y < constraints.gridy + height; y++) {
                        String cell = x + "," + y;
                        if (!occupied.add(cell)) {
                            throw new AssertionError("GridBag cell overlap at " + cell + " in " + container.getClass());
                        }
                    }
                }
            }
        }
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                scanGridBagContainers((Container) component);
            }
        }
    }

    private static <T> T findFirst(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container) {
                T found = findFirst((Container) component, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasGroupPointTable(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable) {
                JTable table = (JTable) component;
                if (table.getColumnCount() == 4
                        && "角色".equals(table.getColumnName(0))
                        && "点位编码".equals(table.getColumnName(2))) {
                    return true;
                }
            }
            if (component instanceof Container && hasGroupPointTable((Container) component)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> collectVisibleTexts(Container container) {
        Set<String> texts = new HashSet<>();
        collectVisibleTexts(container, texts);
        return texts;
    }

    private static GroupEvaluation groupEvaluation(GroupAlertStatus status, String message) {
        return new GroupEvaluation(
                "group-001",
                "A区",
                "一号料架",
                "标准件",
                status,
                true,
                4,
                2,
                2,
                true,
                5,
                false,
                message);
    }

    private static PointGroupDefinition group(String id, int checkIntervalSeconds) {
        return new PointGroupDefinition(
                id,
                "Area A",
                "Rear Panel",
                "Material A",
                true,
                checkIntervalSeconds,
                List.of(
                        new GroupMonitorPoint(id + "-use", "USE_POINT_001", "Use", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup", "BACKUP_POINT_001", "Backup", PointRole.BACKUP, true, 2)),
                new GroupAlertRule(true, true, 1, 5));
    }

    private static List<PointRecord> healthyRecords() {
        return List.of(
                record("USE_POINT_001", "SHELF_USE_001", 1, 0),
                record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0));
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
                LocalDateTime.of(2026, 7, 3, 10, 0),
                LocalDateTime.of(2026, 7, 3, 9, 59));
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

    private static void shutdownExecutor(Object target) throws Exception {
        ScheduledExecutorService executor = fieldValue(target, "executor", ScheduledExecutorService.class);
        executor.shutdownNow();
    }

    private static void assertNoTechnicalGroupText(String text, String context) {
        TestSupport.assertNotContains(text, "status=", context + " should not leak debug status field");
        TestSupport.assertNotContains(text, "PENDING_ALERT", context + " should not leak pending enum");
        TestSupport.assertNotContains(text, "ACTIVE_ALERT", context + " should not leak active enum");
        TestSupport.assertNotContains(text, "ACKED_ALERT", context + " should not leak acknowledged enum");
        TestSupport.assertNotContains(text, "useEmpty=", context + " should not leak debug use point field");
        TestSupport.assertNotContains(text, "backup=", context + " should not leak debug backup field");
        TestSupport.assertNotContains(text, "continuous=", context + " should not leak debug duration field");
    }

    private static void collectVisibleTexts(Component component, Set<String> texts) {
        if (component instanceof JLabel) {
            texts.add(((JLabel) component).getText());
        } else if (component instanceof AbstractButton) {
            texts.add(((AbstractButton) component).getText());
        } else if (component instanceof JTextComponent) {
            texts.add(((JTextComponent) component).getText());
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectVisibleTexts(child, texts);
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

        int capturedCount() {
            return commands.size();
        }

        void runCapturedOnWorker() throws Exception {
            TestSupport.assertEquals(1, commands.size(), "expected one captured command");
            Throwable[] failure = new Throwable[1];
            Thread worker = new Thread(() -> {
                try {
                    commands.get(0).run();
                } catch (Throwable ex) {
                    failure[0] = ex;
                }
            }, "manual-check-worker");
            worker.start();
            worker.join(5000);
            TestSupport.assertFalse(worker.isAlive(), "captured command should finish");
            if (failure[0] instanceof Exception) {
                throw (Exception) failure[0];
            }
            if (failure[0] instanceof Error) {
                throw (Error) failure[0];
            }
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> copy = new ArrayList<>(commands);
            commands.clear();
            return copy;
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
        public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command,
                long initialDelay,
                long period,
                TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command,
                long initialDelay,
                long delay,
                TimeUnit unit) {
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

        static void assertFalse(boolean condition, String message) {
            assertTrue(!condition, message);
        }

        static void assertContains(String text, String expected, String message) {
            if (text == null || !text.contains(expected)) {
                throw new AssertionError(message + " expected to contain=" + expected + " actual=" + text);
            }
        }

        static void assertNotContains(String text, String unexpected, String message) {
            if (text != null && text.contains(unexpected)) {
                throw new AssertionError(message + " unexpected=" + unexpected + " actual=" + text);
            }
        }
    }
}
