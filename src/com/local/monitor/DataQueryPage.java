package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class DataQueryPage extends JPanel {
    public DataQueryPage(Components components, Actions actions) {
        super(new BorderLayout(14, 14));
        setName("数据查询");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(new SectionCard(
                "结构化只读查询",
                "固定来源 tcs_map_data；条件使用参数化 SELECT 和 COUNT",
                buildConditions(components, actions)), BorderLayout.NORTH);

        UiFactory.configureTable(components.resultTable());
        if (components.resultTable().getColumnCount() > 0) {
            UiFactory.configureStatusColumn(
                    components.resultTable(), components.resultTable().getColumnCount() - 1);
        }
        components.resultTable().getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                actions.selectionChanged().run();
            }
        });
        components.detailArea().setEditable(false);
        components.detailArea().setLineWrap(true);
        components.detailArea().setWrapStyleWord(true);
        components.detailArea().setFont(AppTheme.font(Font.PLAIN, 13f));
        components.detailArea().setText("只读查询\n不支持 SQL 编辑\n不支持数据修改");

        SectionCard tableCard = new SectionCard(
                "查询结果",
                "仅显示当前页；可导出当前内存结果",
                UiFactory.tableScrollPane(components.resultTable()));
        SectionCard detailCard = new SectionCard(
                "记录详情与安全边界",
                "无自由表名、字段名、排序表达式或 SQL 编辑器",
                new JScrollPane(components.detailArea()));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableCard, detailCard);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerLocation(820);
        splitPane.setResizeWeight(0.75);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    private static JPanel buildConditions(Components components, Actions actions) {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, 0, 0, "地码关键字", components.pointKeyword());
        addField(form, 0, 2, "货码关键字", components.shelfKeyword());
        addField(form, 0, 4, "区域编码", components.areaCode());
        addField(form, 1, 0, "关联区域编码", components.relatedAreaCode());
        addField(form, 1, 2, "更新时间起", components.updatedFrom());
        addField(form, 1, 4, "更新时间止", components.updatedTo());
        addField(form, 2, 0, "每页行数", components.pageSize());
        container.add(form, BorderLayout.CENTER);

        JButton execute = action(UiFactory.primaryButton("执行只读查询"), actions.executeFirstPage());
        JButton export = action(UiFactory.secondaryButton("导出当前结果"), actions.exportCurrentPage());
        styleExistingButton(components.previousButton());
        styleExistingButton(components.nextButton());
        components.previousButton().addActionListener(event -> actions.previousPage().run());
        components.nextButton().addActionListener(event -> actions.nextPage().run());
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pagination.setOpaque(false);
        pagination.add(execute);
        pagination.add(components.previousButton());
        pagination.add(components.nextButton());
        pagination.add(components.pageLabel());
        pagination.add(components.totalLabel());
        pagination.add(export);
        container.add(pagination, BorderLayout.SOUTH);

        DocumentListener resetListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                actions.conditionsChanged().run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                actions.conditionsChanged().run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                actions.conditionsChanged().run();
            }
        };
        for (JTextField field : List.of(
                components.pointKeyword(), components.shelfKeyword(), components.areaCode(),
                components.relatedAreaCode(), components.updatedFrom(), components.updatedTo())) {
            field.getDocument().addDocumentListener(resetListener);
        }
        components.pageSize().addChangeListener(event -> actions.conditionsChanged().run());
        return container;
    }

    private static void addField(JPanel panel, int row, int column, String text, Component field) {
        GridBagConstraints label = constraints(column, row);
        label.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(text + "："), label);
        GridBagConstraints input = constraints(column + 1, row);
        input.weightx = 1.0;
        input.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, input);
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(4, 5, 4, 5);
        return constraints;
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }

    private static void styleExistingButton(JButton button) {
        button.setFont(AppTheme.font(Font.BOLD, 13f));
        button.setBackground(AppTheme.CARD_BACKGROUND);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        button.setFocusPainted(false);
    }

    public record Components(
            JTextField pointKeyword,
            JTextField shelfKeyword,
            JTextField areaCode,
            JTextField relatedAreaCode,
            JTextField updatedFrom,
            JTextField updatedTo,
            JSpinner pageSize,
            JButton previousButton,
            JButton nextButton,
            JLabel pageLabel,
            JLabel totalLabel,
            JTable resultTable,
            JTextArea detailArea) {
    }

    public record Actions(
            Runnable executeFirstPage,
            Runnable previousPage,
            Runnable nextPage,
            Runnable exportCurrentPage,
            Runnable selectionChanged,
            Runnable conditionsChanged) {
    }
}
