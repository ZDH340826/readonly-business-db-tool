package com.local.monitor;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

public final class GroupConnectionPageTest {
    private GroupConnectionPageTest() {
    }

    public static void main(String[] args) throws Exception {
        groupPageUsesAreaTreeThreeColumnFormsAndBottomTable();
        groupPageBindsEveryRealConfigurationAction();
        connectionPageUsesListFormAndTestSafetyColumn();
        connectionPageBindsEveryRealAction();
        System.out.println("GroupConnectionPageTest PASS");
    }

    private static void groupPageUsesAreaTreeThreeColumnFormsAndBottomTable() throws Exception {
        runOnEdtAndWait(() -> {
            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("A区 / 组一 [group-a-1]");
            model.addElement("A区 / 组二 [group-a-2]");
            model.addElement("B区 / 组三 [group-b-1]");
            GroupManagementPage page = new GroupManagementPage(
                    groupComponents(new JList<>(model)), noOpGroupActions());

            List<String> cardTitles = findAll(page, SectionCard.class).stream().map(SectionCard::title).toList();
            assertTrue(cardTitles.contains("区域 / 点位组树"), "group page must have area/group tree");
            assertTrue(cardTitles.contains("基本信息"), "group page must have center basic information");
            assertTrue(cardTitles.contains("报警规则"), "group page must have right alert rules");
            assertTrue(cardTitles.contains("点位配置表"), "group page must have bottom point table");
            JTree tree = page.groupTree();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            assertEquals(2, root.getChildCount(), "area tree root must contain two areas");
            assertEquals(2, ((DefaultMutableTreeNode) root.getChildAt(0)).getChildCount(),
                    "first area must contain two groups");
            JTable pointTable = findAll(page, JTable.class).get(0);
            assertEquals(32, pointTable.getRowHeight(), "group point table row height");
        });
    }

    private static void groupPageBindsEveryRealConfigurationAction() throws Exception {
        runOnEdtAndWait(() -> {
            int[] actions = new int[10];
            GroupManagementPage.Actions callbacks = new GroupManagementPage.Actions(
                    () -> { },
                    () -> actions[0]++, () -> actions[1]++, () -> actions[2]++, () -> actions[3]++,
                    () -> actions[4]++, () -> actions[5]++, () -> actions[6]++,
                    () -> actions[7]++, () -> actions[8]++, () -> actions[9]++);
            GroupManagementPage page = new GroupManagementPage(
                    groupComponents(new JList<>(new DefaultListModel<>())), callbacks);
            List<String> buttons = List.of(
                    "新增组", "删除组", "新增点位", "删除点位", "保存配置", "放弃修改", "验证配置",
                    "开始监控", "停止", "立即检测");
            for (int index = 0; index < buttons.size(); index++) {
                AbstractButton button = findButton(page, buttons.get(index));
                assertTrue(button != null, "missing group action " + buttons.get(index));
                button.setEnabled(true);
                button.doClick();
                assertEquals(1, actions[index], "group action callback " + buttons.get(index));
            }
        });
    }

    private static void connectionPageUsesListFormAndTestSafetyColumn() throws Exception {
        runOnEdtAndWait(() -> {
            ConnectionManagementPage page = new ConnectionManagementPage(
                    connectionComponents(), noOpConnectionActions());
            List<String> cardTitles = findAll(page, SectionCard.class).stream().map(SectionCard::title).toList();
            assertTrue(cardTitles.contains("连接列表"), "connection page left list");
            assertTrue(cardTitles.contains("连接配置"), "connection page center form");
            assertTrue(cardTitles.contains("连接测试与安全说明"), "connection page right test and safety");
            List<String> labels = findAll(page, JLabel.class).stream().map(JLabel::getText).toList();
            for (String required : List.of(
                    "连接名称：", "数据库类型：", "服务器地址/IP：", "端口：", "数据库名：",
                    "数据库空间/Schema：", "用户名：", "SSL模式：", "密码：",
                    "数据库访问模式：只读")) {
                assertTrue(labels.contains(required), "missing connection field or safety text " + required);
            }
            String allText = String.join(" ", labels);
            assertFalse(allText.contains("jdbc:postgresql://"), "connection page must not display a JDBC URL");
        });
    }

    private static void connectionPageBindsEveryRealAction() throws Exception {
        runOnEdtAndWait(() -> {
            int[] actions = new int[5];
            ConnectionManagementPage.Actions callbacks = new ConnectionManagementPage.Actions(
                    () -> { }, () -> { },
                    () -> actions[0]++, () -> actions[1]++, () -> actions[2]++,
                    () -> actions[3]++, () -> actions[4]++);
            ConnectionManagementPage page = new ConnectionManagementPage(connectionComponents(), callbacks);
            List<String> buttons = List.of("新建连接", "保存连接", "删除连接", "测试连接", "设为当前连接");
            for (int index = 0; index < buttons.size(); index++) {
                AbstractButton button = findButton(page, buttons.get(index));
                assertTrue(button != null, "missing connection action " + buttons.get(index));
                button.doClick();
                assertEquals(1, actions[index], "connection action callback " + buttons.get(index));
            }
        });
    }

    private static GroupManagementPage.Components groupComponents(JList<String> groupList) {
        JTable table = new JTable(new DefaultTableModel(
                new Object[] {"角色", "别名", "点位编码", "启用"}, 0));
        return new GroupManagementPage.Components(
                groupList,
                new JTextField(), new JTextField(), new JTextField(), new JTextField(),
                new JCheckBox("启用"), new JCheckBox("启用规则"), new JCheckBox("使用位无货架"),
                new JCheckBox("备用位下限参与报警"),
                spinner(3), spinner(5), spinner(1),
                table, new JPanel(), new JLabel("当前判断：未检测"), new JTextArea(),
                new javax.swing.JButton("开始监控"), new javax.swing.JButton("停止"),
                new javax.swing.JButton("立即检测"));
    }

    private static GroupManagementPage.Actions noOpGroupActions() {
        return new GroupManagementPage.Actions(
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { },
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { });
    }

    private static ConnectionManagementPage.Components connectionComponents() {
        return new ConnectionManagementPage.Components(
                new JList<>(new String[] {"本地演示"}),
                new JTextField(), new JTextField(), new JComboBox<>(new String[] {"postgres", "h2"}),
                new JTextField(), spinner(5432), new JTextField(), new JTextField(), new JTextField(),
                new JComboBox<>(new String[] {"disable", "prefer", "require"}),
                new JTextField(), new JPasswordField(), new JLabel("尚未执行连接测试"));
    }

    private static ConnectionManagementPage.Actions noOpConnectionActions() {
        return new ConnectionManagementPage.Actions(
                () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { }, () -> { });
    }

    private static JSpinner spinner(int value) {
        return new JSpinner(new SpinnerNumberModel(value, 0, 65535, 1));
    }

    private static AbstractButton findButton(Component root, String text) {
        for (AbstractButton button : findAll(root, AbstractButton.class)) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        return null;
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> values = new ArrayList<>();
        collect(root, type, values);
        return values;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> values) {
        if (type.isInstance(component)) {
            values.add(type.cast(component));
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                collect(child, type, values);
            }
        }
    }

    private static void runOnEdtAndWait(ThrowingRunnable runnable) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception nested) {
                throw nested;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
