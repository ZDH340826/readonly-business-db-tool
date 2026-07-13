package com.local.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

public final class AppThemeTest {
    private AppThemeTest() {
    }

    public static void main(String[] args) {
        exposesRequiredWindowAndShellGeometry();
        exposesRequiredSemanticColors();
        usesChineseCapableSystemFontWithFallback();
        configuresTablesConsistently();
        createsDistinctButtonRoles();
        reusableCardsAndBadgeExposeTextAndSemanticColor();
        System.out.println("AppThemeTest PASS");
    }

    private static void exposesRequiredWindowAndShellGeometry() {
        assertEquals(new Dimension(1440, 900), AppTheme.PREFERRED_WINDOW_SIZE, "preferred window size");
        assertEquals(new Dimension(1180, 760), AppTheme.MINIMUM_WINDOW_SIZE, "minimum window size");
        assertEquals(210, AppTheme.NAVIGATION_WIDTH, "navigation width");
        assertEquals(56, AppTheme.TOP_BAR_HEIGHT, "top status bar height");
        assertEquals(28, AppTheme.BOTTOM_BAR_HEIGHT, "bottom status bar height");
    }

    private static void exposesRequiredSemanticColors() {
        assertEquals(new Color(0x25, 0x63, 0xEB), AppTheme.PRIMARY, "primary blue");
        assertEquals(new Color(0x16, 0xA3, 0x4A), AppTheme.SUCCESS, "normal green");
        assertEquals(new Color(0xF5, 0x9E, 0x0B), AppTheme.WARNING, "pending orange");
        assertEquals(new Color(0xDC, 0x26, 0x26), AppTheme.DANGER, "alert red");
        assertNotEquals(AppTheme.DANGER, AppTheme.QUERY_FAILED, "query failure must have its own color");
        assertNotEquals(AppTheme.PRIMARY, AppTheme.RECOVERED, "recovery must have its own color");
        assertNotEquals(AppTheme.SUCCESS, AppTheme.MUTED, "disabled state must be gray");
    }

    private static void usesChineseCapableSystemFontWithFallback() {
        Font font = AppTheme.font(Font.PLAIN, 14f);
        boolean supported = "Microsoft YaHei".equalsIgnoreCase(font.getFamily())
                || Font.SANS_SERIF.equalsIgnoreCase(font.getFamily())
                || "SansSerif".equalsIgnoreCase(font.getFamily());
        assertTrue(supported, "font must prefer Microsoft YaHei and otherwise use SansSerif: " + font);
    }

    private static void configuresTablesConsistently() {
        JTable table = new JTable(1, 1);
        UiFactory.configureTable(table);

        assertEquals(32, table.getRowHeight(), "standard table row height");
        assertEquals(AppTheme.PRIMARY_LIGHT, table.getSelectionBackground(), "table selection background");
        assertEquals(AppTheme.TEXT_PRIMARY, table.getForeground(), "table foreground");
        assertTrue(table.getTableHeader().isOpaque(), "table header must be opaque");
        assertFalse(table.getShowVerticalLines(), "tables should not show dense vertical grid lines");
    }

    private static void createsDistinctButtonRoles() {
        JButton primary = UiFactory.primaryButton("保存");
        JButton secondary = UiFactory.secondaryButton("取消");
        JButton danger = UiFactory.dangerButton("删除");

        assertEquals(AppTheme.PRIMARY, primary.getBackground(), "primary button background");
        assertEquals(Color.WHITE, primary.getForeground(), "primary button text");
        assertEquals(AppTheme.CARD_BACKGROUND, secondary.getBackground(), "secondary button background");
        assertEquals(AppTheme.DANGER, danger.getBackground(), "danger button background");
        assertNotEquals(primary.getBackground(), danger.getBackground(), "button roles must not be color-identical");
    }

    private static void reusableCardsAndBadgeExposeTextAndSemanticColor() {
        JLabel value = new JLabel("12");
        MetricCard metric = new MetricCard("活跃报警", value, AppTheme.DANGER);
        SectionCard section = new SectionCard("运行状态", "最近一次检测", new JPanel());
        StatusBadge badge = new StatusBadge();
        badge.setStatus("查询失败", AppTheme.QUERY_FAILED);

        assertEquals("活跃报警", metric.title(), "metric title");
        assertEquals(value, metric.valueLabel(), "metric value label");
        assertEquals(AppTheme.DANGER, metric.accentColor(), "metric semantic color");
        assertEquals("运行状态", section.title(), "section title");
        assertEquals("查询失败", badge.getText(), "badge must include Chinese text");
        assertEquals(AppTheme.QUERY_FAILED, badge.statusColor(), "badge must expose semantic color");
        assertTrue(badge.isOpaque(), "badge must paint a visible background");
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
}
