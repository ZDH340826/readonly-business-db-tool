package com.local.monitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public final class ShelfPointMonitorAppUiTest {
    public static void main(String[] args) throws Exception {
        appHasThreeNavigationPages();
        alertPageContainsGroupPointTable();
        gridBagPanelsDoNotOverlapCells();
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
    }
}


