package com.local.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public final class AppTheme {
    public static final Dimension PREFERRED_WINDOW_SIZE = new Dimension(1440, 900);
    public static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(1180, 760);
    public static final int NAVIGATION_WIDTH = 210;
    public static final int TOP_BAR_HEIGHT = 56;
    public static final int BOTTOM_BAR_HEIGHT = 28;

    public static final Color PAGE_BACKGROUND = new Color(0xF5, 0xF7, 0xFA);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color NAVIGATION_BACKGROUND = new Color(0x0F, 0x1F, 0x36);
    public static final Color NAVIGATION_HOVER = new Color(0x1E, 0x3A, 0x5F);
    public static final Color PRIMARY = new Color(0x25, 0x63, 0xEB);
    public static final Color PRIMARY_DARK = new Color(0x1D, 0x4E, 0xD8);
    public static final Color PRIMARY_LIGHT = new Color(0xDB, 0xE9, 0xFF);
    public static final Color SUCCESS = new Color(0x16, 0xA3, 0x4A);
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);
    public static final Color QUERY_FAILED = new Color(0xBE, 0x12, 0x3C);
    public static final Color ACKNOWLEDGED = new Color(0x64, 0x74, 0x8B);
    public static final Color RECOVERED = new Color(0x0F, 0x76, 0x6E);
    public static final Color MUTED = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);
    public static final Color TEXT_SECONDARY = new Color(0x47, 0x55, 0x69);
    public static final Color BORDER = new Color(0xE2, 0xE8, 0xF0);
    public static final Color INPUT_BACKGROUND = new Color(0xFF, 0xFF, 0xFF);

    private static final String FONT_FAMILY = selectFontFamily();

    private AppTheme() {
    }

    public static void install() {
        FontUIResource body = new FontUIResource(font(Font.PLAIN, 14f));
        FontUIResource compact = new FontUIResource(font(Font.PLAIN, 13f));
        FontUIResource bold = new FontUIResource(font(Font.BOLD, 14f));

        UIManager.put("Button.font", bold);
        UIManager.put("CheckBox.font", body);
        UIManager.put("ComboBox.font", body);
        UIManager.put("Label.font", body);
        UIManager.put("List.font", body);
        UIManager.put("Menu.font", body);
        UIManager.put("MenuItem.font", body);
        UIManager.put("OptionPane.messageFont", body);
        UIManager.put("OptionPane.buttonFont", bold);
        UIManager.put("PasswordField.font", body);
        UIManager.put("RadioButton.font", body);
        UIManager.put("Spinner.font", body);
        UIManager.put("Table.font", compact);
        UIManager.put("TableHeader.font", bold);
        UIManager.put("TextArea.font", body);
        UIManager.put("TextField.font", body);
        UIManager.put("TitledBorder.font", bold);
        UIManager.put("Panel.background", PAGE_BACKGROUND);
        UIManager.put("Viewport.background", CARD_BACKGROUND);
        UIManager.put("Table.background", CARD_BACKGROUND);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", new Color(0xF8, 0xFA, 0xFC));
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("TextField.background", INPUT_BACKGROUND);
        UIManager.put("PasswordField.background", INPUT_BACKGROUND);
        UIManager.put("ComboBox.background", INPUT_BACKGROUND);
        UIManager.put("Button.disabledText", TEXT_SECONDARY);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
    }

    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size)).deriveFont(size);
    }

    public static Color softBackground(Color color) {
        int red = Math.round(color.getRed() * 0.12f + 255 * 0.88f);
        int green = Math.round(color.getGreen() * 0.12f + 255 * 0.88f);
        int blue = Math.round(color.getBlue() * 0.12f + 255 * 0.88f);
        return new Color(red, green, blue);
    }

    private static String selectFontFamily() {
        String preferred = "Microsoft YaHei";
        boolean available = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames())
                .anyMatch(preferred::equalsIgnoreCase);
        return available ? preferred : Font.SANS_SERIF;
    }
}
