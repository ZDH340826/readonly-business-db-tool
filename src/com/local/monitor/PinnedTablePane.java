package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

public final class PinnedTablePane extends javax.swing.JPanel {
    private final JTable scrollingTable;
    private final JTable pinnedTable;
    private final ReadableTableScrollPane scrollPane;
    private final TableColumnLayoutStore store;
    private final JButton configureButton = UiFactory.primaryButton("固定重要列");
    private final JButton restoreButton = UiFactory.secondaryButton("恢复原顺序");
    private String schema;
    private String tableName;
    private List<String> pinnedColumns = List.of();

    public PinnedTablePane(JTable scrollingTable, TableColumnLayoutStore store) {
        super(new BorderLayout());
        if (scrollingTable == null) {
            throw new IllegalArgumentException("预览表格不能为空");
        }
        if (store == null) {
            throw new IllegalArgumentException("表格布局存储不能为空");
        }
        this.scrollingTable = scrollingTable;
        this.store = store;
        this.pinnedTable = new JTable(scrollingTable.getModel());
        this.pinnedTable.setAutoCreateColumnsFromModel(false);
        this.scrollingTable.setAutoCreateColumnsFromModel(false);
        this.pinnedTable.setSelectionModel(scrollingTable.getSelectionModel());
        UiFactory.configureTable(this.scrollingTable);
        UiFactory.configureTable(this.pinnedTable);
        this.scrollingTable.getTableHeader().setPreferredSize(new Dimension(0, 52));
        this.pinnedTable.getTableHeader().setPreferredSize(new Dimension(0, 52));
        this.scrollPane = new ReadableTableScrollPane(scrollingTable);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);
        add(buildToolbar(), BorderLayout.NORTH);
        add(this.scrollPane, BorderLayout.CENTER);
        rebuildColumns();
        configureFixedArea();
        configureButton.addActionListener(event -> openChooser());
        restoreButton.addActionListener(event -> restoreFromButton());
        updateActionStates();
    }

    public void showTable(String schema, String tableName) {
        this.schema = requiredName(schema, "Schema");
        this.tableName = requiredName(tableName, "表名");
        List<String> saved = PinnedColumnLayout.normalize(
                availableColumns(),
                store.load(schema, tableName));
        applyPinnedColumnsInternal(saved, false);
        updateActionStates();
    }

    public void applyPinnedColumns(List<String> requested) throws IOException {
        requireTableContext();
        List<String> normalized = PinnedColumnLayout.normalize(availableColumns(), requested);
        try {
            applyPinnedColumnsInternal(normalized, true);
        } finally {
            updateActionStates();
        }
    }

    public void restoreOriginalOrder() throws IOException {
        requireTableContext();
        try {
            applyPinnedColumnsInternal(List.of(), false);
            store.clear(schema, tableName);
        } finally {
            updateActionStates();
        }
    }

    public List<String> pinnedColumns() {
        return pinnedColumns;
    }

    public List<String> scrollingColumns() {
        return viewColumnNames(scrollingTable);
    }

    JTable pinnedTable() {
        return pinnedTable;
    }

    JScrollPane scrollPane() {
        return scrollPane;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(0, 4));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        toolbar.add(new JLabel("左右滚动查看全部列；常用列可固定在左侧"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(configureButton);
        actions.add(restoreButton);
        toolbar.add(actions, BorderLayout.SOUTH);
        return toolbar;
    }

    private void openChooser() {
        try {
            requireTableContext();
            PinnedColumnChooserPanel chooser = new PinnedColumnChooserPanel(
                    availableColumns(),
                    pinnedColumns);
            Object[] options = {"应用固定", "取消"};
            int result = JOptionPane.showOptionDialog(
                    this,
                    chooser,
                    "固定重要列",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]);
            if (result == 0) {
                applyPinnedColumns(chooser.selectedColumns());
            }
        } catch (Exception exception) {
            showDirectError(exception);
        }
    }

    private void restoreFromButton() {
        try {
            restoreOriginalOrder();
        } catch (Exception exception) {
            showDirectError(exception);
        }
    }

    private void showDirectError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "操作失败，请重试";
        }
        JOptionPane.showMessageDialog(this, message, "操作未完成", JOptionPane.WARNING_MESSAGE);
    }

    private void updateActionStates() {
        configureButton.setEnabled(schema != null && tableName != null);
        restoreButton.setEnabled(!pinnedColumns.isEmpty());
    }

    private void applyPinnedColumnsInternal(List<String> normalized, boolean persist) {
        rebuildColumns();
        validateFixedWidth(normalized);
        Set<Integer> fixedModelColumns = new HashSet<>();
        for (String name : normalized) {
            fixedModelColumns.add(modelIndex(name));
        }
        removeColumns(scrollingTable, fixedModelColumns, true);
        removeColumns(pinnedTable, fixedModelColumns, false);
        for (int target = 0; target < normalized.size(); target++) {
            int current = pinnedTable.convertColumnIndexToView(modelIndex(normalized.get(target)));
            if (current >= 0 && current != target) {
                pinnedTable.moveColumn(current, target);
            }
        }
        pinnedColumns = List.copyOf(normalized);
        configureFixedArea();
        if (persist) {
            try {
                store.save(schema, tableName, pinnedColumns);
            } catch (IOException exception) {
                applyPinnedColumnsInternal(List.of(), false);
                throw new LayoutSaveException(exception);
            }
        }
    }

    private void validateFixedWidth(List<String> normalized) {
        if (normalized.isEmpty()) {
            return;
        }
        int width = 0;
        for (String name : normalized) {
            int viewIndex = pinnedTable.convertColumnIndexToView(modelIndex(name));
            if (viewIndex >= 0) {
                width += pinnedTable.getColumnModel().getColumn(viewIndex).getPreferredWidth();
            }
        }
        int availableWidth = getWidth() > 0 ? getWidth() : 900;
        if (width > Math.floor(availableWidth * 0.45d)) {
            throw new IllegalArgumentException("固定列太多，请先取消一列");
        }
    }

    private void rebuildColumns() {
        scrollingTable.createDefaultColumnsFromModel();
        pinnedTable.createDefaultColumnsFromModel();
        applyHeaderLabels(scrollingTable);
        applyHeaderLabels(pinnedTable);
        ReadableTableColumns.resizeNow(scrollingTable);
        ReadableTableColumns.resizeNow(pinnedTable);
        applyPreferredWidths(scrollingTable);
        applyPreferredWidths(pinnedTable);
    }

    private void configureFixedArea() {
        if (pinnedColumns.isEmpty()) {
            scrollPane.setRowHeaderView(null);
            scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);
            return;
        }
        int width = totalColumnWidth(pinnedTable);
        pinnedTable.setPreferredScrollableViewportSize(new Dimension(width, 0));
        scrollPane.setRowHeaderView(pinnedTable);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, pinnedTable.getTableHeader());
    }

    private List<String> availableColumns() {
        TableModel model = scrollingTable.getModel();
        List<String> names = new ArrayList<>();
        for (int index = 0; index < model.getColumnCount(); index++) {
            names.add(model.getColumnName(index));
        }
        return names;
    }

    private List<String> viewColumnNames(JTable table) {
        List<String> names = new ArrayList<>();
        TableModel model = table.getModel();
        for (int viewIndex = 0; viewIndex < table.getColumnCount(); viewIndex++) {
            int modelIndex = table.getColumnModel().getColumn(viewIndex).getModelIndex();
            names.add(model.getColumnName(modelIndex));
        }
        return List.copyOf(names);
    }

    private int modelIndex(String name) {
        TableModel model = scrollingTable.getModel();
        for (int index = 0; index < model.getColumnCount(); index++) {
            if (model.getColumnName(index).equalsIgnoreCase(name)) {
                return index;
            }
        }
        throw new IllegalArgumentException("找不到要固定的列：" + name);
    }

    private static void removeColumns(JTable table, Set<Integer> fixedModelColumns, boolean removeFixed) {
        for (int viewIndex = table.getColumnCount() - 1; viewIndex >= 0; viewIndex--) {
            TableColumn column = table.getColumnModel().getColumn(viewIndex);
            boolean fixed = fixedModelColumns.contains(column.getModelIndex());
            if (fixed == removeFixed) {
                table.removeColumn(column);
            }
        }
    }

    private static void applyHeaderLabels(JTable table) {
        TableModel model = table.getModel();
        for (int viewIndex = 0; viewIndex < table.getColumnCount(); viewIndex++) {
            TableColumn column = table.getColumnModel().getColumn(viewIndex);
            column.setHeaderValue(DatabaseFieldLabel.display(model.getColumnName(column.getModelIndex())));
        }
    }

    private static void applyPreferredWidths(JTable table) {
        for (int viewIndex = 0; viewIndex < table.getColumnCount(); viewIndex++) {
            TableColumn column = table.getColumnModel().getColumn(viewIndex);
            column.setWidth(column.getPreferredWidth());
        }
    }

    private static int totalColumnWidth(JTable table) {
        int width = 0;
        for (int index = 0; index < table.getColumnCount(); index++) {
            width += table.getColumnModel().getColumn(index).getWidth();
        }
        return width;
    }

    private void requireTableContext() {
        if (schema == null || tableName == null) {
            throw new IllegalStateException("请先预览表格，再固定重要列");
        }
    }

    private static String requiredName(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value;
    }

    private static final class LayoutSaveException extends RuntimeException {
        private LayoutSaveException(IOException cause) {
            super("固定列保存失败，请检查本机数据目录是否可写", cause);
        }
    }
}
