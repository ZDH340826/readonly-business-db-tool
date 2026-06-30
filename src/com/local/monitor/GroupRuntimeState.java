package com.local.monitor;

public final class GroupRuntimeState {
    private int continuousMatchedMinutes;
    private boolean acknowledged;
    private boolean activeDialogShown;

    public int continuousMatchedMinutes() {
        return continuousMatchedMinutes;
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

    void markMatched() {
        continuousMatchedMinutes++;
    }

    void markActiveDialogShown() {
        activeDialogShown = true;
    }

    void reset() {
        continuousMatchedMinutes = 0;
        acknowledged = false;
        activeDialogShown = false;
    }
}
