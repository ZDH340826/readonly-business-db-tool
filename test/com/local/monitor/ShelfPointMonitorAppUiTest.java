package com.local.monitor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.io.IOException;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.awt.image.BufferedImage;
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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public final class ShelfPointMonitorAppUiTest {
    public static void main(String[] args) throws Exception {
        appHasFinalNavigationPages();
        finalNavigationPagesCanSwitchAndAreNotBlank();
        finalShellHasRequiredStatusBarsAndSize();
        finalLayoutsExposeAcceptanceStructure();
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
        groupFetchFailureUpdatesSelectedDashboardWithChineseStatus();
        queryFailureClosesActiveDialogForSameGroupOnly();
        queryFailureDoesNotCloseDialogForOtherGroup();
        stoppingMonitoringClosesOldDialogWithoutAcknowledging();
        queryFailureEventsAreDeduplicatedAndSanitized();
        recoveredFilterDoesNotShowNeverAlertedNormalGroup();
        alertCenterActionsUseGroupIdWhenNamesCollide();
        checkGroupsWithFetcherUpdatesSelectedDashboardAfterEdtFlush();
        gridBagPanelsDoNotOverlapCells();
        buttonsPaintReadableColorsAcrossEveryPage();
        standardTablesUseReadableScrollPanesAcrossEveryPage();
        groupStatusTextUsesOperatorChinese();
        groupSummaryDoesNotExposeTechnicalFields();
        groupAlertTextsDoNotExposeTechnicalStatusNames();
        groupAlertTextUsesOperatorLanguageAndListsAbnormalPoints();
        groupAlertTextUsesNeutralPromptWhenUsePointHasMaterial();
        groupAlertDialogButtonsIncludeOpenLogs();
        System.out.println("ShelfPointMonitorAppUiTest PASS");
    }

    private static void appHasFinalNavigationPages() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                JList<?> navigation = findFirst(app.getContentPane(), JList.class);
                TestSupport.assertTrue(navigation != null, "app should have a navigation list");
                List<String> expected = List.of(
                        "监控总览",
                        "点位组管理",
                        "报警中心",
                        "连接管理",
                        "数据查询",
                        "数据源浏览器",
                        "日志与系统",
                        "系统设置");
                TestSupport.assertEquals(expected.size(), navigation.getModel().getSize(),
                        "app should have eight final navigation pages");
                for (int i = 0; i < expected.size(); i++) {
                    TestSupport.assertEquals(expected.get(i), navigation.getModel().getElementAt(i),
                            "navigation item order should match final UI spec at index " + i);
                }
            } finally {
                app.dispose();
            }
        });
    }

    private static void finalNavigationPagesCanSwitchAndAreNotBlank() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                JList<?> navigation = findFirst(app.getContentPane(), JList.class);
                String[] requiredTexts = {
                        "监控点位组",
                        "验证配置",
                        "活跃报警",
                        "数据库访问模式：只读",
                        "点位编码关键字：",
                        "预览前100行",
                        "执行自检",
                        "保存设置"
                };
                for (int i = 0; i < navigation.getModel().getSize(); i++) {
                    navigation.setSelectedIndex(i);
                    Set<String> texts = collectVisibleTexts(app.getContentPane());
                    TestSupport.assertTrue(texts.contains(requiredTexts[i]),
                            "page should expose real visible content for " + navigation.getModel().getElementAt(i));
                }
            } finally {
                app.dispose();
            }
        });
    }

    private static void finalShellHasRequiredStatusBarsAndSize() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                TestSupport.assertTrue(app.getMinimumSize().width >= 1180, "minimum width should be at least 1180");
                TestSupport.assertTrue(app.getMinimumSize().height >= 760, "minimum height should be at least 760");
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains("监控状态：未运行"), "top status bar should show monitor status");
                TestSupport.assertTrue(texts.contains("上次检测：--"), "top status bar should show last check");
                TestSupport.assertTrue(texts.contains("下次检测：--"), "top status bar should show next check");
                TestSupport.assertTrue(texts.contains("就绪"), "bottom status bar should show runtime status");
            } finally {
                app.dispose();
            }
        });
    }

    private static void finalLayoutsExposeAcceptanceStructure() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains("基本信息"), "group management should expose basic info panel");
                TestSupport.assertTrue(texts.contains("报警规则"), "group management should expose rule panel");
                TestSupport.assertTrue(texts.contains("Schema / 表 / 视图对象树"),
                        "browser should expose object tree panel");
                TestSupport.assertTrue(texts.contains("对象元数据与列信息"),
                        "browser should expose metadata panel");
                TestSupport.assertTrue(texts.contains("前 100 行只读预览"),
                        "browser should expose preview panel");
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

    private static void buttonsPaintReadableColorsAcrossEveryPage() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        AppTheme.install();
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                JList<?> navigation = findFirst(app.getContentPane(), JList.class);
                TestSupport.assertTrue(navigation != null, "button audit requires the final navigation list");
                for (int page = 0; page < navigation.getModel().getSize(); page++) {
                    navigation.setSelectedIndex(page);
                    List<JButton> buttons = new ArrayList<>();
                    collectVisibleButtons(app.getContentPane(), buttons);
                    String pageName = String.valueOf(navigation.getModel().getElementAt(page));
                    TestSupport.assertTrue(!buttons.isEmpty(), pageName + " should expose at least one action button");
                    for (JButton button : buttons) {
                        assertButtonPaintsReadableColors(button, pageName);
                    }
                }
            } finally {
                app.dispose();
            }
        });
    }

    private static void assertButtonPaintsReadableColors(JButton button, String pageName) {
        String text = button.getText();
        TestSupport.assertTrue(text != null && !text.isBlank(), pageName + " contains an unlabeled button");
        int width = Math.max(80, button.getPreferredSize().width);
        int height = Math.max(36, button.getPreferredSize().height);
        button.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        Color paintedBackground = new Color(image.getRGB(Math.max(2, width - 8), height / 2), true);
        Color configuredBackground = button.getBackground();
        int paintDifference = Math.abs(paintedBackground.getRed() - configuredBackground.getRed())
                + Math.abs(paintedBackground.getGreen() - configuredBackground.getGreen())
                + Math.abs(paintedBackground.getBlue() - configuredBackground.getBlue());
        TestSupport.assertTrue(paintDifference <= 30,
                pageName + " / " + text + " should paint its configured background; configured="
                        + configuredBackground + " painted=" + paintedBackground);

        Color textColor = button.isEnabled() ? button.getForeground() : UIManager.getColor("Button.disabledText");
        TestSupport.assertTrue(textColor != null, pageName + " / " + text + " must define a text color");
        double contrast = contrastRatio(textColor, configuredBackground);
        TestSupport.assertTrue(contrast >= 4.5d,
                pageName + " / " + text + " text must remain readable; contrast=" + contrast);
    }

    private static void standardTablesUseReadableScrollPanesAcrossEveryPage() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                JList<?> navigation = findFirst(app.getContentPane(), JList.class);
                TestSupport.assertTrue(navigation != null, "table audit requires the final navigation list");
                for (int page = 0; page < navigation.getModel().getSize(); page++) {
                    navigation.setSelectedIndex(page);
                    String pageName = String.valueOf(navigation.getModel().getElementAt(page));
                    List<JTable> tables = new ArrayList<>();
                    collectVisibleTables(app.getContentPane(), tables);
                    for (JTable table : tables) {
                        Component ancestor = SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
                        TestSupport.assertTrue(ancestor instanceof ReadableTableScrollPane,
                                pageName + " / standard data table must support horizontal browsing");
                    }
                }
            } finally {
                app.dispose();
            }
        });
    }

    private static double contrastRatio(Color first, Color second) {
        double light = Math.max(relativeLuminance(first), relativeLuminance(second));
        double dark = Math.min(relativeLuminance(first), relativeLuminance(second));
        return (light + 0.05d) / (dark + 0.05d);
    }

    private static double relativeLuminance(Color color) {
        double red = linearColor(color.getRed() / 255.0d);
        double green = linearColor(color.getGreen() / 255.0d);
        double blue = linearColor(color.getBlue() / 255.0d);
        return 0.2126d * red + 0.7152d * green + 0.0722d * blue;
    }

    private static double linearColor(double value) {
        return value <= 0.04045d ? value / 12.92d : Math.pow((value + 0.055d) / 1.055d, 2.4d);
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
                TestSupport.assertTrue(texts.contains("测试连接"), "connection page should show Chinese test button");
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
            setField(app, "monitorExecutor", executor);
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
            TestSupport.assertEquals(2, result.evaluations().size(),
                    "failed fetch should create a visible query failure evaluation");
            TestSupport.assertEquals(failingGroup.id(), result.evaluations().get(0).groupId(),
                    "failed group should be evaluated first");
            TestSupport.assertEquals(GroupAlertStatus.QUERY_FAILED, result.evaluations().get(0).status(),
                    "failed group should use independent query failure status");
            TestSupport.assertEquals(healthyGroup.id(), result.evaluations().get(1).groupId(),
                    "successful group should still be evaluated");
            TestSupport.assertFalse(result.dialogRequested(), "failed fetch should not request a dialog");

            @SuppressWarnings("unchecked")
            Map<String, GroupAlertStatus> statuses =
                    (Map<String, GroupAlertStatus>) fieldValue(app, "lastGroupStatuses", Map.class);
            TestSupport.assertEquals(GroupAlertStatus.QUERY_FAILED, statuses.get(failingGroup.id()),
                    "failed group should update last status to query failed");
            TestSupport.assertEquals(GroupAlertStatus.NORMAL, statuses.get(healthyGroup.id()),
                    "successful healthy group should update its last status");
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void groupFetchFailureUpdatesSelectedDashboardWithChineseStatus() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        PointGroupDefinition group = group("group-query-failed", 60);
        runOnEdtAndWait(() -> {
            appRef[0] = new ShelfPointMonitorApp();
            setField(appRef[0], "pointGroups", new ArrayList<>(List.of(group)));
            invoke(appRef[0], "refreshGroupList", new Class<?>[] {String.class}, group.id());
            invoke(appRef[0], "populateSelectedGroup", new Class<?>[0]);
        });
        try {
            ShelfPointMonitorApp app = appRef[0];
            ShelfPointMonitorApp.GroupCheckRunResult result = app.checkGroupsWithFetcher(
                    List.of(group),
                    LocalDateTime.of(2026, 7, 3, 10, 20),
                    "test",
                    ignored -> {
                        throw new IllegalStateException("connection timeout");
                    });
            runOnEdtAndWait(() -> {
                JLabel summary = fieldValue(app, "groupSummaryLabel", JLabel.class);
                String text = summary.getText();
                TestSupport.assertContains(text, "查询失败", "dashboard should show query failure in Chinese");
                TestSupport.assertContains(text, "connection timeout", "dashboard should show sanitized error summary");
                assertNoTechnicalGroupText(text, "query failure dashboard");
            });
            TestSupport.assertEquals(GroupAlertStatus.QUERY_FAILED, result.evaluations().get(0).status(),
                    "result should expose query failure status");
            TestSupport.assertFalse(result.dialogRequested(), "query failure should not show shortage dialog");
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void queryFailureClosesActiveDialogForSameGroupOnly() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        Path logDir = Files.createTempDirectory("query-failure-dialog-test");
        PointGroupDefinition group = group("group-dialog-a", 60);
        runOnEdtAndWait(() -> {
            appRef[0] = new ShelfPointMonitorApp();
            setField(appRef[0], "groupLogWriter", new GroupLogWriter(logDir));
            setField(appRef[0], "logPath", logDir.resolve("monitor.log"));
            setField(appRef[0], "activeDialog", new JDialog(appRef[0], "active A"));
            setField(appRef[0], "activeDialogGroupId", group.id());
            @SuppressWarnings("unchecked")
            Map<String, GroupAlertStatus> statuses =
                    (Map<String, GroupAlertStatus>) fieldValue(appRef[0], "lastGroupStatuses", Map.class);
            statuses.put(group.id(), GroupAlertStatus.ACTIVE_ALERT);
        });
        try {
            ShelfPointMonitorApp app = appRef[0];
            ShelfPointMonitorApp.GroupCheckRunResult result = app.checkGroupsWithFetcher(
                    List.of(group),
                    LocalDateTime.of(2026, 7, 3, 10, 30),
                    "test",
                    ignored -> {
                        throw new IllegalStateException("connection timeout");
                    });
            runOnEdtAndWait(() -> {
                TestSupport.assertEquals(null, fieldValue(app, "activeDialog", JDialog.class),
                        "query failure should clear active dialog for the same group");
                TestSupport.assertEquals("", fieldValue(app, "activeDialogGroupId", String.class),
                        "query failure should clear active dialog group id");
            });

            TestSupport.assertEquals(GroupAlertStatus.QUERY_FAILED, result.evaluations().get(0).status(),
                    "same group failure should be a query failure evaluation");
            List<String> eventRows = waitForLogRows(logDir.resolve("event-log.csv"), 2);
            String eventLog = String.join("\n", eventRows);
            TestSupport.assertContains(eventLog, "QUERY_FAILED", "query failure event should be written");
            TestSupport.assertNotContains(eventLog, "ACKNOWLEDGED",
                    "closing stale dialog must not acknowledge the alert");
            TestSupport.assertNotContains(eventLog, "RECOVERED",
                    "query failure must not be logged as recovered");
            TestSupport.assertNotContains(eventLog, "ALERT_OPEN",
                    "query failure must not open a new shortage alert");
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void queryFailureDoesNotCloseDialogForOtherGroup() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        PointGroupDefinition failingGroup = group("group-dialog-a", 60);
        PointGroupDefinition dialogGroup = group("group-dialog-b", 60);
        JDialog[] dialogRef = new JDialog[1];
        runOnEdtAndWait(() -> {
            appRef[0] = new ShelfPointMonitorApp();
            dialogRef[0] = new JDialog(appRef[0], "active B");
            setField(appRef[0], "activeDialog", dialogRef[0]);
            setField(appRef[0], "activeDialogGroupId", dialogGroup.id());
        });
        try {
            ShelfPointMonitorApp app = appRef[0];
            app.checkGroupsWithFetcher(
                    List.of(failingGroup),
                    LocalDateTime.of(2026, 7, 3, 10, 31),
                    "test",
                    ignored -> {
                        throw new IllegalStateException("connection timeout");
                    });
            runOnEdtAndWait(() -> {
                TestSupport.assertEquals(dialogRef[0], fieldValue(app, "activeDialog", JDialog.class),
                        "query failure must not close another group's dialog");
                TestSupport.assertEquals(dialogGroup.id(), fieldValue(app, "activeDialogGroupId", String.class),
                        "other group's active dialog ownership should stay unchanged");
            });
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void stoppingMonitoringClosesOldDialogWithoutAcknowledging() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        Path logDir = Files.createTempDirectory("stop-monitoring-dialog-test");
        PointGroupDefinition group = group("group-stop-dialog", 60);
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            appRef[0] = app;
            setField(app, "groupLogWriter", new GroupLogWriter(logDir));
            setField(app, "logPath", logDir.resolve("monitor.log"));
            setField(app, "activeDialog", new JDialog(app, "old alert"));
            setField(app, "activeDialogGroupId", group.id());
            @SuppressWarnings("unchecked")
            Map<String, GroupAlertStatus> statuses =
                    (Map<String, GroupAlertStatus>) fieldValue(app, "lastGroupStatuses", Map.class);
            statuses.put(group.id(), GroupAlertStatus.ACTIVE_ALERT);
            invoke(app, "stopMonitoring", new Class<?>[0]);
            TestSupport.assertEquals(null, fieldValue(app, "activeDialog", JDialog.class),
                    "stopping monitoring must close the old alert dialog");
            TestSupport.assertEquals("", fieldValue(app, "activeDialogGroupId", String.class),
                    "stopping monitoring must clear the dialog ownership");
            TestSupport.assertEquals(0, statuses.size(),
                    "stopping monitoring must not replace the alert with an acknowledged state");
        });
        try {
            TestSupport.assertFalse(Files.exists(logDir.resolve("event-log.csv")),
                    "closing a dialog during stop must not append an acknowledgement event");
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void queryFailureEventsAreDeduplicatedAndSanitized() throws Exception {
        ShelfPointMonitorApp[] appRef = new ShelfPointMonitorApp[1];
        Path logDir = Files.createTempDirectory("query-failure-log-test");
        PointGroupDefinition group = group("group-query-log", 60);
        runOnEdtAndWait(() -> {
            appRef[0] = new ShelfPointMonitorApp();
            setField(appRef[0], "groupLogWriter", new GroupLogWriter(logDir));
            setField(appRef[0], "logPath", logDir.resolve("monitor.log"));
        });
        try {
            ShelfPointMonitorApp app = appRef[0];
            List<String> unsafeMessages = List.of(
                    "FATAL: password authentication failed for user \"readonly_user\"",
                    "Access denied for user 'readonly_user'@'192.0.2.88'",
                    "role \"readonly_user\" does not exist",
                    "uid=readonly_user password=Secret-123",
                    "jdbc:postgresql://192.0.2.88:2345/cms_web?user=readonly_user&password=Secret-123",
                    "at com.local.monitor.PointRepository.fetch(PointRepository.java:24)");
            for (int i = 0; i < unsafeMessages.size(); i++) {
                String unsafeMessage = unsafeMessages.get(i);
                app.checkGroupsWithFetcher(
                        List.of(group),
                        LocalDateTime.of(2026, 7, 3, 10, 20).plusMinutes(i),
                        "test",
                        ignored -> {
                            throw new IllegalStateException(unsafeMessage);
                        });
            }
            app.checkGroupsWithFetcher(
                    List.of(group),
                    LocalDateTime.of(2026, 7, 3, 10, 40),
                    "test",
                    ignored -> healthyRecords());

            List<String> checkRows = waitForLogRows(logDir.resolve("check-log.csv"), unsafeMessages.size() + 2);
            List<String> eventRows = waitForLogRows(logDir.resolve("event-log.csv"), 3);
            List<String> monitorRows = waitForLogRows(logDir.resolve("monitor.log"), unsafeMessages.size());
            TestSupport.assertEquals(unsafeMessages.size() + 2, checkRows.size(),
                    "each actual check should write one check row plus header");
            TestSupport.assertEquals(3, eventRows.size(),
                    "continuous failures should only write first failure and recovery events");
            TestSupport.assertContains(eventRows.get(1), "QUERY_FAILED", "first event should mark query failure");
            TestSupport.assertContains(eventRows.get(2), "QUERY_RECOVERED", "second event should mark query recovery");

            assertSanitizedQueryFailureLog(String.join("\n", checkRows), "check-log.csv");
            assertSanitizedQueryFailureLog(String.join("\n", eventRows), "event-log.csv");
            assertSanitizedQueryFailureLog(String.join("\n", monitorRows), "monitor.log");
        } finally {
            shutdownExecutor(appRef[0]);
            runOnEdtAndWait(() -> appRef[0].dispose());
        }
    }

    private static void recoveredFilterDoesNotShowNeverAlertedNormalGroup() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                PointGroupDefinition group = group("normal-never-alerted", 60);
                setField(app, "pointGroups", new ArrayList<>(List.of(group)));
                @SuppressWarnings("unchecked")
                Map<String, GroupEvaluation> evaluations =
                        (Map<String, GroupEvaluation>) fieldValue(app, "lastGroupEvaluations", Map.class);
                evaluations.put(group.id(), evaluation(group.id(), GroupAlertStatus.NORMAL, "正常"));

                JComboBox<?> filter = fieldValue(app, "alertCenterFilterBox", JComboBox.class);
                filter.setSelectedItem("已恢复");
                invoke(app, "refreshAlertCenterPage", new Class<?>[0]);

                JTable table = fieldValue(app, "alertCenterTable", JTable.class);
                TestSupport.assertEquals(0, table.getRowCount(),
                        "normal groups that never emitted RECOVERED must not appear in recovered filter");
            } finally {
                app.dispose();
            }
        });
    }

    private static void alertCenterActionsUseGroupIdWhenNamesCollide() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                PointGroupDefinition groupA = groupWithNames("group-A", "同区域", "同组名");
                PointGroupDefinition groupB = groupWithNames("group-B", "同区域", "同组名");
                setField(app, "pointGroups", new ArrayList<>(List.of(groupA, groupB)));
                @SuppressWarnings("unchecked")
                Map<String, GroupEvaluation> evaluations =
                        (Map<String, GroupEvaluation>) fieldValue(app, "lastGroupEvaluations", Map.class);
                evaluations.put(groupA.id(), evaluation(groupA.id(), GroupAlertStatus.ACTIVE_ALERT, "A报警"));
                evaluations.put(groupB.id(), evaluation(groupB.id(), GroupAlertStatus.ACTIVE_ALERT, "B报警"));

                JComboBox<?> filter = fieldValue(app, "alertCenterFilterBox", JComboBox.class);
                filter.setSelectedItem("活跃报警");
                invoke(app, "refreshAlertCenterPage", new Class<?>[0]);

                JTable table = fieldValue(app, "alertCenterTable", JTable.class);
                TestSupport.assertEquals(2, table.getRowCount(), "test setup should expose two active rows");
                table.setRowSelectionInterval(1, 1);
                invoke(app, "acknowledgeSelectedAlertCenterGroup", new Class<?>[0]);

                @SuppressWarnings("unchecked")
                Map<String, GroupAlertStatus> statuses =
                        (Map<String, GroupAlertStatus>) fieldValue(app, "lastGroupStatuses", Map.class);
                TestSupport.assertEquals(GroupAlertStatus.ACKED_ALERT, statuses.get("group-B"),
                        "acknowledge should target selected row groupId");
                TestSupport.assertTrue(!GroupAlertStatus.ACKED_ALERT.equals(statuses.get("group-A")),
                        "acknowledge must not target the first group with the same area/name");
            } finally {
                app.dispose();
            }
        });
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
        TestSupport.assertEquals("查询失败", GroupStatusText.statusText(GroupAlertStatus.QUERY_FAILED),
                "query failure status should be operator Chinese");

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
                TestSupport.assertContains(text, "报警条件已达到报警时间，请现场确认补料或调度状态。",
                        "dialog should end with the operator action prompt");
                TestSupport.assertNotContains(text, "使用位无料已达到报警时间",
                        "dialog should not hard-code the use point as empty");
                assertNoTechnicalGroupText(text, "operator alert text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void groupAlertTextUsesNeutralPromptWhenUsePointHasMaterial() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Method alertText = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "groupAlertText",
                        GroupEvaluation.class);
                alertText.setAccessible(true);

                String text = (String) alertText.invoke(app, groupEvaluationWithBackupShortageAlert());

                TestSupport.assertContains(text, "使用位：有料",
                        "dialog should show the use point state from evaluation");
                TestSupport.assertNotContains(text, "使用位无料已达到报警时间",
                        "dialog should not claim use point empty when it has material");
                TestSupport.assertContains(text, "报警条件已达到报警时间，请现场确认补料或调度状态。",
                        "dialog should use a neutral action prompt for backup-only alerts");
                assertNoTechnicalGroupText(text, "backup-only active alert text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void groupAlertDialogButtonsIncludeOpenLogs() throws Exception {
        Path logDir = Files.createTempDirectory("group-alert-button-test").resolve("logs");
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Method buttonsMethod = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "buildGroupAlertButtons",
                        GroupEvaluation.class,
                        Runnable.class,
                        Runnable.class);
                buttonsMethod.setAccessible(true);

                GroupEvaluation evaluation = groupEvaluationWithAbnormalPointStatuses();
                boolean[] closeCalled = {false};
                Runnable openLogsAction = () -> {
                    try {
                        Files.createDirectories(logDir);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                };
                JPanel buttons = (JPanel) buttonsMethod.invoke(
                        app,
                        evaluation,
                        openLogsAction,
                        (Runnable) () -> closeCalled[0] = true);
                Set<String> texts = collectVisibleTexts(buttons);

                TestSupport.assertTrue(texts.contains("打开日志目录"),
                        "group alert dialog should include an open logs button");
                TestSupport.assertTrue(texts.contains("已关注"),
                        "group alert dialog should keep the acknowledge button");

                @SuppressWarnings("unchecked")
                Map<String, GroupAlertStatus> statuses =
                        (Map<String, GroupAlertStatus>) fieldValue(app, "lastGroupStatuses", Map.class);
                statuses.put(evaluation.groupId(), GroupAlertStatus.ACTIVE_ALERT);

                AbstractButton openLogs = findButtonByText(buttons, "打开日志目录");
                AbstractButton acknowledge = findButtonByText(buttons, "已关注");

                openLogs.doClick();

                TestSupport.assertTrue(Files.isDirectory(logDir),
                        "open logs action should create the requested log directory");
                TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, statuses.get(evaluation.groupId()),
                        "open logs should not change the last group alert status");
                TestSupport.assertFalse(closeCalled[0],
                        "open logs should not close the dialog");

                acknowledge.doClick();

                TestSupport.assertEquals(GroupAlertStatus.ACKED_ALERT, statuses.get(evaluation.groupId()),
                        "acknowledge should update the last group alert status");
                TestSupport.assertTrue(closeCalled[0],
                        "acknowledge should close the dialog");
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

    private static AbstractButton findButtonByText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton && text.equals(((AbstractButton) component).getText())) {
                return (AbstractButton) component;
            }
            if (component instanceof Container && !(component instanceof AbstractButton)) {
                AbstractButton found = findButtonByText((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        throw new AssertionError("Button not found: " + text);
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

    private static GroupEvaluation groupEvaluationWithBackupShortageAlert() {
        return new GroupEvaluation(
                "group-002",
                "A区",
                "二号料架",
                "标准件",
                GroupAlertStatus.ACTIVE_ALERT,
                false,
                3,
                0,
                3,
                true,
                180,
                300,
                List.of(
                        new PointStatusView(
                                "use",
                                "USE_POINT_002",
                                "使用位",
                                PointRole.USE,
                                true,
                                PointMaterialStatus.AVAILABLE,
                                "SHELF_USE_002",
                                LocalDateTime.of(2026, 7, 3, 10, 0),
                                "正常"),
                        new PointStatusView(
                                "backup",
                                "BACKUP_POINT_004",
                                "备用位",
                                PointRole.BACKUP,
                                true,
                                PointMaterialStatus.MISSING,
                                "",
                                null,
                                "未返回记录")),
                true,
                "备用位不足");
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

    private static PointGroupDefinition groupWithNames(String id, String areaName, String groupName) {
        return new PointGroupDefinition(
                id,
                areaName,
                groupName,
                "Material A",
                true,
                60,
                List.of(
                        new GroupMonitorPoint(id + "-use", "USE_POINT_" + id, "Use", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup", "BACKUP_POINT_" + id, "Backup", PointRole.BACKUP, true, 2)),
                new GroupAlertRule(true, true, 1, 5));
    }

    private static GroupEvaluation evaluation(String groupId, GroupAlertStatus status, String message) {
        return new GroupEvaluation(
                groupId,
                "同区域",
                "同组名",
                "Material A",
                status,
                status == GroupAlertStatus.ACTIVE_ALERT,
                1,
                0,
                1,
                status == GroupAlertStatus.ACTIVE_ALERT,
                status == GroupAlertStatus.ACTIVE_ALERT ? 5 : 0,
                status == GroupAlertStatus.ACTIVE_ALERT,
                message);
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
        ScheduledExecutorService monitorExecutor = fieldValue(target, "monitorExecutor", ScheduledExecutorService.class);
        ScheduledExecutorService ioExecutor = fieldValue(target, "ioExecutor", ScheduledExecutorService.class);
        monitorExecutor.shutdownNow();
        ioExecutor.shutdownNow();
    }

    private static void assertNoTechnicalGroupText(String text, String context) {
        TestSupport.assertNotContains(text, "status=", context + " should not leak debug status field");
        TestSupport.assertNotContains(text, "PENDING_ALERT", context + " should not leak pending enum");
        TestSupport.assertNotContains(text, "ACTIVE_ALERT", context + " should not leak active enum");
        TestSupport.assertNotContains(text, "ACKED_ALERT", context + " should not leak acknowledged enum");
        TestSupport.assertNotContains(text, "QUERY_FAILED", context + " should not leak query failure enum");
        TestSupport.assertNotContains(text, "useEmpty=", context + " should not leak debug use point field");
        TestSupport.assertNotContains(text, "backup=", context + " should not leak debug backup field");
        TestSupport.assertNotContains(text, "continuous=", context + " should not leak debug duration field");
    }

    private static void assertSanitizedQueryFailureLog(String text, String context) {
        TestSupport.assertContains(text, "查询失败", context + " should retain useful failure category");
        TestSupport.assertNotContains(text, "readonly_user", context + " must not contain database username");
        TestSupport.assertNotContains(text, "Secret-123", context + " must not contain password");
        TestSupport.assertNotContains(text, "jdbc:postgresql://192.0.2.88:2345/cms_web",
                context + " must not contain full JDBC URL");
        TestSupport.assertNotContains(text, "PointRepository.java:24",
                context + " must not contain stack trace location");
    }

    private static List<String> waitForLogRows(Path path, int minimumRows) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Files.exists(path)) {
                List<String> rows = Files.readAllLines(path);
                if (rows.size() >= minimumRows) {
                    return rows;
                }
            }
            Thread.sleep(10L);
        }
        throw new AssertionError("timed out waiting for log rows: " + path.getFileName());
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

    private static void collectVisibleButtons(Component component, List<JButton> buttons) {
        if (!component.isVisible()) {
            return;
        }
        if (component instanceof JButton) {
            JButton button = (JButton) component;
            if (button.getText() != null && !button.getText().isBlank()) {
                buttons.add(button);
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectVisibleButtons(child, buttons);
            }
        }
    }

    private static void collectVisibleTables(Component component, List<JTable> tables) {
        if (!component.isVisible()) {
            return;
        }
        if (component instanceof JTable) {
            tables.add((JTable) component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectVisibleTables(child, tables);
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
