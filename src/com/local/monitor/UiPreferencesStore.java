package com.local.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class UiPreferencesStore {
    private final Path path;

    public UiPreferencesStore(Path path) {
        this.path = path;
    }

    public UiPreferences load() {
        if (!Files.exists(path)) {
            return UiPreferences.defaults();
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return new UiPreferences(
                    p.getProperty("defaultPage", UiPreferences.defaults().defaultPage()),
                    parseInt(p.getProperty("overviewRefreshSeconds"),
                            UiPreferences.defaults().overviewRefreshSeconds()),
                    parseBoolean(p.getProperty("alertPopupEnabled"),
                            UiPreferences.defaults().alertPopupEnabled()),
                    parseBoolean(p.getProperty("alertSoundEnabled"),
                            UiPreferences.defaults().alertSoundEnabled()),
                    parseInt(p.getProperty("logRetentionDays"),
                            UiPreferences.defaults().logRetentionDays()),
                    p.getProperty("density", UiPreferences.defaults().density()),
                    parseBoolean(p.getProperty("startupSelfTestEnabled"),
                            UiPreferences.defaults().startupSelfTestEnabled()),
                    parseBoolean(p.getProperty("autoCleanupLogsEnabled"),
                            UiPreferences.defaults().autoCleanupLogsEnabled()));
        } catch (Exception ignored) {
            return UiPreferences.defaults();
        }
    }

    public void save(UiPreferences preferences) throws IOException {
        UiPreferences.validate(preferences);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties p = new Properties();
        p.setProperty("defaultPage", preferences.defaultPage());
        p.setProperty("overviewRefreshSeconds", String.valueOf(preferences.overviewRefreshSeconds()));
        p.setProperty("alertPopupEnabled", String.valueOf(preferences.alertPopupEnabled()));
        p.setProperty("alertSoundEnabled", String.valueOf(preferences.alertSoundEnabled()));
        p.setProperty("logRetentionDays", String.valueOf(preferences.logRetentionDays()));
        p.setProperty("density", preferences.density());
        p.setProperty("startupSelfTestEnabled", String.valueOf(preferences.startupSelfTestEnabled()));
        p.setProperty("autoCleanupLogsEnabled", String.valueOf(preferences.autoCleanupLogsEnabled()));
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "UI preferences. No database secrets.");
        }
    }

    public void restoreDefaults() throws IOException {
        save(UiPreferences.defaults());
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value);
    }
}
