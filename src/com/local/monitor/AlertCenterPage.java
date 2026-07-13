package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

public final class AlertCenterPage extends JPanel {
    public static final List<String> FILTERS = List.of("活跃报警", "已关注", "观察中", "查询失败", "已恢复");

    public AlertCenterPage(
            JComboBox<String> filterBox,
            JTable alertTable,
            JTextArea detailArea,
            Runnable selectionChanged,
            Runnable filterChanged,
            Runnable refreshAlerts,
            Runnable acknowledgeAlert,
            Runnable showGroupDetails,
            Runnable checkGroup,
            Runnable showConnectionStatus) {
        super(new BorderLayout(14, 14));
        setName("报警中心");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filters.setOpaque(false);
        ButtonGroup filterGroup = new ButtonGroup();
        for (String filter : FILTERS) {
            JToggleButton toggle = filterToggle(filter);
            toggle.setSelected(filter.equals(String.valueOf(filterBox.getSelectedItem())));
            toggle.addActionListener(event -> {
                if (filter.equals(String.valueOf(filterBox.getSelectedItem()))) {
                    filterChanged.run();
                } else {
                    filterBox.setSelectedItem(filter);
                }
            });
            filterGroup.add(toggle);
            filters.add(toggle);
        }
        filterBox.setName("报警筛选");
        filterBox.addActionListener(event -> filterChanged.run());
        filters.add(filterBox);
        header.add(filters, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(action(UiFactory.secondaryButton("刷新报警"), refreshAlerts));
        actions.add(action(UiFactory.primaryButton("标记已关注"), acknowledgeAlert));
        actions.add(action(UiFactory.secondaryButton("查看点位详情"), showGroupDetails));
        actions.add(action(UiFactory.primaryButton("立即检测该组"), checkGroup));
        actions.add(action(UiFactory.secondaryButton("查看连接状态"), showConnectionStatus));
        header.add(actions, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        UiFactory.configureTable(alertTable);
        UiFactory.configureStatusColumn(alertTable, Math.max(0, alertTable.getColumnCount() - 1));
        alertTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectionChanged.run();
            }
        });
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(AppTheme.font(Font.PLAIN, 13f));
        detailArea.setForeground(AppTheme.TEXT_PRIMARY);
        detailArea.setBackground(AppTheme.CARD_BACKGROUND);
        detailArea.setText("当前无报警事件。查询失败属于系统数据异常，不属于缺料报警。");

        SectionCard tableCard = new SectionCard(
                "报警事件",
                "操作目标始终使用 groupId，不通过区域名或组名反查",
                new JScrollPane(alertTable));
        SectionCard detailCard = new SectionCard(
                "事件详情",
                "查询失败与业务缺料、业务恢复分别呈现",
                new JScrollPane(detailArea));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableCard, detailCard);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerLocation(780);
        splitPane.setResizeWeight(0.70);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    private static JToggleButton filterToggle(String text) {
        JToggleButton toggle = new JToggleButton(text);
        Color color = switch (text) {
            case "活跃报警" -> AppTheme.DANGER;
            case "观察中" -> AppTheme.WARNING;
            case "查询失败" -> AppTheme.QUERY_FAILED;
            case "已恢复" -> AppTheme.RECOVERED;
            default -> AppTheme.ACKNOWLEDGED;
        };
        toggle.setFont(AppTheme.font(Font.BOLD, 12f));
        toggle.setForeground(color);
        toggle.setBackground(AppTheme.softBackground(color));
        toggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        toggle.setFocusPainted(false);
        toggle.setOpaque(true);
        return toggle;
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }
}
