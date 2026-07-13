package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

public final class SystemSettingsPage extends JPanel {
    public SystemSettingsPage(Components components, Actions actions) {
        super(new BorderLayout(14, 14));
        setName("系统设置");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel settingsForm = new JPanel(new GridBagLayout());
        settingsForm.setOpaque(false);
        addField(settingsForm, 0, "默认首页", components.defaultPage());
        addField(settingsForm, 1, "监控总览自动刷新间隔(秒)", components.overviewRefreshSeconds());
        addCheckBox(settingsForm, 2, components.alertPopup());
        addCheckBox(settingsForm, 3, components.alertSound());
        addField(settingsForm, 4, "日志保留天数", components.logRetentionDays());
        addField(settingsForm, 5, "界面显示密度", components.density());
        addCheckBox(settingsForm, 6, components.startupSelfTest());
        addCheckBox(settingsForm, 7, components.autoCleanupLogs());

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(action(UiFactory.primaryButton("保存设置"), actions.save()));
        actionsPanel.add(action(UiFactory.secondaryButton("恢复默认"), actions.restoreDefaults()));
        actionsPanel.add(action(UiFactory.secondaryButton("重新加载配置"), actions.reload()));
        JPanel settingsContent = new JPanel(new BorderLayout(0, 12));
        settingsContent.setOpaque(false);
        settingsContent.add(settingsForm, BorderLayout.CENTER);
        settingsContent.add(actionsPanel, BorderLayout.SOUTH);
        add(new SectionCard("有效设置", "以下八项均会真实影响程序行为", settingsContent), BorderLayout.CENTER);
        add(new SectionCard("固定安全边界", "不可关闭或绕过", buildSafetyStatements()), BorderLayout.EAST);
    }

    private static JPanel buildSafetyStatements() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 8, 8));
        panel.setOpaque(false);
        panel.add(badge("敏感信息脱敏：强制启用", AppTheme.SUCCESS));
        panel.add(badge("数据库访问模式：只读", AppTheme.SUCCESS));
        panel.add(badge("SQL 编辑能力：未提供", AppTheme.PRIMARY));
        panel.add(badge("密码持久化：禁止", AppTheme.QUERY_FAILED));
        return panel;
    }

    private static StatusBadge badge(String text, java.awt.Color color) {
        StatusBadge badge = new StatusBadge();
        badge.setStatus(text, color);
        return badge;
    }

    private static void addField(JPanel panel, int row, String labelText, Component field) {
        GridBagConstraints label = constraints(0, row);
        label.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel(labelText + "："), label);
        GridBagConstraints input = constraints(1, row);
        input.weightx = 1.0;
        input.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, input);
    }

    private static void addCheckBox(JPanel panel, int row, JCheckBox checkBox) {
        GridBagConstraints constraints = constraints(0, row);
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        panel.add(checkBox, constraints);
    }

    private static GridBagConstraints constraints(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(6, 6, 6, 6);
        return constraints;
    }

    private static JButton action(JButton button, Runnable callback) {
        button.addActionListener(event -> callback.run());
        return button;
    }

    public record Components(
            JComboBox<String> defaultPage,
            JSpinner overviewRefreshSeconds,
            JCheckBox alertPopup,
            JCheckBox alertSound,
            JSpinner logRetentionDays,
            JComboBox<String> density,
            JCheckBox startupSelfTest,
            JCheckBox autoCleanupLogs) {
    }

    public record Actions(Runnable save, Runnable restoreDefaults, Runnable reload) {
    }
}
