package com.local.monitor;

import java.util.Collections;
import java.util.List;

public final class MonitorEvaluation {
    private final List<PointAlert> alerts;
    private final String alertKey;
    private final boolean suppressedByAck;

    public MonitorEvaluation(List<PointAlert> alerts, String alertKey, boolean suppressedByAck) {
        this.alerts = Collections.unmodifiableList(alerts);
        this.alertKey = alertKey;
        this.suppressedByAck = suppressedByAck;
    }

    public boolean hasActiveAlert() {
        return !alerts.isEmpty() && !suppressedByAck;
    }

    public List<PointAlert> alerts() {
        return suppressedByAck ? Collections.emptyList() : alerts;
    }

    public String alertKey() {
        return alertKey;
    }

    public boolean suppressedByAck() {
        return suppressedByAck;
    }
}


