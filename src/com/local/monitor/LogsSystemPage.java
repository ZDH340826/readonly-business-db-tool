package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public final class LogsSystemPage extends JPanel {
    public LogsSystemPage(Components components, Actions actions) {
        super(new BorderLayout(14, 14));
        setName("日志与系统");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16),
                "日志与系统"));
        add(buildHeader(components, actions), BorderLayout.NORTH);
        add(buildLogWorkspace(components, actions), BorderLayout.CENTER);
    }

    private static JPanel buildHeader(Components components, Actions actions) {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 6, 10, 0));
        metrics.setOpaque(false);
        metrics.add(new MetricCard("监控调度器", components.schedulerHealth(), AppTheme.PRIMARY));
        metrics.add(new MetricCard("当前连接", components.connectionHealth(), AppTheme.PRIMARY_DARK));
        metrics.add(new MetricCard("最近一次检测", components.detectionHealth(), AppTheme.WARNING));
        metrics.add(new MetricCard("配置文件", components.configHealth(), AppTheme.SUCCESS));
        metrics.add(new MetricCard("日志目录", components.logDirectoryHealth(), AppTheme.RECOVERED));
        metrics.add(new MetricCard("自检状态", components.selfTestHealth(), AppTheme.ACKNOWLEDGED));
        header.add(metrics, BorderLayout.CENTER);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        filters.setOpaque(false);
        addFilter(filters, "事件类型", components.typeFilter());
        addFilter(filters, "时间起", components.from());
        addFilter(filters, "时间止", components.to());
        addFilter(filters, "点位组", components.group());
        addFilter(filters, "关键字", components.keyword());
        filters.add(action(UiFactory.secondaryButton("刷新日志"), actions.refresh()));
        filters.add(action(UiFactory.secondaryButton("打开日志目录"), actions.openLogs()));
        filters.add(action(UiFactory.primaryButton("执行自检"), actions.runSelfTest()));
        filters.add(action(UiFactory.primaryButton("导出诊断包"), actions.exportDiagnostics()));
        components.typeFilter().addActionListener(event -> actions.filterChanged().run());
        for (JTextField field : java.util.List.of(
                components.from(), components.to(), components.group(), components.keyword())) {
            field.addActionListener(event -> actions.filterChanged().run());
        }
        header.add(filters, BorderLayout.SOUTH);
        return header;
    }

    private static Component buildLogWorkspace(Components components, Actions actions) {
        UiFactory.configureTable(components.logTable());
        components.logTable().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                actions.selectionChanged().run();
            }
        });
        configureReadOnlyText(components.detailArea());
        configureReadOnlyText(components.runtimeArea());

        JPanel limits = new JPanel(new GridLayout(1, 3, 8, 0));
        limits.setOpaque(false);
        limits.add(new JLabel("event-log.csv：最近 1000 条"));
        limits.add(new JLabel("check-log.csv：最近 1000 条"));
        limits.add(new JLabel("monitor.log：最近 200 行"));
        JPanel tableContent = new JPanel(new BorderLayout(0, 8));
        tableContent.setOpaque(false);
        tableContent.add(limits, BorderLayout.NORTH);
        tableContent.add(UiFactory.tableScrollPane(components.logTable()), BorderLayout.CENTER);
        SectionCard tableCard = new SectionCard("日志筛选结果", "所有显示内容均经过脱敏", tableContent);
        SectionCard detailCard = new SectionCard("事件详情", "选中记录的字段明细", new JScrollPane(components.detailArea()));
        JSplitPane eventSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableCard, detailCard);
        eventSplit.setBorder(BorderFactory.createEmptyBorder());
        eventSplit.setResizeWeight(0.72);
        eventSplit.setDividerLocation(820);
        SectionCard runtimeCard = new SectionCard(
                "当前运行日志",
                "仅显示最近 200 行运行日志",
                new JScrollPane(components.runtimeArea()));
        JSplitPane workspace = new JSplitPane(JSplitPane.VERTICAL_SPLIT, eventSplit, runtimeCard);
        workspace.setBorder(BorderFactory.createEmptyBorder());
        workspace.setResizeWeight(0.72);
        workspace.setDividerLocation(500);
        workspace.setContinuousLayout(true);
        return workspace;
    }

    private static void addFilter(JPanel panel, String label, Component field) {
        panel.add(new JLabel(label + "："));
        panel.add(field);
    }

    private static void configureReadOnlyText(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(AppTheme.font(Font.PLAIN, 12f));
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }

    public record Components(
            JLabel schedulerHealth,
            JLabel connectionHealth,
            JLabel detectionHealth,
            JLabel configHealth,
            JLabel logDirectoryHealth,
            JLabel selfTestHealth,
            JComboBox<String> typeFilter,
            JTextField from,
            JTextField to,
            JTextField group,
            JTextField keyword,
            JTable logTable,
            JTextArea detailArea,
            JTextArea runtimeArea) {
    }

    public record Actions(
            Runnable refresh,
            Runnable filterChanged,
            Runnable openLogs,
            Runnable runSelfTest,
            Runnable exportDiagnostics,
            Runnable selectionChanged) {
        public Actions(
                Runnable refresh,
                Runnable filterChanged,
                Runnable openLogs,
                Runnable runSelfTest,
                Runnable exportDiagnostics) {
            this(refresh, filterChanged, openLogs, runSelfTest, exportDiagnostics, () -> { });
        }
    }
}
