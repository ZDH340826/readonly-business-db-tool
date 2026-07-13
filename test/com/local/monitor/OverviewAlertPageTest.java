package com.local.monitor;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public final class OverviewAlertPageTest {
    private static final List<String> OVERVIEW_COLUMNS = List.of(
            "状态", "区域", "点位组", "使用位状态", "备用位可用数", "持续时间", "上次检测");

    private OverviewAlertPageTest() {
    }

    public static void main(String[] args) throws Exception {
        overviewHasFourMetricsTableDetailAndFiveRealActions();
        alertCenterHasFiveSemanticFiltersAndFiveRealActions();
        queryFailureUsesIndependentTextAndColor();
        System.out.println("OverviewAlertPageTest PASS");
    }

    private static void overviewHasFourMetricsTableDetailAndFiveRealActions() throws Exception {
        runOnEdtAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(OVERVIEW_COLUMNS.toArray(), 0));
            int[] actions = new int[5];
            OverviewPage page = new OverviewPage(
                    new JLabel("8"), new JLabel("1"), new JLabel("2"), new JLabel("1"),
                    table, new JTextArea(), () -> { },
                    () -> actions[0]++, () -> actions[1]++, () -> actions[2]++,
                    () -> actions[3]++, () -> actions[4]++);

            assertEquals(4, findAll(page, MetricCard.class).size(), "overview metric card count");
            assertEquals(OVERVIEW_COLUMNS, tableColumns(table), "overview status columns");
            assertEquals(32, table.getRowHeight(), "overview table row height");
            assertEquals(1, findAll(page, JTable.class).size(), "overview status table count");
            assertEquals(1, findAll(page, JTextArea.class).size(), "overview detail area count");

            List<String> buttonTexts = List.of("开始监控", "停止监控", "立即检测", "查看报警", "标记已关注");
            for (int index = 0; index < buttonTexts.size(); index++) {
                AbstractButton button = findButton(page, buttonTexts.get(index));
                assertTrue(button != null, "missing overview action " + buttonTexts.get(index));
                button.doClick();
                assertEquals(1, actions[index], "overview action callback " + buttonTexts.get(index));
            }
        });
    }

    private static void alertCenterHasFiveSemanticFiltersAndFiveRealActions() throws Exception {
        runOnEdtAndWait(() -> {
            JComboBox<String> filter = new JComboBox<>(AlertCenterPage.FILTERS.toArray(String[]::new));
            JTable table = new JTable(new DefaultTableModel(
                    new Object[] {"时间", "区域", "点位组", "原因", "持续时间", "处理状态"}, 0));
            int[] actions = new int[5];
            AlertCenterPage page = new AlertCenterPage(
                    filter, table, new JTextArea(), () -> { }, () -> { },
                    () -> actions[0]++, () -> actions[1]++, () -> actions[2]++,
                    () -> actions[3]++, () -> actions[4]++);

            assertEquals(List.of("活跃报警", "已关注", "观察中", "查询失败", "已恢复"),
                    AlertCenterPage.FILTERS, "alert semantic filters");
            assertFalse(AlertCenterPage.FILTERS.contains("QUERY_RECOVERED"),
                    "query recovery must not become a business recovered filter");
            assertEquals(32, table.getRowHeight(), "alert table row height");
            for (String filterText : AlertCenterPage.FILTERS) {
                assertTrue(findButton(page, filterText) != null, "missing alert filter " + filterText);
            }

            List<String> buttonTexts = List.of("刷新报警", "标记已关注", "查看点位详情", "立即检测该组", "查看连接状态");
            for (int index = 0; index < buttonTexts.size(); index++) {
                AbstractButton button = findButton(page, buttonTexts.get(index));
                assertTrue(button != null, "missing alert action " + buttonTexts.get(index));
                button.doClick();
                assertEquals(1, actions[index], "alert action callback " + buttonTexts.get(index));
            }
        });
    }

    private static void queryFailureUsesIndependentTextAndColor() throws Exception {
        runOnEdtAndWait(() -> {
            assertEquals("查询失败，数据不可用", OverviewPage.QUERY_FAILURE_TEXT,
                    "overview query failure text");
            JTable table = new JTable(new DefaultTableModel(
                    new Object[][] {{OverviewPage.QUERY_FAILURE_TEXT}}, new Object[] {"状态"}));
            UiFactory.configureTable(table);
            UiFactory.configureStatusColumn(table, 0);
            Component rendered = table.getCellRenderer(0, 0).getTableCellRendererComponent(
                    table, table.getValueAt(0, 0), false, false, 0, 0);
            assertTrue(rendered instanceof JLabel, "status renderer must preserve visible text");
            JLabel label = (JLabel) rendered;
            assertEquals(OverviewPage.QUERY_FAILURE_TEXT, label.getText(), "status renderer text");
            assertEquals(AppTheme.QUERY_FAILED, label.getForeground(), "query failure semantic color");
            assertNotEquals(AppTheme.DANGER, label.getForeground(),
                    "query failure must not look identical to material shortage");
        });
    }

    private static List<String> tableColumns(JTable table) {
        List<String> columns = new ArrayList<>();
        for (int index = 0; index < table.getColumnCount(); index++) {
            columns.add(table.getColumnName(index));
        }
        return columns;
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

    private static void assertNotEquals(Object first, Object second, String message) {
        if (java.util.Objects.equals(first, second)) {
            throw new AssertionError(message + " both=" + first);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
