package com.local.monitor;

import java.time.Duration;
import java.time.LocalDateTime;

public final class GroupRuntimeState {
    private LocalDateTime lastCheckedAt;
    private LocalDateTime conditionFirstMatchedAt;
    private LocalDateTime conditionLastMatchedAt;
    private boolean acknowledged;
    private boolean activeDialogShown;

    public LocalDateTime lastCheckedAt() {
        return lastCheckedAt;
    }

    public void markChecked(LocalDateTime checkedAt) {
        lastCheckedAt = checkedAt;
    }

    public LocalDateTime conditionFirstMatchedAt() {
        return conditionFirstMatchedAt;
    }

    public LocalDateTime conditionLastMatchedAt() {
        return conditionLastMatchedAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    boolean activeDialogShown() {
        return activeDialogShown;
    }

    public void acknowledge() {
        acknowledged = true;
        activeDialogShown = true;
    }

    void markMatched(LocalDateTime matchedAt) {
        if (conditionFirstMatchedAt == null) {
            conditionFirstMatchedAt = matchedAt;
        }
        conditionLastMatchedAt = matchedAt;
        lastCheckedAt = matchedAt;
    }

    public int continuousMatchedSeconds(LocalDateTime now) {
        if (conditionFirstMatchedAt == null || now == null) {
            return 0;
        }
        long seconds = Duration.between(conditionFirstMatchedAt, now).getSeconds();
        if (seconds <= 0L) {
            return 0;
        }
        if (seconds > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) seconds;
    }

    public int continuousMatchedMinutes(LocalDateTime now) {
        int seconds = continuousMatchedSeconds(now);
        if (seconds == 0) {
            return 0;
        }
        return (seconds + 59) / 60;
    }

    public int continuousMatchedMinutes() {
        return continuousMatchedMinutes(conditionLastMatchedAt);
    }

    void markActiveDialogShown() {
        activeDialogShown = true;
    }

    void reset() {
        conditionFirstMatchedAt = null;
        conditionLastMatchedAt = null;
        acknowledged = false;
        activeDialogShown = false;
    }
}
