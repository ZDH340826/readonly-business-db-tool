package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public final class PinnedTablePaneTest {
    private PinnedTablePaneTest() {
    }

    public static void main(String[] args) throws Exception {
        databaseIdentifiersHaveOperatorFriendlyLabels();
        presetCreatesTwoFrozenAdjacentColumnsWithoutChangingTheModel();
        restoreClearsOnlyTheViewLayout();
        fixedAreaOverFortyFivePercentIsRejectedInChinese();
        structureChangeKeepsValidPinsAndLeavesNewColumnsScrollable();
        chooserUsesVisibleChineseControlsAndOneClickPreset();
        mainPaneExposesDiscoverableActionsWithSafeStates();
        System.out.println("PinnedTablePaneTest PASS");
    }

    private static void databaseIdentifiersHaveOperatorFriendlyLabels() {
        String landCode = DatabaseFieldLabel.display("map_data_code");
        String podCode = DatabaseFieldLabel.display("POD_CODE");
        TestSupport.assertContains(landCode, "地码", "map_data_code must use the agreed operator term");
        TestSupport.assertContains(landCode, "map_data_code", "technical identifier must remain visible as context");
        TestSupport.assertContains(podCode, "货码", "pod_code must use the agreed operator term");
        TestSupport.assertContains(podCode, "pod_code", "technical identifier must remain visible as context");
        TestSupport.assertEquals("status", DatabaseFieldLabel.display("status"),
                "unmapped database columns must retain their real names");
    }

    private static void presetCreatesTwoFrozenAdjacentColumnsWithoutChangingTheModel() throws Exception {
        TableColumnLayoutStore store = temporaryStore();
        DefaultTableModel model = previewModel(
                "status", "map_data_code", "date_chg", "pod_code", "area_code");
        PinnedTablePane[] pane = new PinnedTablePane[1];
        JTable[] scrolling = new JTable[1];
        runOnEdtAndWait(() -> {
            scrolling[0] = new JTable(model);
            pane[0] = new PinnedTablePane(scrolling[0], store);
            pane[0].setSize(900, 500);
            pane[0].doLayout();
            pane[0].showTable("public", "tcs_map_data");
            pane[0].applyPinnedColumns(PinnedColumnLayout.presetFor(modelColumnNames(model)));

            TestSupport.assertEquals(
                    List.of("map_data_code", "pod_code"),
                    pane[0].pinnedColumns(),
                    "land code and pod code must be adjacent in the fixed area");
            TestSupport.assertEquals(
                    List.of("status", "date_chg", "area_code"),
                    pane[0].scrollingColumns(),
                    "non-fixed database columns must retain their model order");
            TestSupport.assertEquals(
                    List.of("status", "map_data_code", "date_chg", "pod_code", "area_code"),
                    modelColumnNames(model),
                    "fixed columns must not rewrite database/model column order");
            TestSupport.assertTrue(
                    pane[0].pinnedTable().getModel() == scrolling[0].getModel(),
                    "fixed and scrolling areas must share one model");
            TestSupport.assertTrue(
                    pane[0].pinnedTable().getSelectionModel() == scrolling[0].getSelectionModel(),
                    "fixed and scrolling areas must share row selection");
            TestSupport.assertContains(
                    String.valueOf(pane[0].pinnedTable().getColumnModel().getColumn(0).getHeaderValue()),
                    "地码",
                    "first fixed header must use the operator term land code");
            TestSupport.assertContains(
                    String.valueOf(pane[0].pinnedTable().getColumnModel().getColumn(1).getHeaderValue()),
                    "货码",
                    "second fixed header must use the operator term pod code");
        });
        TestSupport.assertEquals(
                List.of("map_data_code", "pod_code"),
                store.load("public", "tcs_map_data"),
                "applied fixed columns must persist only their identifiers");
    }

    private static void restoreClearsOnlyTheViewLayout() throws Exception {
        TableColumnLayoutStore store = temporaryStore();
        DefaultTableModel model = previewModel("status", "map_data_code", "pod_code");
        PinnedTablePane[] pane = new PinnedTablePane[1];
        runOnEdtAndWait(() -> {
            pane[0] = new PinnedTablePane(new JTable(model), store);
            pane[0].setSize(900, 500);
            pane[0].showTable("public", "tcs_map_data");
            pane[0].applyPinnedColumns(List.of("map_data_code", "pod_code"));
            pane[0].restoreOriginalOrder();
            TestSupport.assertEquals(List.of(), pane[0].pinnedColumns(),
                    "restore must clear the fixed area");
            TestSupport.assertEquals(
                    List.of("status", "map_data_code", "pod_code"),
                    pane[0].scrollingColumns(),
                    "restore must return every column to model order");
            TestSupport.assertEquals(
                    List.of("status", "map_data_code", "pod_code"),
                    modelColumnNames(model),
                    "restore must not rewrite model order");
        });
        TestSupport.assertEquals(List.of(), store.load("public", "tcs_map_data"),
                "restore must clear the saved layout for this table");
    }

    private static void fixedAreaOverFortyFivePercentIsRejectedInChinese() throws Exception {
        DefaultTableModel model = previewModel("a", "b", "c", "d", "e");
        runOnEdtAndWait(() -> {
            PinnedTablePane pane = new PinnedTablePane(new JTable(model), temporaryStore());
            pane.setSize(600, 400);
            pane.showTable("public", "wide_table");
            IllegalArgumentException error = TestSupport.assertThrowsReturning(
                    IllegalArgumentException.class,
                    () -> pane.applyPinnedColumns(List.of("a", "b", "c", "d")),
                    "oversized fixed area must be rejected");
            TestSupport.assertEquals(
                    "固定列太多，请先取消一列",
                    error.getMessage(),
                    "operator must receive a direct Chinese recovery message");
        });
    }

    private static void structureChangeKeepsValidPinsAndLeavesNewColumnsScrollable() throws Exception {
        TableColumnLayoutStore store = temporaryStore();
        store.save("public", "tcs_map_data", List.of("map_data_code", "missing", "pod_code"));
        DefaultTableModel model = previewModel(
                "status", "map_data_code", "pod_code", "new_column");
        runOnEdtAndWait(() -> {
            PinnedTablePane pane = new PinnedTablePane(new JTable(model), store);
            pane.setSize(900, 500);
            pane.showTable("public", "tcs_map_data");
            TestSupport.assertEquals(
                    List.of("map_data_code", "pod_code"),
                    pane.pinnedColumns(),
                    "valid saved pins must survive structure changes");
            TestSupport.assertTrue(
                    pane.scrollingColumns().contains("new_column"),
                    "new database columns must remain visible in the scrolling area");
            TestSupport.assertEquals(
                    List.of("status", "map_data_code", "pod_code", "new_column"),
                    modelColumnNames(model),
                    "restoring a view layout must not alter the preview model");
        });
    }

    private static void chooserUsesVisibleChineseControlsAndOneClickPreset() throws Exception {
        runOnEdtAndWait(() -> {
            PinnedColumnChooserPanel chooser = new PinnedColumnChooserPanel(
                    List.of("status", "map_data_code", "date_chg", "pod_code"),
                    List.of("status"));
            AbstractButton preset = findButton(chooser, "一键固定地码和货码");
            TestSupport.assertTrue(preset != null,
                    "operator must see a one-click land-code and pod-code action");
            TestSupport.assertTrue(findButton(chooser, "上移") != null,
                    "operator must see an explicit move-up action");
            TestSupport.assertTrue(findButton(chooser, "下移") != null,
                    "operator must see an explicit move-down action");
            preset.doClick();
            TestSupport.assertEquals(
                    List.of("map_data_code", "pod_code"),
                    chooser.selectedColumns(),
                    "one click must replace the selection with adjacent business columns");
        });
    }

    private static void mainPaneExposesDiscoverableActionsWithSafeStates() throws Exception {
        runOnEdtAndWait(() -> {
            PinnedTablePane pane = new PinnedTablePane(
                    new JTable(previewModel("status", "map_data_code", "pod_code")),
                    temporaryStore());
            AbstractButton configure = findButton(pane, "固定重要列");
            AbstractButton restore = findButton(pane, "恢复原顺序");
            TestSupport.assertTrue(configure != null,
                    "operator must see an explicit fixed-column entry point");
            TestSupport.assertTrue(restore != null,
                    "operator must see an explicit restore action");
            TestSupport.assertTrue(!configure.isEnabled(),
                    "configuration must stay disabled before a table is previewed");
            TestSupport.assertTrue(!restore.isEnabled(),
                    "restore must stay disabled while no columns are fixed");
            Component toolbar = ((BorderLayout) pane.getLayout())
                    .getLayoutComponent(BorderLayout.NORTH);
            TestSupport.assertTrue(
                    toolbar.getPreferredSize().height > configure.getPreferredSize().height + 20,
                    "narrow preview must reserve a separate visible row for the action buttons");

            pane.setSize(900, 500);
            pane.showTable("public", "tcs_map_data");
            TestSupport.assertTrue(configure.isEnabled(),
                    "configuration must become available after preview loads");
            TestSupport.assertTrue(!restore.isEnabled(),
                    "restore must remain disabled until a fixed layout exists");

            pane.applyPinnedColumns(List.of("map_data_code", "pod_code"));
            TestSupport.assertTrue(restore.isEnabled(),
                    "restore must become available after columns are fixed");
            pane.restoreOriginalOrder();
            TestSupport.assertTrue(!restore.isEnabled(),
                    "restore must disable again after returning to database order");
        });
    }

    private static AbstractButton findButton(Component component, String text) {
        if (component instanceof AbstractButton && text.equals(((AbstractButton) component).getText())) {
            return (AbstractButton) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                AbstractButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static DefaultTableModel previewModel(String... columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        Object[] row = new Object[columns.length];
        for (int index = 0; index < row.length; index++) {
            row[index] = "值" + index;
        }
        model.addRow(row);
        return model;
    }

    private static TableColumnLayoutStore temporaryStore() throws Exception {
        return new TableColumnLayoutStore(
                Files.createTempDirectory("pinned-table-pane").resolve("table-column-layout.properties"));
    }

    private static List<String> modelColumnNames(DefaultTableModel model) {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < model.getColumnCount(); index++) {
            names.add(model.getColumnName(index));
        }
        return names;
    }

    private static void runOnEdtAndWait(ThrowingRunnable action) throws Exception {
        final Exception[] failure = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                failure[0] = exception;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    private static final class TestSupport {
        private static void assertContains(String actual, String expected, String message) {
            if (actual == null || !actual.toLowerCase(java.util.Locale.ROOT)
                    .contains(expected.toLowerCase(java.util.Locale.ROOT))) {
                throw new AssertionError(message + " expected fragment=" + expected + " actual=" + actual);
            }
        }

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

        private static <T extends Throwable> T assertThrowsReturning(
                Class<T> type,
                ThrowingRunnable action,
                String message) {
            try {
                action.run();
            } catch (Throwable error) {
                if (type.isInstance(error)) {
                    return type.cast(error);
                }
                throw new AssertionError(message + " wrong exception=" + error, error);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
