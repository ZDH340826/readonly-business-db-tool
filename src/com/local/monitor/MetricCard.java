package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class MetricCard extends SectionCard {
    private final JLabel valueLabel;
    private final Color accentColor;

    public MetricCard(String title, JLabel valueLabel, Color accentColor) {
        super(title, "", metricContent(valueLabel, accentColor));
        this.valueLabel = valueLabel;
        this.accentColor = accentColor;
        setMinimumSize(new Dimension(180, 112));
    }

    public JLabel valueLabel() {
        return valueLabel;
    }

    public Color accentColor() {
        return accentColor;
    }

    private static JPanel metricContent(JLabel valueLabel, Color accentColor) {
        JPanel content = new JPanel(new BorderLayout(12, 0));
        content.setOpaque(false);
        JPanel accent = new JPanel();
        accent.setBackground(accentColor);
        accent.setPreferredSize(new Dimension(4, 48));
        content.add(accent, BorderLayout.WEST);
        valueLabel.setFont(AppTheme.font(Font.BOLD, 28f));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        content.add(valueLabel, BorderLayout.CENTER);
        return content;
    }
}
