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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public final class ShelfPointMonitorAppUiTest {
    public static void main(String[] args) throws Exception {
        appHasThreeNavigationPages();
        connectionPageUsesChineseOperatorLabels();
        alertPageContainsGroupPointTable();
        groupCheckIntervalFieldExists();
        alertPageShowsPointStatusDashboardLanguage();
        updateSelectedGroupFromFormPreservesCustomInterval();
        updateSelectedGroupFromFormSavesBackupThresholdParticipation();
        populateSelectedGroupRoundsPartialMinutesUp();
        renderPointStatusBoardShowsMaterialStates();
        renderPointStatusBoardDoesNotOverlapWhenBackupsPrecedeUse();
        capturedMonitoredGroupsIgnoreLaterFormChanges();
        manualCheckUsesGroupSnapshotCapturedOnEdt();
        groupFetchFailureMarksCheckedAndContinues();
        checkGroupsWithFetcherUpdatesSelectedDashboardAfterEdtFlush();
        gridBagPanelsDoNotOverlapCells();
        groupStatusTextUsesOperatorChinese();
        groupSummaryDoesNotExposeTechnicalFields();
        groupAlertTextsDoNotExposeTechnicalStatusNames();
        groupAlertTextUsesOperatorLanguageAndListsAbnormalPoints();
        groupAlertDialogButtonsIncludeOpenLogs();
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

    private static void alertPageShowsPointStatusDashboardLanguage() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains("检测周期(分钟)："),
                        "dashboard should show group check interval");
                TestSupport.assertTrue(texts.contains("报警持续(分钟)："),
                        "dashboard should show alert duration");
                TestSupport.assertTrue(texts.contains("备用位下限参与报警"),
                        "dashboard should allow backup threshold participation");
                TestSupport.assertTrue(texts.contains("点位状态看板"),
                        "dashboard should have status board title");
                TestSupport.assertTrue(texts.contains("当前判断：未检测"),
                        "dashboard should show current business judgement");
                TestSupport.assertTrue(fieldValue(app, "backupThresholdParticipatesBox", JCheckBox.class) != null,
                        "backup threshold participation checkbox should exist");
                TestSupport.assertTrue(fieldValue(app, "pointStatusPanel", JPanel.class) != null,
                        "point status panel should exist");
                TestSupport.assertTrue(fieldValue(app, "groupSummaryLabel", JLabel.class) != null,
                        "group summary label should exist");
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

    private static void updateSelectedGroupFromFormSavesBackupThresholdParticipation() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                PointGroupDefinition source = group("group-001", 300);
                setField(app, "pointGroups", new ArrayList<>(List.of(source)));
                invoke(app, "refreshGroupList", new Class<?>[] {String.class}, source.id());
                invoke(app, "populateSelectedGroup", new Class<?>[0]);

                JCheckBox box = fieldValue(app, "backupThresholdParticipatesBox", JCheckBox.class);
                TestSupport.assertTrue(box.isSelected(),
                        "test setup should load default backup threshold participation");
                box.setSelected(false);
                invoke(app, "updateSelectedGroupFromForm", new Class<?>[0]);

                List<?> updatedGroups = fieldValue(app, "pointGroups", List.class);
                PointGroupDefinition updated = (PointGroupDefinition) updatedGroups.get(0);
                TestSupport.assertFalse(updated.rule().backupThresholdParticipates(),
                        "backup threshold participation should be saved from checkbox");
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

    private static void renderPointStatusBoardShowsMaterialStates() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                invoke(app, "renderPointStatusBoard", new Class<?>[] {GroupEvaluation.class},
                        groupEvaluationWithPointStatuses());

                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains(PointMaterialStatus.AVAILABLE.displayText()),
                        "point status board should show available text");
                TestSupport.assertTrue(texts.contains(PointMaterialStatus.EMPTY.displayText()),
                        "point status board should show empty text");
                TestSupport.assertTrue(texts.contains(PointMaterialStatus.MISSING.displayText()),
                        "point status board should show missing text");
                TestSupport.assertTrue(texts.contains(PointMaterialStatus.DISABLED.displayText()),
                        "point status board should show disabled text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void renderPointStatusBoardDoesNotOverlapWhenBackupsPrecedeUse() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                invoke(app, "renderPointStatusBoard", new Class<?>[] {GroupEvaluation.class},
                        groupEvaluationWithBackupBeforeUsePointStatuses());

                JPanel panel = fieldValue(app, "pointStatusPanel", JPanel.class);
                scanGridBagContainers(panel);
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

    private static void checkGroupsWithFetcherUpdatesSelectedDashboardAfterEdtFlush() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        PointGroupDefinition group = group("group-dashboard", 60);
        runOnEdtAndWait(() -> {
            appRef[0] = new ShelfPointMonitorApp();
            setField(appRef[0], "pointGroups", new ArrayList<>(List.of(group)));
            invoke(appRef[0], "refreshGroupList", new Class<?>[] {String.class}, group.id());
            invoke(appRef[0], "populateSelectedGroup", new Class<?>[0]);
        });
        try {
            ShelfPointMonitorApp app = appRef[0];
            app.checkGroupsWithFetcher(
                    List.of(group),
                    LocalDateTime.of(2026, 7, 3, 10, 20),
                    "test",
                    ignored -> recordsWithEmptyUseAndAvailableBackup());

            runOnEdtAndWait(() -> {
                JLabel summary = fieldValue(app, "groupSummaryLabel", JLabel.class);
                TestSupport.assertContains(summary.getText(), "\u5f53\u524d\u5224\u65ad",
                        "selected dashboard should show current judgement after EDT flush");
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains(PointMaterialStatus.AVAILABLE.displayText()),
                        "selected dashboard should show available text after check");
                TestSupport.assertTrue(texts.contains(PointMaterialStatus.EMPTY.displayText()),
                        "selected dashboard should show empty text after check");
            });
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
                String debugRuntimeText = (String) formatResult.invoke(
                        app,
                        "自动检测",
                        List.of(),
                        groupEvaluationWithAbnormalPointStatuses());
                String activeDialogText = (String) alertText.invoke(
                        app,
                        groupEvaluation(GroupAlertStatus.ACTIVE_ALERT, "需关注：请现场确认使用位和备用位。"));
                String ackedDialogText = (String) alertText.invoke(
                        app,
                        groupEvaluation(GroupAlertStatus.ACKED_ALERT, "已关注：等待现场处理完成。"));

                TestSupport.assertContains(pendingRuntimeText, "观察中", "runtime text should use Chinese pending status");
                TestSupport.assertContains(debugRuntimeText, "使用位无料",
                        "runtime text should use operator material state");
                TestSupport.assertContains(activeDialogText, "A区 / 一号料架 需要关注",
                        "dialog text should use operator active status");
                TestSupport.assertContains(ackedDialogText, "A区 / 一号料架 已关注",
                        "dialog text should use Chinese acknowledged status");
                assertNoTechnicalGroupText(pendingRuntimeText, "runtime text");
                assertNoTechnicalGroupText(debugRuntimeText, "debug runtime text");
                assertNoTechnicalGroupText(activeDialogText, "active dialog text");
                assertNoTechnicalGroupText(ackedDialogText, "acknowledged dialog text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void groupStatusTextUsesOperatorChinese() {
        TestSupport.assertEquals("正常", GroupStatusText.statusText(GroupAlertStatus.NORMAL),
                "normal status should be operator Chinese");
        TestSupport.assertEquals("观察中", GroupStatusText.statusText(GroupAlertStatus.PENDING_ALERT),
                "pending status should be operator Chinese");
        TestSupport.assertEquals("需关注", GroupStatusText.statusText(GroupAlertStatus.ACTIVE_ALERT),
                "active status should be operator Chinese");
        TestSupport.assertEquals("已关注", GroupStatusText.statusText(GroupAlertStatus.ACKED_ALERT),
                "acknowledged status should be operator Chinese");

        for (GroupAlertStatus status : GroupAlertStatus.values()) {
            String text = GroupStatusText.statusText(status);
            TestSupport.assertFalse(status.name().equals(text), "status text should not return enum name");
        }
    }

    private static void groupSummaryDoesNotExposeTechnicalFields() {
        String summary = GroupStatusText.summary(
                group("group-001", 300),
                GroupAlertStatus.ACTIVE_ALERT,
                true,
                3,
                1,
                121,
                300,
                groupEvaluationWithAbnormalPointStatuses().pointStatuses());

        TestSupport.assertContains(summary, "需关注", "summary should use operator status text");
        TestSupport.assertContains(summary, "使用位无料", "summary should show use point material state");
        TestSupport.assertContains(summary, "备用位有料 1/3", "summary should show backup material count");
        assertNoTechnicalGroupText(summary, "group summary");
    }

    private static void groupAlertTextUsesOperatorLanguageAndListsAbnormalPoints() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Method alertText = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "groupAlertText",
                        GroupEvaluation.class);
                alertText.setAccessible(true);

                String text = (String) alertText.invoke(app, groupEvaluationWithAbnormalPointStatuses());

                TestSupport.assertContains(text, "检测时间：", "dialog should show detection time");
                TestSupport.assertContains(text, "A区 / 一号料架 需要关注",
                        "dialog should identify the area and group in operator language");
                TestSupport.assertContains(text, "物料：标准件", "dialog should show material");
                TestSupport.assertContains(text, "使用位：无料", "dialog should show use point state");
                TestSupport.assertContains(text, "备用位：1/3 有料", "dialog should show backup available count");
                TestSupport.assertContains(text, "持续：3 分钟", "dialog should show rounded duration minutes");
                TestSupport.assertContains(text, "异常点位列表", "dialog should label abnormal point details");
                TestSupport.assertContains(text, "使用位 USE_POINT_001 无料 原因：无货架",
                        "dialog should list empty use point with reason");
                TestSupport.assertContains(text, "备用位 BACKUP_POINT_002 未查到 原因：未返回记录",
                        "dialog should list missing backup point with reason");
                TestSupport.assertNotContains(text, "BACKUP_POINT_001",
                        "dialog should not list available points as abnormal");
                TestSupport.assertContains(text, "使用位无料已达到报警时间，请现场确认补料或调度状态。",
                        "dialog should end with the operator action prompt");
                assertNoTechnicalGroupText(text, "operator alert text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void groupAlertDialogButtonsIncludeOpenLogs() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Method buttonsMethod = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "buildGroupAlertButtons",
                        GroupEvaluation.class,
                        Runnable.class);
                buttonsMethod.setAccessible(true);

                JPanel buttons = (JPanel) buttonsMethod.invoke(
                        app,
                        groupEvaluationWithAbnormalPointStatuses(),
                        (Runnable) () -> { });
                Set<String> texts = collectVisibleTexts(buttons);

                TestSupport.assertTrue(texts.contains("打开日志目录"),
                        "group alert dialog should include an open logs button");
                TestSupport.assertTrue(texts.contains("已关注"),
                        "group alert dialog should keep the acknowledge button");
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

    private static GroupEvaluation groupEvaluationWithPointStatuses() {
        return new GroupEvaluation(
                "group-001",
                "A区",
                "一号料架",
                "标准件",
                GroupAlertStatus.PENDING_ALERT,
                true,
                1,
                1,
                0,
                true,
                120,
                300,
                List.of(
                        new PointStatusView(
                                "use",
                                "USE_POINT_001",
                                "使用位",
                                PointRole.USE,
                                true,
                                PointMaterialStatus.AVAILABLE,
                                "SHELF_USE_001",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "正常"),
                        new PointStatusView(
                                "backup",
                                "BACKUP_POINT_001",
                                "备用位",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.EMPTY,
                                "",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "无货架"),
                        new PointStatusView(
                                "missing",
                                "BACKUP_POINT_002",
                                "Backup 2",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.MISSING,
                                "",
                                null,
                                "missing"),
                        new PointStatusView(
                                "disabled",
                                "BACKUP_POINT_003",
                                "Backup 3",
                                PointRole.BACKUP,
                                false,
                                PointMaterialStatus.DISABLED,
                                "",
                                null,
                                "disabled")),
                false,
                "观察中：使用位无料，备用位有料 1/1。");
    }

    private static GroupEvaluation groupEvaluationWithAbnormalPointStatuses() {
        return new GroupEvaluation(
                "group-001",
                "A区",
                "一号料架",
                "标准件",
                GroupAlertStatus.ACTIVE_ALERT,
                true,
                3,
                1,
                1,
                true,
                121,
                300,
                List.of(
                        new PointStatusView(
                                "use",
                                "USE_POINT_001",
                                "使用位",
                                PointRole.USE,
                                true,
                                PointMaterialStatus.EMPTY,
                                "",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "无货架"),
                        new PointStatusView(
                                "backup-1",
                                "BACKUP_POINT_001",
                                "备用位1",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.AVAILABLE,
                                "SHELF_BACKUP_001",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "正常"),
                        new PointStatusView(
                                "backup-2",
                                "BACKUP_POINT_002",
                                "备用位2",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.MISSING,
                                "",
                                null,
                                "未返回记录"),
                        new PointStatusView(
                                "backup-3",
                                "BACKUP_POINT_003",
                                "备用位3",
                                PointRole.BACKUP,
                                false,
                                PointMaterialStatus.DISABLED,
                                "",
                                null,
                                "停用")),
                true,
                "status=ACTIVE_ALERT useEmpty=true backup=1/3");
    }

    private static GroupEvaluation groupEvaluationWithBackupBeforeUsePointStatuses() {
        return new GroupEvaluation(
                "group-001",
                "Area A",
                "Rear Panel",
                "Material A",
                GroupAlertStatus.PENDING_ALERT,
                true,
                3,
                1,
                2,
                true,
                120,
                300,
                List.of(
                        new PointStatusView(
                                "backup-1",
                                "BACKUP_POINT_001",
                                "Backup 1",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.AVAILABLE,
                                "SHELF_BACKUP_001",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "normal"),
                        new PointStatusView(
                                "backup-2",
                                "BACKUP_POINT_002",
                                "Backup 2",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.EMPTY,
                                "",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "empty"),
                        new PointStatusView(
                                "use",
                                "USE_POINT_001",
                                "Use",
                                PointRole.USE,
                                true,
                                PointMaterialStatus.EMPTY,
                                "",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "empty"),
                        new PointStatusView(
                                "backup-3",
                                "BACKUP_POINT_003",
                                "Backup 3",
                                PointRole.BACKUP,
                                false,
                                PointMaterialStatus.DISABLED,
                                "",
                                null,
                                "disabled")),
                false,
                "pending");
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

    private static List<PointRecord> recordsWithEmptyUseAndAvailableBackup() {
        return List.of(
                record("USE_POINT_001", "", 1, 0),
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
        if (component instanceof JComponent
                && ((JComponent) component).getBorder() instanceof TitledBorder) {
            texts.add(((TitledBorder) ((JComponent) component).getBorder()).getTitle());
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
