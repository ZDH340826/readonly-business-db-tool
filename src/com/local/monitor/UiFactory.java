package com.local.monitor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;

public final class UiFactory {
    private UiFactory() {
    }

    public static JButton primaryButton(String text) {
        return styleActionButton(new JButton(text), AppTheme.PRIMARY, Color.WHITE, AppTheme.PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return styleActionButton(
                new JButton(text), AppTheme.CARD_BACKGROUND, AppTheme.TEXT_PRIMARY, AppTheme.BORDER);
    }

    public static JButton dangerButton(String text) {
        return styleActionButton(new JButton(text), AppTheme.DANGER, Color.WHITE, AppTheme.DANGER);
    }

    public static void configureTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(AppTheme.font(Font.PLAIN, 13f));
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setBackground(AppTheme.CARD_BACKGROUND);
        table.setSelectionBackground(AppTheme.PRIMARY_LIGHT);
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setGridColor(AppTheme.BORDER);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setBackground(new Color(0xF8, 0xFA, 0xFC));
        table.getTableHeader().setForeground(AppTheme.TEXT_SECONDARY);
        table.getTableHeader().setFont(AppTheme.font(Font.BOLD, 13f));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        ReadableTableColumns.install(table);
    }

    public static void configureStatusColumn(JTable table, int modelColumn) {
        if (modelColumn < 0 || modelColumn >= table.getColumnModel().getColumnCount()) {
            throw new IllegalArgumentException("status column is out of range: " + modelColumn);
        }
        table.getColumnModel().getColumn(modelColumn).setCellRenderer(new SemanticStatusRenderer());
    }

    static JButton styleActionButton(
            JButton button,
            Color enabledBackground,
            Color enabledForeground,
            Color enabledBorder) {
        button.setUI(new BasicButtonUI());
        button.setFont(AppTheme.font(Font.BOLD, 14f));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addPropertyChangeListener("enabled", event -> applyButtonState(
                button,
                enabledBackground,
                enabledForeground,
                enabledBorder));
        applyButtonState(button, enabledBackground, enabledForeground, enabledBorder);
        return button;
    }

    private static void applyButtonState(
            JButton button,
            Color enabledBackground,
            Color enabledForeground,
            Color enabledBorder) {
        Color background = button.isEnabled()
                ? enabledBackground
                : AppTheme.softBackground(AppTheme.MUTED);
        Color foreground = button.isEnabled()
                ? enabledForeground
                : AppTheme.TEXT_SECONDARY;
        Color border = button.isEnabled() ? enabledBorder : AppTheme.MUTED;
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
    }

    private static final class SemanticStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            String text = value == null ? "--" : String.valueOf(value);
            Color semanticColor = statusColor(text);
            label.setText(text);
            label.setFont(AppTheme.font(Font.BOLD, 12f));
            label.setForeground(semanticColor);
            label.setBackground(isSelected ? table.getSelectionBackground() : AppTheme.softBackground(semanticColor));
            label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            return label;
        }

        private static Color statusColor(String text) {
            if (text.contains("查询失败") || text.contains("数据不可用")) {
                return AppTheme.QUERY_FAILED;
            }
            if (text.contains("缺料") || text.contains("需关注") || text.contains("活跃报警")) {
                return AppTheme.DANGER;
            }
            if (text.contains("观察中")) {
                return AppTheme.WARNING;
            }
            if (text.contains("已关注")) {
                return AppTheme.ACKNOWLEDGED;
            }
            if (text.contains("已恢复")) {
                return AppTheme.RECOVERED;
            }
            if (text.contains("正常") || text.contains("有料")) {
                return AppTheme.SUCCESS;
            }
            return AppTheme.MUTED;
        }
    }
}
