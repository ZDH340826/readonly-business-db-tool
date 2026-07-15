package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;

public final class DataSourceBrowserPage extends JPanel {
    public DataSourceBrowserPage(Components components, Actions actions) {
        super(new BorderLayout(14, 14));
        setName("数据源浏览器");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(buildHeader(components, actions), BorderLayout.NORTH);
        add(buildBrowserColumns(components, actions), BorderLayout.CENTER);
    }

    private static JPanel buildHeader(Components components, Actions actions) {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(new MetricCard("Schema", components.schemaCount(), AppTheme.PRIMARY));
        metrics.add(new MetricCard("表 / 视图", components.objectCount(), AppTheme.PRIMARY_DARK));
        metrics.add(new MetricCard("对象类型", components.objectType(), AppTheme.WARNING));
        metrics.add(new MetricCard("访问模式", components.browserMode(), AppTheme.SUCCESS));
        header.add(metrics, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Schema："));
        controls.add(components.schemaBox());
        controls.add(action(UiFactory.secondaryButton("刷新 Schema"), actions.refreshSchemas()));
        controls.add(action(UiFactory.secondaryButton("加载表/视图"), actions.loadObjects()));
        controls.add(action(UiFactory.primaryButton("预览前100行"), actions.previewSelectedObject()));
        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    private static JSplitPane buildBrowserColumns(Components components, Actions actions) {
        for (JTable table : java.util.List.of(
                components.objectTable(), components.columnTable(), components.previewTable())) {
            UiFactory.configureTable(table);
        }
        components.objectTable().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                actions.objectSelectionChanged().run();
            }
        });

        JSplitPane treeAndObjects = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(components.objectTree()),
                UiFactory.tableScrollPane(components.objectTable()));
        treeAndObjects.setBorder(BorderFactory.createEmptyBorder());
        treeAndObjects.setResizeWeight(0.45);
        treeAndObjects.setDividerLocation(220);
        SectionCard left = new SectionCard(
                "Schema / 表 / 视图对象树",
                "对象来自 JDBC 元数据读取",
                treeAndObjects);
        SectionCard center = new SectionCard(
                "对象元数据与列信息",
                "列名、类型、长度、可空性和默认值",
                UiFactory.tableScrollPane(components.columnTable()));
        SectionCard right = new SectionCard(
                "前 100 行只读预览",
                "固定上限 100；无写入、删除、更新或 DDL",
                UiFactory.tableScrollPane(components.previewTable()));

        JSplitPane centerAndRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, center, right);
        centerAndRight.setBorder(BorderFactory.createEmptyBorder());
        centerAndRight.setResizeWeight(0.50);
        centerAndRight.setDividerLocation(470);
        JSplitPane all = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, centerAndRight);
        all.setBorder(BorderFactory.createEmptyBorder());
        all.setResizeWeight(0.28);
        all.setDividerLocation(340);
        all.setContinuousLayout(true);
        return all;
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }

    public record Components(
            JLabel schemaCount,
            JLabel objectCount,
            JLabel objectType,
            JLabel browserMode,
            JComboBox<String> schemaBox,
            JTree objectTree,
            JTable objectTable,
            JTable columnTable,
            JTable previewTable) {
    }

    public record Actions(
            Runnable refreshSchemas,
            Runnable loadObjects,
            Runnable objectSelectionChanged,
            Runnable previewSelectedObject) {
    }
}
