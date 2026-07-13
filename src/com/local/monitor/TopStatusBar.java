package com.local.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class TopStatusBar extends JPanel {
    private final List<JLabel> statusLabels;

    public TopStatusBar(JLabel connection, JLabel monitoring, JLabel lastCheck, JLabel nextCheck) {
        super(new GridLayout(1, 4, 12, 0));
        statusLabels = List.of(connection, monitoring, lastCheck, nextCheck);
        setName("顶部运行状态栏");
        setPreferredSize(new Dimension(100, AppTheme.TOP_BAR_HEIGHT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        for (int index = 0; index < statusLabels.size(); index++) {
            JLabel label = statusLabels.get(index);
            label.setFont(AppTheme.font(index == 0 ? Font.BOLD : Font.PLAIN, 13f));
            label.setForeground(index == 0 ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
            add(label);
        }
    }

    public List<String> statusTexts() {
        return statusLabels.stream().map(JLabel::getText).toList();
    }
}
