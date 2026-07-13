package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class BottomStatusBar extends JPanel {
    private final String version;

    public BottomStatusBar(JLabel statusLabel, String version) {
        super(new BorderLayout());
        this.version = version;
        setName("底部状态栏");
        setPreferredSize(new Dimension(100, AppTheme.BOTTOM_BAR_HEIGHT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        statusLabel.setFont(AppTheme.font(Font.PLAIN, 12f));
        statusLabel.setForeground(AppTheme.TEXT_SECONDARY);
        JLabel versionLabel = new JLabel("版本：" + version);
        versionLabel.setFont(AppTheme.font(Font.PLAIN, 12f));
        versionLabel.setForeground(AppTheme.MUTED);
        add(statusLabel, BorderLayout.WEST);
        add(versionLabel, BorderLayout.EAST);
    }

    public String version() {
        return version;
    }
}
