package com.local.monitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;

public final class ShelfPointMonitorAppUiTest {
    public static void main(String[] args) throws Exception {
        appHasThreeNavigationPages();
        connectionPageUsesChineseOperatorLabels();
        alertPageContainsGroupPointTable();
        gridBagPanelsDoNotOverlapCells();
        groupAlertTextsDoNotExposeTechnicalStatusNames();
        System.out.println("ShelfPointMonitorAppUiTest PASS");
    }

    private static void appHasThreeNavigationPages() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                JList<?> navigation = findFirst(app.getContentPane(), JList.class);
                TestSupport.assertTrue(navigation != null, "app should have a navigation list");
                TestSupport.assertEquals(3, navigation.getModel().getSize(), "app should have three navigation pages");
                TestSupport.assertEquals("连接管理", navigation.getModel().getElementAt(0), "first page should be connection management");
                TestSupport.assertEquals("数据库浏览器", navigation.getModel().getElementAt(1), "second page should be database browser");
                TestSupport.assertEquals("点位缺料报警", navigation.getModel().getElementAt(2), "third page should be point alert");
            } finally {
                app.dispose();
            }
        });
    }

    private static void gridBagPanelsDoNotOverlapCells() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                scanGridBagContainers(app.getContentPane());
            } finally {
                app.dispose();
            }
        });
    }

    private static void connectionPageUsesChineseOperatorLabels() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Set<String> texts = collectVisibleTexts(app.getContentPane());
                TestSupport.assertTrue(texts.contains("连接ID："), "connection page should show Chinese ID label");
                TestSupport.assertTrue(texts.contains("连接名称："), "connection page should show Chinese name label");
                TestSupport.assertTrue(texts.contains("数据库类型："), "connection page should show Chinese type label");
                TestSupport.assertTrue(texts.contains("服务器地址/IP："), "connection page should show Chinese host label");
                TestSupport.assertTrue(texts.contains("端口："), "connection page should show Chinese port label");
                TestSupport.assertTrue(texts.contains("数据库名："), "connection page should show Chinese database label");
                TestSupport.assertTrue(texts.contains("数据库空间/Schema："), "connection page should show Chinese schema label");
                TestSupport.assertTrue(texts.contains("用户名："), "connection page should show Chinese user label");
                TestSupport.assertTrue(texts.contains("密码："), "connection page should show Chinese password label");
                TestSupport.assertTrue(texts.contains("SSL模式："), "connection page should show Chinese SSL label");
                TestSupport.assertTrue(texts.contains("新建连接"), "connection page should show Chinese new button");
                TestSupport.assertTrue(texts.contains("测试连接并使用"), "connection page should show Chinese test button");
            } finally {
                app.dispose();
            }
        });
    }

    private static void alertPageContainsGroupPointTable() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                TestSupport.assertTrue(hasGroupPointTable(app.getContentPane()),
                        "alert page should contain the group point table");
            } finally {
                app.dispose();
            }
        });
    }

    private static void groupAlertTextsDoNotExposeTechnicalStatusNames() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Method formatResult = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "formatGroupCheckResult",
                        String.class,
                        List.class,
                        GroupEvaluation.class);
                formatResult.setAccessible(true);
                Method alertText = ShelfPointMonitorApp.class.getDeclaredMethod(
                        "groupAlertText",
                        GroupEvaluation.class);
                alertText.setAccessible(true);

                String pendingRuntimeText = (String) formatResult.invoke(
                        app,
                        "手动检测",
                        List.of(),
                        groupEvaluation(GroupAlertStatus.PENDING_ALERT, "观察中：使用位无料，备用位有料 2/4。"));
                String activeDialogText = (String) alertText.invoke(
                        app,
                        groupEvaluation(GroupAlertStatus.ACTIVE_ALERT, "需关注：请现场确认使用位和备用位。"));
                String ackedDialogText = (String) alertText.invoke(
                        app,
                        groupEvaluation(GroupAlertStatus.ACKED_ALERT, "已关注：等待现场处理完成。"));

                TestSupport.assertContains(pendingRuntimeText, "观察中", "runtime text should use Chinese pending status");
                TestSupport.assertContains(activeDialogText, "状态：需关注", "dialog text should use Chinese active status");
                TestSupport.assertContains(ackedDialogText, "状态：已关注", "dialog text should use Chinese acknowledged status");
                assertNoTechnicalGroupText(pendingRuntimeText, "runtime text");
                assertNoTechnicalGroupText(activeDialogText, "active dialog text");
                assertNoTechnicalGroupText(ackedDialogText, "acknowledged dialog text");
            } finally {
                app.dispose();
            }
        });
    }

    private static void scanGridBagContainers(Container container) {
        if (container.getLayout() instanceof GridBagLayout) {
            GridBagLayout layout = (GridBagLayout) container.getLayout();
            Set<String> occupied = new HashSet<>();
            for (Component component : container.getComponents()) {
                GridBagConstraints constraints = layout.getConstraints(component);
                int width = Math.max(1, constraints.gridwidth);
                int height = Math.max(1, constraints.gridheight);
                for (int x = constraints.gridx; x < constraints.gridx + width; x++) {
                    for (int y = constraints.gridy; y < constraints.gridy + height; y++) {
                        String cell = x + "," + y;
                        if (!occupied.add(cell)) {
                            throw new AssertionError("GridBag cell overlap at " + cell + " in " + container.getClass());
                        }
                    }
                }
            }
        }
        for (Component component : container.getComponents()) {
            if (component instanceof Container) {
                scanGridBagContainers((Container) component);
            }
        }
    }

    private static <T> T findFirst(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container) {
                T found = findFirst((Container) component, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasGroupPointTable(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTable) {
                JTable table = (JTable) component;
                if (table.getColumnCount() == 4
                        && "角色".equals(table.getColumnName(0))
                        && "点位编码".equals(table.getColumnName(2))) {
                    return true;
                }
            }
            if (component instanceof Container && hasGroupPointTable((Container) component)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> collectVisibleTexts(Container container) {
        Set<String> texts = new HashSet<>();
        collectVisibleTexts(container, texts);
        return texts;
    }

    private static GroupEvaluation groupEvaluation(GroupAlertStatus status, String message) {
        return new GroupEvaluation(
                "group-001",
                "A区",
                "一号料架",
                "标准件",
                status,
                true,
                4,
                2,
                2,
                true,
                5,
                false,
                message);
    }

    private static void assertNoTechnicalGroupText(String text, String context) {
        TestSupport.assertNotContains(text, "status=", context + " should not leak debug status field");
        TestSupport.assertNotContains(text, "PENDING_ALERT", context + " should not leak pending enum");
        TestSupport.assertNotContains(text, "ACTIVE_ALERT", context + " should not leak active enum");
        TestSupport.assertNotContains(text, "ACKED_ALERT", context + " should not leak acknowledged enum");
        TestSupport.assertNotContains(text, "useEmpty=", context + " should not leak debug use point field");
        TestSupport.assertNotContains(text, "backup=", context + " should not leak debug backup field");
        TestSupport.assertNotContains(text, "continuous=", context + " should not leak debug duration field");
    }

    private static void collectVisibleTexts(Component component, Set<String> texts) {
        if (component instanceof JLabel) {
            texts.add(((JLabel) component).getText());
        } else if (component instanceof AbstractButton) {
            texts.add(((AbstractButton) component).getText());
        } else if (component instanceof JTextComponent) {
            texts.add(((JTextComponent) component).getText());
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectVisibleTexts(child, texts);
            }
        }
    }

    private static void runOnEdtAndWait(ThrowingRunnable runnable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception) {
                throw (Exception) cause.getCause();
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw ex;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        static void assertContains(String text, String expected, String message) {
            if (text == null || !text.contains(expected)) {
                throw new AssertionError(message + " expected to contain=" + expected + " actual=" + text);
            }
        }

        static void assertNotContains(String text, String unexpected, String message) {
            if (text != null && text.contains(unexpected)) {
                throw new AssertionError(message + " unexpected=" + unexpected + " actual=" + text);
            }
        }
    }
}
