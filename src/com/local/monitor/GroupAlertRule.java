package com.local.monitor;

public final class GroupAlertRule {
    public static final int MIN_DURATION_MINUTES = 1;
    public static final int MAX_DURATION_MINUTES = 1440;

    private final boolean enabled;
    private final boolean requireUsePointEmpty;
    private final int minBackupAvailable;
    private final int durationMinutes;

    public GroupAlertRule(boolean enabled, boolean requireUsePointEmpty, int minBackupAvailable, int durationMinutes) {
        if (minBackupAvailable < 0) {
            throw new IllegalArgumentException("minBackupAvailable must be >= 0");
        }
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("durationMinutes must be between 1 and 1440");
        }
        this.enabled = enabled;
        this.requireUsePointEmpty = requireUsePointEmpty;
        this.minBackupAvailable = minBackupAvailable;
        this.durationMinutes = durationMinutes;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean requireUsePointEmpty() {
        return requireUsePointEmpty;
    }

    public int minBackupAvailable() {
        return minBackupAvailable;
    }

    public int durationMinutes() {
        return durationMinutes;
    }
}
