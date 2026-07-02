package com.local.monitor;

public enum PointMaterialStatus {
    AVAILABLE("有料"),
    EMPTY("无料"),
    MISSING("未查到"),
    DISABLED("停用");

    private final String displayText;

    PointMaterialStatus(String displayText) {
        this.displayText = displayText;
    }

    public String displayText() {
        return displayText;
    }
}
