package com.local.monitor;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public final class LogsSettingsPageTest {
    private LogsSettingsPageTest() {
    }

    public static void main(String[] args) throws Exception {
        logsPageHasSixDynamicHealthCardsLimitsAndRealActions();
        settingsPageContainsOnlyEffectiveSettingsAndFourFixedSafetyStatements();
        System.out.println("LogsSettingsPageTest PASS");
    }

    private static void logsPageHasSixDynamicHealthCardsLimitsAndRealActions() throws Exception {
        runOnEdtAndWait(() -> {
            int[] actions = new int[4];
            LogsSystemPage page = new LogsSystemPage(
                    logComponents(),
                    new LogsSystemPage.Actions(
                            () -> actions[0]++, () -> { }, () -> actions[1]++,
                            () -> actions[2]++, () -> actions[3]++));
            assertEquals(6, findAll(page, MetricCard.class).size(), "dynamic health card count");
            assertTrue(collectText(page).contains("event-log.csv：最近 1000 条"), "event log limit");
            assertTrue(collectText(page).contains("check-log.csv：最近 1000 条"), "check log limit");
            assertTrue(collectText(page).contains("monitor.log：最近 200 行"), "monitor log limit");
            List<String> buttons = List.of("刷新日志", "打开日志目录", "执行自检", "导出诊断包");
            for (int index = 0; index < buttons.size(); index++) {
                AbstractButton button = findButton(page, buttons.get(index));
                assertTrue(button != null, "missing logs action " + buttons.get(index));
                button.doClick();
                assertEquals(1, actions[index], "logs action callback " + buttons.get(index));
            }
            assertEquals(32, findAll(page, JTable.class).get(0).getRowHeight(), "system log table row height");
            for (JTextArea area : findAll(page, JTextArea.class)) {
                assertFalse(area.isEditable(), "log text areas must be read-only");
            }
        });
    }

    private static void settingsPageContainsOnlyEffectiveSettingsAndFourFixedSafetyStatements() throws Exception {
        runOnEdtAndWait(() -> {
            int[] actions = new int[3];
            SystemSettingsPage page = new SystemSettingsPage(
                    settingComponents(),
                    new SystemSettingsPage.Actions(
                            () -> actions[0]++, () -> actions[1]++, () -> actions[2]++));
            String text = collectText(page);
            for (String setting : List.of(
                    "默认首页：", "监控总览自动刷新间隔(秒)：", "报警弹窗启用", "报警声音提示",
                    "日志保留天数：", "界面显示密度：", "启动时执行自检", "日志自动清理")) {
                assertTrue(text.contains(setting), "missing effective setting " + setting);
            }
            List<String> safety = List.of(
                    "敏感信息脱敏：强制启用", "数据库访问模式：只读",
                    "SQL 编辑能力：未提供", "密码持久化：禁止");
            for (String statement : safety) {
                assertTrue(text.contains(statement), "missing fixed safety statement " + statement);
            }
            assertEquals(4, findAll(page, StatusBadge.class).size(), "fixed safety statement count");
            List<String> buttons = List.of("保存设置", "恢复默认", "重新加载配置");
            for (int index = 0; index < buttons.size(); index++) {
                AbstractButton button = findButton(page, buttons.get(index));
                assertTrue(button != null, "missing settings action " + buttons.get(index));
                button.doClick();
                assertEquals(1, actions[index], "settings action callback " + buttons.get(index));
            }
        });
    }

    private static LogsSystemPage.Components logComponents() {
        return new LogsSystemPage.Components(
                new JLabel("监控调度器：未运行"), new JLabel("当前连接：未选择连接"),
                new JLabel("最近一次检测：未检测"), new JLabel("配置文件：正常"),
                new JLabel("日志目录：可写"), new JLabel("自检状态：未执行"),
                new JComboBox<>(new String[] {"全部"}), new JTextField(), new JTextField(),
                new JTextField(), new JTextField(),
                new JTable(new DefaultTableModel(new Object[] {"时间", "事件", "级别", "点位组", "详情", "来源"}, 0)),
                new JTextArea(), new JTextArea());
    }

    private static SystemSettingsPage.Components settingComponents() {
        return new SystemSettingsPage.Components(
                new JComboBox<>(new String[] {"监控总览"}), spinner(10),
                new JCheckBox("报警弹窗启用"), new JCheckBox("报警声音提示"), spinner(30),
                new JComboBox<>(new String[] {"标准"}), new JCheckBox("启动时执行自检"),
                new JCheckBox("日志自动清理"));
    }

    private static JSpinner spinner(int value) {
        return new JSpinner(new SpinnerNumberModel(value, 1, 3650, 1));
    }

    private static String collectText(Component root) {
        List<String> values = new ArrayList<>();
        for (JLabel label : findAll(root, JLabel.class)) {
            values.add(label.getText());
        }
        for (AbstractButton button : findAll(root, AbstractButton.class)) {
            values.add(button.getText());
        }
        return String.join(" ", values);
    }

    private static AbstractButton findButton(Component root, String text) {
        for (AbstractButton button : findAll(root, AbstractButton.class)) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        return null;
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> values = new ArrayList<>();
        collect(root, type, values);
        return values;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> values) {
        if (type.isInstance(component)) {
            values.add(type.cast(component));
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                collect(child, type, values);
            }
        }
    }

    private static void runOnEdtAndWait(ThrowingRunnable runnable) throws Exception {
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
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception nested) {
                throw nested;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
