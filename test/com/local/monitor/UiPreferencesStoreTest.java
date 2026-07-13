package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;

public final class UiPreferencesStoreTest {
    public static void main(String[] args) throws Exception {
        savesLoadsAndRestoresDefaults();
        rejectsInvalidValues();
        System.out.println("UiPreferencesStoreTest PASS");
    }

    private static void savesLoadsAndRestoresDefaults() throws Exception {
        Path path = Files.createTempDirectory("ui-settings-test").resolve("ui-settings.properties");
        UiPreferencesStore store = new UiPreferencesStore(path);

        UiPreferences defaults = store.load();
        TestSupport.assertEquals("监控总览", defaults.defaultPage(), "default home page should be overview");
        TestSupport.assertTrue(defaults.alertPopupEnabled(), "alert popup should default enabled");

        UiPreferences custom = new UiPreferences(
                "报警中心",
                15,
                false,
                true,
                14,
                "紧凑",
                false,
                true);
        store.save(custom);

        UiPreferences loaded = store.load();
        TestSupport.assertEquals("报警中心", loaded.defaultPage(), "default page should round-trip");
        TestSupport.assertEquals(15, loaded.overviewRefreshSeconds(), "overview refresh should round-trip");
        TestSupport.assertFalse(loaded.alertPopupEnabled(), "alert popup setting should round-trip");
        TestSupport.assertTrue(loaded.alertSoundEnabled(), "sound setting should round-trip");
        TestSupport.assertEquals(14, loaded.logRetentionDays(), "retention should round-trip");
        TestSupport.assertEquals("紧凑", loaded.density(), "density should round-trip");
        TestSupport.assertFalse(loaded.startupSelfTestEnabled(), "startup self-test should round-trip");
        TestSupport.assertTrue(loaded.autoCleanupLogsEnabled(), "cleanup setting should round-trip");

        store.restoreDefaults();
        TestSupport.assertEquals("监控总览", store.load().defaultPage(), "restore should reset default page");
    }

    private static void rejectsInvalidValues() throws Exception {
        Path path = Files.createTempDirectory("ui-settings-invalid-test").resolve("ui-settings.properties");
        UiPreferencesStore store = new UiPreferencesStore(path);

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> store.save(new UiPreferences("不存在页面", 10, true, false, 30, "标准", true, false)),
                "unknown default page should be rejected");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> store.save(new UiPreferences("监控总览", 0, true, false, 30, "标准", true, false)),
                "invalid refresh interval should be rejected");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> store.save(new UiPreferences("监控总览", 10, true, false, 0, "标准", true, false)),
                "invalid retention should be rejected");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> store.save(new UiPreferences("监控总览", 10, true, false, 30, "不存在密度", true, false)),
                "invalid density should be rejected");
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertFalse(boolean condition, String message) {
            if (condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        static void assertThrows(Class<? extends Throwable> type, ThrowingRunnable action, String message) {
            try {
                action.run();
            } catch (Throwable t) {
                if (type.isInstance(t)) {
                    return;
                }
                throw new AssertionError(message + " wrong exception=" + t);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
