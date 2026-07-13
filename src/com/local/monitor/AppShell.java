package com.local.monitor;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;

public final class AppShell extends JPanel {
    public AppShell(
            JList<String> navigationList,
            JPanel pageContainer,
            JComponent topStatusBar,
            JComponent bottomStatusBar) {
        super(new BorderLayout());
        setName("应用外壳");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setPreferredSize(AppTheme.PREFERRED_WINDOW_SIZE);
        setMinimumSize(AppTheme.MINIMUM_WINDOW_SIZE);
        add(topStatusBar, BorderLayout.NORTH);
        add(new NavigationSidebar(navigationList), BorderLayout.WEST);
        add(pageContainer, BorderLayout.CENTER);
        add(bottomStatusBar, BorderLayout.SOUTH);
    }
}
