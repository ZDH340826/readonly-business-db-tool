package com.local.monitor;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

public final class UiFactory {
    private UiFactory() {
    }

    public static JButton primaryButton(String text) {
        return button(text, AppTheme.PRIMARY, Color.WHITE, AppTheme.PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return button(text, AppTheme.CARD_BACKGROUND, AppTheme.TEXT_PRIMARY, AppTheme.BORDER);
    }

    public static JButton dangerButton(String text) {
        return button(text, AppTheme.DANGER, Color.WHITE, AppTheme.DANGER);
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
    }

    private static JButton button(String text, Color background, Color foreground, Color border) {
        JButton button = new JButton(text);
        button.setFont(AppTheme.font(Font.BOLD, 14f));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
