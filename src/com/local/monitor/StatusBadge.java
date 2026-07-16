package com.local.monitor;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

public final class StatusBadge extends JLabel {
    private Color statusColor = AppTheme.MUTED;

    public StatusBadge() {
        setFont(AppTheme.font(Font.BOLD, 12f));
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        setOpaque(true);
        setStatus("未知", AppTheme.MUTED);
    }

    public void setStatus(String text, Color color) {
        statusColor = color == null ? AppTheme.MUTED : color;
        setText(text == null || text.isBlank() ? "未知" : text);
        setForeground(statusColor);
        setBackground(AppTheme.softBackground(statusColor));
        setToolTipText(getText());
    }

    public Color statusColor() {
        return statusColor;
    }
}
