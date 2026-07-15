package com.local.monitor;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public final class ReadableTableTest {
    private ReadableTableTest() {
    }

    public static void main(String[] args) throws Exception {
        standardTablesDoNotCompressAllColumns();
        headersAndRowsDetermineBoundedPreferredWidths();
        modelStructureChangesRecalculateNewColumns();
        tableScrollPaneShowsHorizontalRangeOnlyWhenNeeded();
        shiftWheelMovesTheHorizontalBar();
        System.out.println("ReadableTableTest PASS");
    }

    private static void standardTablesDoNotCompressAllColumns() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = tableWithWideContent();
            UiFactory.configureTable(table);
            TestSupport.assertEquals(
                    JTable.AUTO_RESIZE_OFF,
                    table.getAutoResizeMode(),
                    "wide tables must preserve readable column widths");
        });
    }

    private static JTable tableWithWideContent() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] {"地码（map_data_code）", "货码（pod_code）", "更新时间"},
                0);
        model.addRow(new Object[] {
                "ABCDEFBB123456",
                "POD-CODE-VERY-LONG-001",
                "2026-07-16 14:30:00"
        });
        return new JTable(model);
    }

    private static void headersAndRowsDetermineBoundedPreferredWidths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = tableWithWideContent();
            UiFactory.configureTable(table);
            boolean hasContentSizedColumn = false;
            for (int index = 0; index < table.getColumnCount(); index++) {
                int width = table.getColumnModel().getColumn(index).getPreferredWidth();
                TestSupport.assertTrue(width >= 80, "column must retain a readable minimum width");
                TestSupport.assertTrue(width <= 320, "one value must not create an unbounded column");
                hasContentSizedColumn |= width > 80;
            }
            TestSupport.assertTrue(hasContentSizedColumn,
                    "long headers or values must expand beyond the minimum width");
        });
    }

    private static void modelStructureChangesRecalculateNewColumns() throws Exception {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"短列"}, 0);
        JTable[] table = new JTable[1];
        SwingUtilities.invokeAndWait(() -> {
            table[0] = new JTable(model);
            UiFactory.configureTable(table[0]);
        });
        SwingUtilities.invokeAndWait(() -> {
            model.setColumnIdentifiers(new Object[] {
                    "地码（map_data_code）",
                    "货码（pod_code）",
                    "更新时间"
            });
            model.addRow(new Object[] {
                    "ABCDEFBB123456",
                    "POD-CODE-VERY-LONG-001",
                    "2026-07-16 14:30:00"
            });
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> {
            TestSupport.assertEquals(3, table[0].getColumnModel().getColumnCount(),
                    "new preview columns must participate in readable sizing");
            for (int index = 0; index < table[0].getColumnCount(); index++) {
                int width = table[0].getColumnModel().getColumn(index).getPreferredWidth();
                TestSupport.assertTrue(width >= 80,
                        "new preview columns must be resized after a structure change");
            }
        });
    }

    private static void tableScrollPaneShowsHorizontalRangeOnlyWhenNeeded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = tableWithEightReadableColumns();
            JScrollPane pane = UiFactory.tableScrollPane(table);
            pane.setSize(420, 240);
            pane.doLayout();
            TestSupport.assertEquals(
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED,
                    pane.getHorizontalScrollBarPolicy(),
                    "wide tables need an as-needed horizontal bar");
            TestSupport.assertTrue(
                    table.getPreferredSize().width > pane.getViewport().getExtentSize().width,
                    "wide table content must exceed the viewport instead of being compressed");
        });
    }

    private static void shiftWheelMovesTheHorizontalBar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = tableWithEightReadableColumns();
            JScrollPane pane = UiFactory.tableScrollPane(table);
            pane.setSize(320, 220);
            pane.doLayout();
            JScrollBar horizontal = pane.getHorizontalScrollBar();
            TestSupport.assertTrue(horizontal.isVisible(),
                    "wide table must expose its horizontal scrollbar");
            int before = horizontal.getValue();
            MouseWheelEvent event = new MouseWheelEvent(
                    pane,
                    MouseEvent.MOUSE_WHEEL,
                    System.currentTimeMillis(),
                    InputEvent.SHIFT_DOWN_MASK,
                    120,
                    80,
                    0,
                    false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    3,
                    2);
            pane.dispatchEvent(event);
            TestSupport.assertTrue(horizontal.getValue() > before,
                    "Shift plus mouse wheel must move the horizontal scrollbar");
        });
    }

    private static JTable tableWithEightReadableColumns() {
        Object[] columns = new Object[8];
        Object[] row = new Object[8];
        for (int index = 0; index < columns.length; index++) {
            columns[index] = "完整字段名称" + (index + 1);
            row[index] = "示例数据-" + (index + 1);
        }
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        model.addRow(row);
        JTable table = new JTable(model);
        UiFactory.configureTable(table);
        return table;
    }

    private static final class TestSupport {
        private static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        private static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
