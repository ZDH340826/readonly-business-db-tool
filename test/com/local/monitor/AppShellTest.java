package com.local.monitor;

import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class AppShellTest {
    private AppShellTest() {
    }

    public static void main(String[] args) throws Exception {
        shellUsesRequiredGeometryAndEightRealRoutes();
        statusBarsExposeOperationalTextAndVersion();
        applicationUsesExtractedShellAndPreferredSize();
        System.out.println("AppShellTest PASS");
    }

    private static void shellUsesRequiredGeometryAndEightRealRoutes() throws Exception {
        runOnEdtAndWait(() -> {
            JList<String> navigation = new JList<>(new String[] {
                    "监控总览", "点位组管理", "报警中心", "连接管理",
                    "数据查询", "数据源浏览器", "日志与系统", "系统设置"
            });
            TopStatusBar top = new TopStatusBar(
                    new JLabel("当前连接：未选择"),
                    new JLabel("监控状态：未运行"),
                    new JLabel("上次检测：--"),
                    new JLabel("下次检测：--"));
            BottomStatusBar bottom = new BottomStatusBar(new JLabel("就绪"), "0.5.0-rc.1");
            AppShell shell = new AppShell(navigation, new JPanel(), top, bottom);

            assertEquals(AppTheme.PREFERRED_WINDOW_SIZE, shell.getPreferredSize(), "shell preferred size");
            assertEquals(AppTheme.MINIMUM_WINDOW_SIZE, shell.getMinimumSize(), "shell minimum size");
            assertEquals(8, navigation.getModel().getSize(), "shell must retain eight real routes");
            assertEquals(46, navigation.getFixedCellHeight(), "navigation item height");
            assertTrue(navigation.getCellRenderer() instanceof DefaultListCellRenderer,
                    "navigation must have an explicit text renderer");
            NavigationSidebar sidebar = findFirst(shell, NavigationSidebar.class);
            assertTrue(sidebar != null, "shell must use extracted navigation sidebar");
            assertEquals(AppTheme.NAVIGATION_WIDTH, sidebar.getPreferredSize().width, "navigation width");
        });
    }

    private static void statusBarsExposeOperationalTextAndVersion() throws Exception {
        runOnEdtAndWait(() -> {
            TopStatusBar top = new TopStatusBar(
                    new JLabel("当前连接：本地演示"),
                    new JLabel("监控状态：运行中"),
                    new JLabel("上次检测：10:00"),
                    new JLabel("下次检测：10:10"));
            BottomStatusBar bottom = new BottomStatusBar(new JLabel("就绪"), "0.5.0-rc.1");

            assertEquals(AppTheme.TOP_BAR_HEIGHT, top.getPreferredSize().height, "top bar height");
            assertEquals(AppTheme.BOTTOM_BAR_HEIGHT, bottom.getPreferredSize().height, "bottom bar height");
            assertEquals(List.of("当前连接：本地演示", "监控状态：运行中", "上次检测：10:00", "下次检测：10:10"),
                    top.statusTexts(), "top bar must retain all four operational statuses");
            assertEquals("0.5.0-rc.1", bottom.version(), "bottom bar version");
        });
    }

    private static void applicationUsesExtractedShellAndPreferredSize() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                assertTrue(app.getContentPane() instanceof AppShell, "application content must use AppShell");
                assertEquals(AppTheme.PREFERRED_WINDOW_SIZE, app.getPreferredSize(), "application preferred size");
                assertEquals(AppTheme.MINIMUM_WINDOW_SIZE, app.getMinimumSize(), "application minimum size");
            } finally {
                app.dispose();
            }
        });
    }

    private static <T extends Component> T findFirst(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                T found = findFirst(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
