package com.local.monitor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;

public class SectionCard extends JPanel {
    private final String title;

    public SectionCard(String title, String subtitle, JComponent content) {
        super(new BorderLayout(0, 14));
        this.title = title == null ? "" : title;
        setName(this.title);
        setOpaque(true);
        setBackground(AppTheme.CARD_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(AppTheme.BORDER, 12),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(this.title);
        titleLabel.setFont(AppTheme.font(Font.BOLD, 16f));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            heading.add(Box.createVerticalStrut(4));
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(AppTheme.font(Font.PLAIN, 12f));
            subtitleLabel.setForeground(AppTheme.TEXT_SECONDARY);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            heading.add(subtitleLabel);
        }
        add(heading, BorderLayout.NORTH);
        if (content != null) {
            add(content, BorderLayout.CENTER);
        }
    }

    public String title() {
        return title;
    }

    private static final class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        private RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D copy = (Graphics2D) graphics.create();
            try {
                copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                copy.setColor(color);
                copy.setStroke(new BasicStroke(1f));
                copy.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            } finally {
                copy.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }
    }
}
