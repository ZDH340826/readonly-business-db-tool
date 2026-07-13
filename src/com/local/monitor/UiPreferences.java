package com.local.monitor;

import java.util.List;

public final class UiPreferences {
    private static final List<String> PAGE_NAMES = List.of(
            "监控总览",
            "点位组管理",
            "报警中心",
            "连接管理",
            "数据查询",
            "数据源浏览器",
            "日志与系统",
            "系统设置");
    private static final List<String> DENSITIES = List.of("紧凑", "标准", "舒适");

    private final String defaultPage;
    private final int overviewRefreshSeconds;
    private final boolean alertPopupEnabled;
    private final boolean alertSoundEnabled;
    private final int logRetentionDays;
    private final String density;
    private final boolean startupSelfTestEnabled;
    private final boolean autoCleanupLogsEnabled;

    public UiPreferences(
            String defaultPage,
            int overviewRefreshSeconds,
            boolean alertPopupEnabled,
            boolean alertSoundEnabled,
            int logRetentionDays,
            String density,
            boolean startupSelfTestEnabled,
            boolean autoCleanupLogsEnabled) {
        this.defaultPage = defaultPage;
        this.overviewRefreshSeconds = overviewRefreshSeconds;
        this.alertPopupEnabled = alertPopupEnabled;
        this.alertSoundEnabled = alertSoundEnabled;
        this.logRetentionDays = logRetentionDays;
        this.density = density;
        this.startupSelfTestEnabled = startupSelfTestEnabled;
        this.autoCleanupLogsEnabled = autoCleanupLogsEnabled;
        validate(this);
    }

    public static UiPreferences defaults() {
        return new UiPreferences("监控总览", 10, true, false, 30, "标准", false, true);
    }

    public static List<String> pageNames() {
        return PAGE_NAMES;
    }

    public static List<String> densities() {
        return DENSITIES;
    }

    public static void validate(UiPreferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("界面设置不能为空");
        }
        if (!PAGE_NAMES.contains(preferences.defaultPage)) {
            throw new IllegalArgumentException("默认首页不在可用页面列表中");
        }
        if (preferences.overviewRefreshSeconds < 5 || preferences.overviewRefreshSeconds > 3600) {
            throw new IllegalArgumentException("监控总览自动刷新间隔必须在 5 到 3600 秒之间");
        }
        if (preferences.logRetentionDays < 1 || preferences.logRetentionDays > 3650) {
            throw new IllegalArgumentException("日志保留天数必须在 1 到 3650 天之间");
        }
        if (!DENSITIES.contains(preferences.density)) {
            throw new IllegalArgumentException("界面显示密度不合法");
        }
    }

    public String defaultPage() {
        return defaultPage;
    }

    public int overviewRefreshSeconds() {
        return overviewRefreshSeconds;
    }

    public boolean alertPopupEnabled() {
        return alertPopupEnabled;
    }

    public boolean alertSoundEnabled() {
        return alertSoundEnabled;
    }

    public int logRetentionDays() {
        return logRetentionDays;
    }

    public String density() {
        return density;
    }

    public boolean startupSelfTestEnabled() {
        return startupSelfTestEnabled;
    }

    public boolean autoCleanupLogsEnabled() {
        return autoCleanupLogsEnabled;
    }
}
