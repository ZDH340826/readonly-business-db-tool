package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

public final class OverviewPage extends JPanel {
    public static final String QUERY_FAILURE_TEXT = "查询失败，数据不可用";

    public OverviewPage(
            JLabel groupCount,
            JLabel alertCount,
            JLabel pendingCount,
            JLabel dataErrorCount,
            JTable statusTable,
            JTextArea detailArea,
            Runnable selectionChanged,
            Runnable startMonitoring,
            Runnable stopMonitoring,
            Runnable checkNow,
            Runnable showAlerts,
            Runnable acknowledgeAlert) {
        super(new BorderLayout(14, 14));
        setName("监控总览");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(new MetricCard("监控点位组", groupCount, AppTheme.PRIMARY));
        metrics.add(new MetricCard("缺料报警", alertCount, AppTheme.DANGER));
        metrics.add(new MetricCard("观察中", pendingCount, AppTheme.WARNING));
        metrics.add(new MetricCard("数据异常", dataErrorCount, AppTheme.QUERY_FAILED));
        add(metrics, BorderLayout.NORTH);

        UiFactory.configureTable(statusTable);
        UiFactory.configureStatusColumn(statusTable, 0);
        statusTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectionChanged.run();
            }
        });

        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(AppTheme.font(java.awt.Font.PLAIN, 13f));
        detailArea.setForeground(AppTheme.TEXT_PRIMARY);
        detailArea.setBackground(AppTheme.CARD_BACKGROUND);
        detailArea.setText("请选择左侧点位组查看详情。");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(action(UiFactory.primaryButton("开始监控"), startMonitoring));
        actions.add(action(UiFactory.dangerButton("停止监控"), stopMonitoring));
        actions.add(action(UiFactory.primaryButton("立即检测"), checkNow));
        actions.add(action(UiFactory.secondaryButton("查看报警"), showAlerts));
        actions.add(action(UiFactory.secondaryButton("标记已关注"), acknowledgeAlert));

        JPanel detailContent = new JPanel(new BorderLayout(0, 12));
        detailContent.setOpaque(false);
        detailContent.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        detailContent.add(actions, BorderLayout.SOUTH);

        SectionCard tableCard = new SectionCard(
                "点位组运行状态",
                "状态、区域、使用位与备用位数据均来自最近一次真实检测",
                UiFactory.tableScrollPane(statusTable));
        SectionCard detailCard = new SectionCard(
                "点位组详情",
                "规则摘要、持续时间、检测时间与点位明细",
                detailContent);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableCard, detailCard);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerLocation(760);
        splitPane.setResizeWeight(0.68);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }
}
