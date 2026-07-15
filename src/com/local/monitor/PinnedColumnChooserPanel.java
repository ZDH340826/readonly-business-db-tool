package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

final class PinnedColumnChooserPanel extends JPanel {
    private final List<String> available;
    private final DefaultListModel<String> orderModel = new DefaultListModel<>();
    private final JList<String> orderList = new JList<>(orderModel);
    private final Map<String, JCheckBox> checkBoxes = new LinkedHashMap<>();
    private final JLabel statusLabel = new JLabel(" ");

    PinnedColumnChooserPanel(List<String> available, List<String> selected) {
        super(new BorderLayout(0, 12));
        this.available = available == null ? List.of() : List.copyOf(available);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(680, 430));

        JLabel instruction = new JLabel("勾选需要固定在左侧的列，最多 4 列");
        instruction.setFont(AppTheme.font(Font.BOLD, 14f));
        add(instruction, BorderLayout.NORTH);

        JPanel choices = new JPanel();
        choices.setLayout(new javax.swing.BoxLayout(choices, javax.swing.BoxLayout.Y_AXIS));
        choices.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        for (String column : this.available) {
            JCheckBox checkBox = new JCheckBox(DatabaseFieldLabel.display(column));
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBox.setFont(AppTheme.font(Font.PLAIN, 13f));
            checkBox.addActionListener(event -> updateSelection(column, checkBox.isSelected()));
            checkBoxes.put(normalizedName(column), checkBox);
            choices.add(checkBox);
        }

        orderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderList.setFont(AppTheme.font(Font.PLAIN, 13f));
        JPanel orderPanel = new JPanel(new BorderLayout(0, 8));
        orderPanel.add(new JLabel("固定后的显示顺序"), BorderLayout.NORTH);
        orderPanel.add(new JScrollPane(orderList), BorderLayout.CENTER);
        JPanel moveButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton moveUp = UiFactory.secondaryButton("上移");
        JButton moveDown = UiFactory.secondaryButton("下移");
        moveUp.addActionListener(event -> moveSelected(true));
        moveDown.addActionListener(event -> moveSelected(false));
        moveButtons.add(moveUp);
        moveButtons.add(moveDown);
        orderPanel.add(moveButtons, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(choices),
                orderPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setResizeWeight(0.55d);
        splitPane.setDividerLocation(350);
        add(splitPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        JButton preset = UiFactory.primaryButton("一键固定地码和货码");
        List<String> presetColumns = PinnedColumnLayout.presetFor(this.available);
        preset.setEnabled(!presetColumns.isEmpty());
        preset.addActionListener(event -> setSelectedColumns(presetColumns));
        actions.add(preset, BorderLayout.WEST);
        statusLabel.setForeground(AppTheme.DANGER);
        actions.add(statusLabel, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        setSelectedColumns(PinnedColumnLayout.normalize(this.available, selected));
    }

    List<String> selectedColumns() {
        List<String> selected = new ArrayList<>();
        for (int index = 0; index < orderModel.size(); index++) {
            selected.add(orderModel.get(index));
        }
        return List.copyOf(selected);
    }

    private void updateSelection(String column, boolean selected) {
        List<String> columns = new ArrayList<>(selectedColumns());
        int existing = indexOf(columns, column);
        if (selected && existing < 0) {
            if (columns.size() >= PinnedColumnLayout.MAX_PINNED_COLUMNS) {
                checkBoxes.get(normalizedName(column)).setSelected(false);
                statusLabel.setText("最多固定 4 列，请先取消一列");
                return;
            }
            columns.add(column);
        } else if (!selected && existing >= 0) {
            columns.remove(existing);
        }
        statusLabel.setText(" ");
        setSelectedColumns(columns);
    }

    private void moveSelected(boolean up) {
        int selectedIndex = orderList.getSelectedIndex();
        if (selectedIndex < 0) {
            statusLabel.setText("请先选择要调整顺序的固定列");
            return;
        }
        List<String> moved = up
                ? PinnedColumnLayout.moveUp(selectedColumns(), selectedIndex)
                : PinnedColumnLayout.moveDown(selectedColumns(), selectedIndex);
        int nextIndex = up
                ? Math.max(0, selectedIndex - 1)
                : Math.min(moved.size() - 1, selectedIndex + 1);
        setSelectedColumns(moved);
        orderList.setSelectedIndex(nextIndex);
    }

    private void setSelectedColumns(List<String> selected) {
        List<String> normalized = PinnedColumnLayout.normalize(available, selected);
        orderModel.clear();
        for (String column : normalized) {
            orderModel.addElement(column);
        }
        for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
            entry.getValue().setSelected(indexOf(normalized, entry.getKey()) >= 0);
        }
        statusLabel.setText(" ");
    }

    private static int indexOf(List<String> columns, String target) {
        String normalizedTarget = normalizedName(target);
        for (int index = 0; index < columns.size(); index++) {
            if (normalizedName(columns.get(index)).equals(normalizedTarget)) {
                return index;
            }
        }
        return -1;
    }

    private static String normalizedName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
