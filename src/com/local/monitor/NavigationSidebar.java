package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public final class NavigationSidebar extends JPanel {
    public NavigationSidebar(JList<String> navigationList) {
        super(new BorderLayout());
        setName("主导航");
        setBackground(AppTheme.NAVIGATION_BACKGROUND);
        setPreferredSize(new Dimension(AppTheme.NAVIGATION_WIDTH, 100));
        setMinimumSize(new Dimension(AppTheme.NAVIGATION_WIDTH, 100));

        JPanel brand = new JPanel(new BorderLayout(0, 3));
        brand.setOpaque(false);
        brand.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 14));
        JLabel title = new JLabel("只读监控中心");
        title.setForeground(Color.WHITE);
        title.setFont(AppTheme.font(Font.BOLD, 17f));
        JLabel subtitle = new JLabel("FIELD OPERATIONS");
        subtitle.setForeground(new Color(0x94, 0xA3, 0xB8));
        subtitle.setFont(AppTheme.font(Font.BOLD, 10f));
        brand.add(title, BorderLayout.CENTER);
        brand.add(subtitle, BorderLayout.SOUTH);
        add(brand, BorderLayout.NORTH);

        navigationList.setName("页面导航");
        navigationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navigationList.setFixedCellHeight(46);
        navigationList.setBackground(AppTheme.NAVIGATION_BACKGROUND);
        navigationList.setForeground(new Color(0xCB, 0xD5, 0xE1));
        navigationList.setSelectionBackground(AppTheme.PRIMARY);
        navigationList.setSelectionForeground(Color.WHITE);
        navigationList.setBorder(BorderFactory.createEmptyBorder(4, 10, 12, 10));
        navigationList.setCellRenderer(new NavigationRenderer());

        JScrollPane scrollPane = new JScrollPane(navigationList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(AppTheme.NAVIGATION_BACKGROUND);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private static final class NavigationRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setText("  " + String.valueOf(value));
            label.setFont(AppTheme.font(isSelected ? Font.BOLD : Font.PLAIN, 14f));
            label.setForeground(isSelected ? Color.WHITE : new Color(0xCB, 0xD5, 0xE1));
            label.setBackground(isSelected ? AppTheme.PRIMARY : AppTheme.NAVIGATION_BACKGROUND);
            label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return label;
        }
    }
}
