package com.local.monitor;

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
