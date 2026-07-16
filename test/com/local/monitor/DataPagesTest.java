package com.local.monitor;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public final class DataPagesTest {
    private DataPagesTest() {
    }

    public static void main(String[] args) throws Exception {
        queryPageHasStructuredConditionsPaginationExportAndNoSqlEditor();
        queryConditionEditResetsPaginationState();
        browserPageHasThreeFinalColumnsAndRealActions();
        browserPreviewHasEnoughSpaceForTheImportantColumnPreset();
        System.out.println("DataPagesTest PASS");
    }

    private static void queryPageHasStructuredConditionsPaginationExportAndNoSqlEditor() throws Exception {
        runOnEdtAndWait(() -> {
            int[] actions = new int[4];
            DataQueryPage.Components components = queryComponents();
            DataQueryPage page = new DataQueryPage(
                    components,
                    new DataQueryPage.Actions(
                            () -> actions[0]++, () -> actions[1]++, () -> actions[2]++, () -> actions[3]++,
                            () -> { }, () -> { }));
            List<String> labels = findAll(page, JLabel.class).stream().map(JLabel::getText).toList();
            for (String field : List.of(
                    "地码关键字：", "货码关键字：", "区域编码：", "关联区域编码：",
                    "更新时间起：", "更新时间止：", "每页行数：", "第 1 / 1 页", "总记录数：0")) {
                assertTrue(labels.contains(field), "missing structured query field " + field);
            }
            List<String> buttons = List.of("执行只读查询", "上一页", "下一页", "导出当前结果");
            for (int index = 0; index < buttons.size(); index++) {
                AbstractButton button = findButton(page, buttons.get(index));
                assertTrue(button != null, "missing query action " + buttons.get(index));
                button.setEnabled(true);
                button.doClick();
                assertEquals(1, actions[index], "query action callback " + buttons.get(index));
            }
            for (JTextArea area : findAll(page, JTextArea.class)) {
                assertFalse(area.isEditable(), "query page must not expose an editable SQL area");
            }
            assertTrue(collectText(page).contains("不支持 SQL 编辑"), "query page must state SQL editor is unavailable");
        });
    }

    private static void queryConditionEditResetsPaginationState() throws Exception {
        runOnEdtAndWait(() -> {
            int[] resets = new int[1];
            DataQueryPage.Components components = queryComponents();
            new DataQueryPage(
                    components,
                    new DataQueryPage.Actions(
                            () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> resets[0]++));
            components.pointKeyword().setText("USE_POINT");
            assertTrue(resets[0] > 0, "editing a condition must reset pagination to page one");
        });
    }

    private static void browserPageHasThreeFinalColumnsAndRealActions() throws Exception {
        runOnEdtAndWait(() -> {
            int[] actions = new int[3];
            DataSourceBrowserPage.Components components = browserComponents();
            components.previewTable().setModel(new DefaultTableModel(
                    new Object[] {"status", "map_data_code", "pod_code", "date_chg"}, 0));
            DataSourceBrowserPage page = new DataSourceBrowserPage(
                    components,
                    new DataSourceBrowserPage.Actions(
                            () -> actions[0]++, () -> actions[1]++, () -> { }, () -> actions[2]++),
                    new TableColumnLayoutStore(Files.createTempDirectory("browser-page-layout")
                            .resolve("table-column-layout.properties")));
            page.previewLoaded("public", "tcs_map_data");
            List<String> titles = findAll(page, SectionCard.class).stream().map(SectionCard::title).toList();
            assertTrue(titles.contains("Schema / 表 / 视图对象树"), "browser left tree column");
            assertTrue(titles.contains("对象元数据与列信息"), "browser center metadata column");
            assertTrue(titles.contains("前 100 行只读预览"), "browser right preview column");
            List<String> buttons = List.of("刷新 Schema", "加载表/视图", "预览前100行");
            for (int index = 0; index < buttons.size(); index++) {
                AbstractButton button = findButton(page, buttons.get(index));
                assertTrue(button != null, "missing browser action " + buttons.get(index));
                button.doClick();
                assertEquals(1, actions[index], "browser action callback " + buttons.get(index));
            }
            assertTrue(findButton(page, "固定重要列") != null,
                    "preview must expose a visible fixed-column entry");
            assertTrue(findButton(page, "恢复原顺序") != null,
                    "preview must expose an obvious reset action");
            assertTrue(findAll(page, ReadableTableScrollPane.class).size() >= 3,
                    "browser tables must support horizontal browsing");
            assertEquals(32, findAll(page, JTable.class).get(0).getRowHeight(), "browser table row height");
            assertFalse(collectText(page).contains("执行 SQL"), "browser must not offer SQL execution");
        });
    }

    private static void browserPreviewHasEnoughSpaceForTheImportantColumnPreset() throws Exception {
        runOnEdtAndWait(() -> {
            DataSourceBrowserPage.Components components = browserComponents();
            DefaultTableModel previewModel = new DefaultTableModel(
                    new Object[] {"map_data_code", "pod_code", "pod_status", "status", "date_chg"},
                    0);
            previewModel.addRow(new Object[] {
                    "USE_POINT_001", "SHELF_BACKUP_001", "0", "1", "2026-07-16 10:00:00"
            });
            components.previewTable().setModel(previewModel);
            DataSourceBrowserPage page = new DataSourceBrowserPage(
                    components,
                    new DataSourceBrowserPage.Actions(() -> { }, () -> { }, () -> { }, () -> { }),
                    new TableColumnLayoutStore(Files.createTempDirectory("browser-preview-width")
                            .resolve("table-column-layout.properties")));
            page.setSize(1184, 700);
            layoutTree(page);
            layoutTree(page);
            page.previewLoaded("public", "tcs_map_data");
            PinnedTablePane previewPane = findAll(page, PinnedTablePane.class).get(0);
            previewPane.applyPinnedColumns(List.of("map_data_code", "pod_code"));
            assertEquals(
                    List.of("map_data_code", "pod_code"),
                    previewPane.pinnedColumns(),
                    "normal 1440-wide layout must allow the one-click important-column preset");
        });
    }

    private static DataQueryPage.Components queryComponents() {
        return new DataQueryPage.Components(
                new JTextField(), new JTextField(), new JTextField(), new JTextField(),
                new JTextField(), new JTextField(), new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1)),
                new javax.swing.JButton("上一页"), new javax.swing.JButton("下一页"),
                new JLabel("第 1 / 1 页"), new JLabel("总记录数：0"),
                new JTable(new DefaultTableModel(new Object[] {"点位编码", "查询状态"}, 0)),
                new JTextArea());
    }

    private static DataSourceBrowserPage.Components browserComponents() {
        return new DataSourceBrowserPage.Components(
                new JLabel("Schema 数量：0"), new JLabel("对象数量：0"),
                new JLabel("对象类型：--"), new JLabel("浏览模式：只读"),
                new JComboBox<>(new String[] {"public"}), new JTree(),
                new JTable(new DefaultTableModel(new Object[] {"Schema", "Name", "Type"}, 0)),
                new JTable(new DefaultTableModel(new Object[] {"列名", "类型"}, 0)),
                new JTable(new DefaultTableModel()));
    }

    private static String collectText(Component root) {
        List<String> values = new ArrayList<>();
        for (JLabel label : findAll(root, JLabel.class)) {
            values.add(label.getText());
        }
        for (AbstractButton button : findAll(root, AbstractButton.class)) {
            values.add(button.getText());
        }
        for (JTextArea area : findAll(root, JTextArea.class)) {
            values.add(area.getText());
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

    private static void layoutTree(Component component) {
        if (component instanceof java.awt.Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
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
