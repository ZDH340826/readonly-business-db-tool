package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public final class GroupManagementPage extends JPanel {
    private final Components components;
    private final Actions actions;
    private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("点位组");
    private final JTree groupTree = new JTree(treeRoot);
    private boolean synchronizingSelection;

    public GroupManagementPage(Components components, Actions actions) {
        super(new BorderLayout(14, 14));
        this.components = components;
        this.actions = actions;
        setName("点位组管理");
        setBackground(AppTheme.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(buildActionBar(), BorderLayout.NORTH);
        add(buildWorkspace(), BorderLayout.CENTER);
        configureSelectionBridge();
        rebuildTree();
    }

    public JTree groupTree() {
        return groupTree;
    }

    private JPanel buildActionBar() {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(action(UiFactory.primaryButton("新增组"), actions.addGroup()));
        actionsPanel.add(action(UiFactory.dangerButton("删除组"), actions.removeGroup()));
        actionsPanel.add(action(UiFactory.secondaryButton("新增点位"), actions.addPoint()));
        actionsPanel.add(action(UiFactory.secondaryButton("删除点位"), actions.removePoint()));
        actionsPanel.add(action(UiFactory.primaryButton("保存配置"), actions.save()));
        actionsPanel.add(action(UiFactory.secondaryButton("放弃修改"), actions.discard()));
        actionsPanel.add(action(UiFactory.secondaryButton("验证配置"), actions.validate()));
        styleMonitorButton(components.startButton(), AppTheme.PRIMARY, java.awt.Color.WHITE);
        styleMonitorButton(components.stopButton(), AppTheme.DANGER, java.awt.Color.WHITE);
        styleMonitorButton(components.checkButton(), AppTheme.PRIMARY_DARK, java.awt.Color.WHITE);
        components.startButton().addActionListener(event -> actions.startMonitoring().run());
        components.stopButton().addActionListener(event -> actions.stopMonitoring().run());
        components.checkButton().addActionListener(event -> actions.checkNow().run());
        components.stopButton().setEnabled(false);
        actionsPanel.add(components.startButton());
        actionsPanel.add(components.stopButton());
        actionsPanel.add(components.checkButton());
        return actionsPanel;
    }

    private Component buildWorkspace() {
        groupTree.setName("区域点位组树");
        groupTree.setRootVisible(false);
        groupTree.setShowsRootHandles(true);
        groupTree.setRowHeight(28);
        groupTree.setFont(AppTheme.font(Font.PLAIN, 13f));
        SectionCard treeCard = new SectionCard(
                "区域 / 点位组树",
                "按区域浏览；叶节点身份为 groupId",
                new JScrollPane(groupTree));
        treeCard.setMinimumSize(new Dimension(220, 220));

        JSplitPane forms = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new SectionCard("基本信息", "点位组身份与检测周期", buildBasicForm()),
                new SectionCard("报警规则", "缺料判定和持续时间", buildRuleForm()));
        forms.setBorder(BorderFactory.createEmptyBorder());
        forms.setResizeWeight(0.55);
        forms.setDividerLocation(430);

        JSplitPane upper = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeCard, forms);
        upper.setBorder(BorderFactory.createEmptyBorder());
        upper.setResizeWeight(0.20);
        upper.setDividerLocation(240);

        configurePointTable();
        SectionCard pointTableCard = new SectionCard(
                "点位配置表",
                "角色、别名、点位编码和启用状态",
                UiFactory.tableScrollPane(components.pointTable()));
        SectionCard runtimeCard = new SectionCard(
                "点位状态看板",
                "最近一次检测的真实状态与运行摘要",
                buildRuntimePanel());
        JSplitPane lower = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pointTableCard, runtimeCard);
        lower.setBorder(BorderFactory.createEmptyBorder());
        lower.setResizeWeight(0.62);
        lower.setDividerLocation(760);

        JSplitPane workspace = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        workspace.setBorder(BorderFactory.createEmptyBorder());
        workspace.setResizeWeight(0.42);
        workspace.setDividerLocation(300);
        workspace.setContinuousLayout(true);
        return workspace;
    }

    private JPanel buildBasicForm() {
        JPanel form = formPanel();
        addField(form, 0, "组ID", components.groupId());
        addField(form, 1, "区域", components.area());
        addField(form, 2, "组名", components.groupName());
        addField(form, 3, "物料", components.material());
        addField(form, 4, "检测周期(分钟)", components.checkIntervalMinutes());
        addCheckBox(form, 5, components.groupEnabled());
        addVerticalFiller(form, 6);
        return form;
    }

    private JPanel buildRuleForm() {
        JPanel form = formPanel();
        addCheckBox(form, 0, components.ruleEnabled());
        addCheckBox(form, 1, components.requireUseEmpty());
        addCheckBox(form, 2, components.backupThresholdParticipates());
        addField(form, 3, "最少备用位有料", components.minBackupAvailable());
        addField(form, 4, "报警持续(分钟)", components.durationMinutes());
        addVerticalFiller(form, 5);
        return form;
    }

    private JPanel buildRuntimePanel() {
        JPanel runtime = new JPanel(new BorderLayout(0, 8));
        runtime.setOpaque(false);
        components.summaryLabel().setFont(AppTheme.font(Font.BOLD, 14f));
        components.summaryLabel().setForeground(AppTheme.TEXT_PRIMARY);
        components.pointStatusPanel().setBackground(AppTheme.CARD_BACKGROUND);
        components.pointStatusPanel().setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        components.runtimeArea().setEditable(false);
        components.runtimeArea().setRows(4);
        components.runtimeArea().setFont(AppTheme.font(Font.PLAIN, 12f));
        runtime.add(components.summaryLabel(), BorderLayout.NORTH);
        runtime.add(new JScrollPane(components.pointStatusPanel()), BorderLayout.CENTER);
        runtime.add(new JScrollPane(components.runtimeArea()), BorderLayout.SOUTH);
        return runtime;
    }

    private void configurePointTable() {
        JTable table = components.pointTable();
        UiFactory.configureTable(table);
        if (table.getColumnCount() >= 4) {
            table.getColumnModel().getColumn(0).setPreferredWidth(80);
            table.getColumnModel().getColumn(1).setPreferredWidth(160);
            table.getColumnModel().getColumn(2).setPreferredWidth(260);
            table.getColumnModel().getColumn(3).setPreferredWidth(60);
            table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(
                    new JComboBox<>(new String[] {PointRole.USE.name(), PointRole.BACKUP.name()})));
        }
    }

    private void configureSelectionBridge() {
        components.groupList().getModel().addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent event) {
                rebuildTree();
            }

            @Override
            public void intervalRemoved(ListDataEvent event) {
                rebuildTree();
            }

            @Override
            public void contentsChanged(ListDataEvent event) {
                rebuildTree();
            }
        });
        components.groupList().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectTreeForListIndex(components.groupList().getSelectedIndex());
                actions.selectionChanged().run();
            }
        });
        groupTree.addTreeSelectionListener(event -> {
            if (synchronizingSelection) {
                return;
            }
            DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) groupTree.getLastSelectedPathComponent();
            if (selectedNode == null) {
                return;
            }
            Object selected = selectedNode.getUserObject();
            if (selected instanceof GroupNode groupNode) {
                synchronizingSelection = true;
                try {
                    components.groupList().setSelectedIndex(groupNode.listIndex());
                } finally {
                    synchronizingSelection = false;
                }
            }
        });
    }

    private void rebuildTree() {
        int selectedIndex = components.groupList().getSelectedIndex();
        treeRoot.removeAllChildren();
        Map<String, DefaultMutableTreeNode> areas = new LinkedHashMap<>();
        for (int index = 0; index < components.groupList().getModel().getSize(); index++) {
            String display = String.valueOf(components.groupList().getModel().getElementAt(index));
            String area = textBefore(display, " / ", "未分区");
            String groupLabel = textBetween(display, " / ", " [", display);
            String groupId = textBetween(display, "[", "]", groupLabel);
            DefaultMutableTreeNode areaNode = areas.computeIfAbsent(area, key -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
                treeRoot.add(node);
                return node;
            });
            areaNode.add(new DefaultMutableTreeNode(new GroupNode(groupId, groupLabel, index)));
        }
        ((DefaultTreeModel) groupTree.getModel()).reload();
        for (int row = 0; row < groupTree.getRowCount(); row++) {
            groupTree.expandRow(row);
        }
        selectTreeForListIndex(selectedIndex);
    }

    private void selectTreeForListIndex(int listIndex) {
        if (listIndex < 0) {
            groupTree.clearSelection();
            return;
        }
        for (int areaIndex = 0; areaIndex < treeRoot.getChildCount(); areaIndex++) {
            DefaultMutableTreeNode area = (DefaultMutableTreeNode) treeRoot.getChildAt(areaIndex);
            for (int groupIndex = 0; groupIndex < area.getChildCount(); groupIndex++) {
                DefaultMutableTreeNode group = (DefaultMutableTreeNode) area.getChildAt(groupIndex);
                if (group.getUserObject() instanceof GroupNode node && node.listIndex() == listIndex) {
                    synchronizingSelection = true;
                    try {
                        groupTree.setSelectionPath(new TreePath(group.getPath()));
                        groupTree.scrollPathToVisible(new TreePath(group.getPath()));
                    } finally {
                        synchronizingSelection = false;
                    }
                    return;
                }
            }
        }
    }

    private static JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
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

    private static void addCheckBox(JPanel form, int row, JCheckBox checkBox) {
        GridBagConstraints constraints = constraints(0, row);
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        form.add(checkBox, constraints);
    }

    private static void addVerticalFiller(JPanel form, int row) {
        GridBagConstraints filler = constraints(0, row);
        filler.gridwidth = 2;
        filler.weighty = 1.0;
        filler.fill = GridBagConstraints.BOTH;
        form.add(new JPanel(), filler);
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

    private static void styleMonitorButton(JButton button, java.awt.Color background, java.awt.Color foreground) {
        UiFactory.styleActionButton(button, background, foreground, background);
    }

    private static String textBefore(String value, String delimiter, String fallback) {
        int index = value.indexOf(delimiter);
        return index < 0 ? fallback : value.substring(0, index).trim();
    }

    private static String textBetween(String value, String start, String end, String fallback) {
        int startIndex = value.indexOf(start);
        int endIndex = startIndex < 0 ? -1 : value.indexOf(end, startIndex + start.length());
        return startIndex < 0 || endIndex < 0
                ? fallback
                : value.substring(startIndex + start.length(), endIndex).trim();
    }

    private record GroupNode(String groupId, String label, int listIndex) {
        @Override
        public String toString() {
            return label + "  [" + groupId + "]";
        }
    }

    public record Components(
            JList<String> groupList,
            JTextField groupId,
            JTextField area,
            JTextField groupName,
            JTextField material,
            JCheckBox groupEnabled,
            JCheckBox ruleEnabled,
            JCheckBox requireUseEmpty,
            JCheckBox backupThresholdParticipates,
            JSpinner minBackupAvailable,
            JSpinner durationMinutes,
            JSpinner checkIntervalMinutes,
            JTable pointTable,
            JPanel pointStatusPanel,
            JLabel summaryLabel,
            JTextArea runtimeArea,
            JButton startButton,
            JButton stopButton,
            JButton checkButton) {
    }

    public record Actions(
            Runnable selectionChanged,
            Runnable addGroup,
            Runnable removeGroup,
            Runnable addPoint,
            Runnable removePoint,
            Runnable save,
            Runnable discard,
            Runnable validate,
            Runnable startMonitoring,
            Runnable stopMonitoring,
            Runnable checkNow) {
    }
}
