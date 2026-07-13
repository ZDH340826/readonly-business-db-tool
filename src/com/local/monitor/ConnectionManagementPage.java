package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public final class ConnectionManagementPage extends JPanel {
    public ConnectionManagementPage(Components components, Actions actions) {
        super(new BorderLayout(14, 14));
        setName("连接管理");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        components.profileList().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        components.profileList().setFixedCellHeight(36);
        components.profileList().setFont(AppTheme.font(Font.PLAIN, 13f));
        components.profileList().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                actions.selectionChanged().run();
            }
        });
        components.databaseType().addActionListener(event -> actions.databaseTypeChanged().run());

        SectionCard listCard = new SectionCard(
                "连接列表",
                "仅保存非敏感连接元数据",
                new JScrollPane(components.profileList()));
        listCard.setMinimumSize(new Dimension(210, 240));
        SectionCard formCard = new SectionCard(
                "连接配置",
                "密码只保存在当前进程内存中",
                buildForm(components, actions));
        SectionCard safetyCard = new SectionCard(
                "连接测试与安全说明",
                "测试结果经过脱敏后显示",
                buildSafetyPanel(components.testResult()));

        JSplitPane formAndSafety = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formCard, safetyCard);
        formAndSafety.setBorder(BorderFactory.createEmptyBorder());
        formAndSafety.setResizeWeight(0.67);
        formAndSafety.setDividerLocation(690);
        JSplitPane workspace = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listCard, formAndSafety);
        workspace.setBorder(BorderFactory.createEmptyBorder());
        workspace.setResizeWeight(0.18);
        workspace.setDividerLocation(230);
        workspace.setContinuousLayout(true);
        add(workspace, BorderLayout.CENTER);
    }

    private static JPanel buildForm(Components components, Actions actions) {
        JPanel container = new JPanel(new BorderLayout(0, 14));
        container.setOpaque(false);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        addField(form, row++, "连接ID", components.profileId());
        addField(form, row++, "连接名称", components.profileName());
        addField(form, row++, "数据库类型", components.databaseType());
        addField(form, row++, "服务器地址/IP", components.host());
        addField(form, row++, "端口", components.port());
        addField(form, row++, "数据库名", components.database());
        addField(form, row++, "数据库空间/Schema", components.schema());
        addField(form, row++, "用户名", components.user());
        addField(form, row++, "SSL模式", components.sslMode());
        addField(form, row++, "本地测试库路径", components.localPath());
        addField(form, row++, "密码", components.password());
        GridBagConstraints filler = constraints(0, row);
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.BOTH;
        form.add(new JPanel(), filler);
        container.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(action(UiFactory.secondaryButton("新建连接"), actions.newProfile()));
        buttons.add(action(UiFactory.primaryButton("保存连接"), actions.saveProfile()));
        buttons.add(action(UiFactory.dangerButton("删除连接"), actions.deleteProfile()));
        buttons.add(action(UiFactory.primaryButton("测试连接"), actions.testConnection()));
        buttons.add(action(UiFactory.secondaryButton("设为当前连接"), actions.useProfile()));
        container.add(buttons, BorderLayout.SOUTH);
        return container;
    }

    private static JPanel buildSafetyPanel(JLabel testResult) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        testResult.setFont(AppTheme.font(Font.BOLD, 14f));
        testResult.setForeground(AppTheme.TEXT_PRIMARY);
        addWide(panel, 0, testResult);
        addWide(panel, 1, safetyLabel("数据库访问模式：只读", AppTheme.SUCCESS));
        addWide(panel, 2, safetyLabel("密码仅保存在当前运行内存，不写入配置文件", AppTheme.PRIMARY));
        addWide(panel, 3, safetyLabel("现场必须使用数据库只读账号", AppTheme.WARNING));
        addWide(panel, 4, safetyLabel("不显示完整 JDBC URL、密码或历史密码", AppTheme.QUERY_FAILED));
        GridBagConstraints filler = constraints(0, 5);
        filler.weightx = 1.0;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), filler);
        return panel;
    }

    private static JLabel safetyLabel(String text, java.awt.Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(AppTheme.font(Font.PLAIN, 13f));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.softBackground(color)),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        return label;
    }

    private static void addField(JPanel form, int row, String labelText, Component field) {
        GridBagConstraints label = constraints(0, row);
        label.anchor = GridBagConstraints.LINE_END;
        form.add(new JLabel(labelText + "："), label);
        GridBagConstraints input = constraints(1, row);
        input.weightx = 1.0;
        input.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, input);
    }

    private static void addWide(JPanel panel, int row, Component component) {
        GridBagConstraints constraints = constraints(0, row);
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, constraints);
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(5, 5, 5, 5);
        return constraints;
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }

    public record Components(
            JList<String> profileList,
            JTextField profileId,
            JTextField profileName,
            JComboBox<String> databaseType,
            JTextField host,
            JSpinner port,
            JTextField database,
            JTextField schema,
            JTextField user,
            JComboBox<String> sslMode,
            JTextField localPath,
            JPasswordField password,
            JLabel testResult) {
    }

    public record Actions(
            Runnable selectionChanged,
            Runnable databaseTypeChanged,
            Runnable newProfile,
            Runnable saveProfile,
            Runnable deleteProfile,
            Runnable testConnection,
            Runnable useProfile) {
    }
}
