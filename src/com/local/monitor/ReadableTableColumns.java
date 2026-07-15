package com.local.monitor;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

final class ReadableTableColumns {
    static final int MIN_WIDTH = 80;
    static final int MAX_WIDTH = 320;
    static final int SAMPLE_ROWS = 100;
    private static final int WIDTH_PADDING = 24;
    private static final String CONTROLLER_PROPERTY =
            ReadableTableColumns.class.getName() + ".controller";

    private ReadableTableColumns() {
    }

    static void install(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Object existing = table.getClientProperty(CONTROLLER_PROPERTY);
        if (existing instanceof Controller) {
            ((Controller) existing).requestResize();
            return;
        }
        Controller controller = new Controller(table);
        table.putClientProperty(CONTROLLER_PROPERTY, controller);
        controller.install();
    }

    static void resizeNow(JTable table) {
        int rows = Math.min(table.getRowCount(), SAMPLE_ROWS);
        for (int viewColumn = 0; viewColumn < table.getColumnCount(); viewColumn++) {
            TableColumn column = table.getColumnModel().getColumn(viewColumn);
            int preferred = headerWidth(table, column, viewColumn);
            for (int row = 0; row < rows; row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, viewColumn);
                Component component = table.prepareRenderer(renderer, row, viewColumn);
                preferred = Math.max(preferred, component.getPreferredSize().width + WIDTH_PADDING);
            }
            column.setMinWidth(MIN_WIDTH);
            column.setPreferredWidth(Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, preferred)));
        }
    }

    private static int headerWidth(JTable table, TableColumn column, int viewColumn) {
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component component = renderer.getTableCellRendererComponent(
                table,
                column.getHeaderValue(),
                false,
                false,
                -1,
                viewColumn);
        return component.getPreferredSize().width + WIDTH_PADDING;
    }

    private static final class Controller implements TableModelListener, PropertyChangeListener {
        private final JTable table;
        private TableModel model;
        private boolean resizeScheduled;

        private Controller(JTable table) {
            this.table = table;
        }

        private void install() {
            table.addPropertyChangeListener("model", this);
            attachTo(table.getModel());
            if (SwingUtilities.isEventDispatchThread()) {
                resizeNow(table);
            } else {
                requestResize();
            }
        }

        private void attachTo(TableModel nextModel) {
            if (model != null) {
                model.removeTableModelListener(this);
            }
            model = nextModel;
            if (model != null) {
                model.addTableModelListener(this);
            }
        }

        @Override
        public void tableChanged(TableModelEvent event) {
            requestResize();
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            attachTo(table.getModel());
            requestResize();
        }

        private void requestResize() {
            if (resizeScheduled) {
                return;
            }
            resizeScheduled = true;
            SwingUtilities.invokeLater(() -> {
                resizeScheduled = false;
                resizeNow(table);
            });
        }
    }
}
