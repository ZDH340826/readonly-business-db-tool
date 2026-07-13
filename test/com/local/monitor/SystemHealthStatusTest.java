package com.local.monitor;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

public final class SystemHealthStatusTest {
    public static void main(String[] args) throws Exception {
        configFileStatusDistinguishesMissingReadableAndFailed();
        logDirectoryStatusDistinguishesMissingWritableAndUnwritable();
        logsPageHasOnlyOneDynamicHealthCardSet();
        System.out.println("SystemHealthStatusTest PASS");
    }

    private static void configFileStatusDistinguishesMissingReadableAndFailed() throws Exception {
        Path dir = Files.createTempDirectory("health-config");
        Path missing = dir.resolve("missing.properties");
        Path readable = Files.writeString(dir.resolve("config.properties"), "ok=true");
        Path notAFile = Files.createDirectory(dir.resolve("not-a-file.properties"));
        assertEquals("缺失", ShelfPointMonitorApp.configFileHealthStatus(missing),
                "missing config file should be marked missing");
        assertEquals("正常", ShelfPointMonitorApp.configFileHealthStatus(readable),
                "readable config file should be marked normal");
        assertEquals("读取失败", ShelfPointMonitorApp.configFileHealthStatus(notAFile),
                "existing non-file config path should be marked failed");
    }

    private static void logDirectoryStatusDistinguishesMissingWritableAndUnwritable() throws Exception {
        Path dir = Files.createTempDirectory("health-log");
        Path missing = dir.resolve("missing-logs");
        Path writable = Files.createDirectory(dir.resolve("logs"));
        Path notDirectory = Files.writeString(dir.resolve("logs.txt"), "");
        assertEquals("缺失", ShelfPointMonitorApp.logDirectoryHealthStatus(missing),
                "missing log directory should be marked missing");
        assertEquals("可写", ShelfPointMonitorApp.logDirectoryHealthStatus(writable),
                "writable log directory should be marked writable");
        assertEquals("不可写", ShelfPointMonitorApp.logDirectoryHealthStatus(notDirectory),
                "non-directory log path should be marked unwritable");
    }

    private static void logsPageHasOnlyOneDynamicHealthCardSet() throws Exception {
        runOnEdtAndWait(() -> {
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            try {
                Component logsPage = findTitledComponent(app.getContentPane(), "日志与系统");
                if (logsPage == null) {
                    throw new AssertionError("logs page should exist");
                }
                Map<String, Integer> counts = new LinkedHashMap<>();
                counts.put("监控调度器：", 0);
                counts.put("当前连接：", 0);
                counts.put("最近一次检测：", 0);
                counts.put("配置文件：", 0);
                counts.put("日志目录：", 0);
                counts.put("自检状态：", 0);
                countHealthLabels(logsPage, counts);
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    assertEquals(1, entry.getValue(), entry.getKey() + " should appear once in health cards");
                }
            } finally {
                app.dispose();
            }
        });
    }

    private static void countHealthLabels(Component component, Map<String, Integer> counts) {
        if (component instanceof JLabel) {
            String text = ((JLabel) component).getText();
            for (String prefix : counts.keySet()) {
                if (text != null && text.startsWith(prefix)) {
                    counts.put(prefix, counts.get(prefix) + 1);
                }
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                countHealthLabels(child, counts);
            }
        }
    }

    private static Component findTitledComponent(Component component, String title) {
        if (component instanceof JComponent
                && ((JComponent) component).getBorder() instanceof TitledBorder
                && title.equals(((TitledBorder) ((JComponent) component).getBorder()).getTitle())) {
            return component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                Component found = findTitledComponent(child, title);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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
        } catch (java.lang.reflect.InvocationTargetException ex) {
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

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
