package com.local.monitor;

public final class PointDefinition {
    public static final int DEFAULT_INTERVAL_MINUTES = 5;
    public static final int MIN_INTERVAL_MINUTES = 1;
    public static final int MAX_INTERVAL_MINUTES = 1440;

    private final String code;
    private final String alias;
    private final int intervalMinutes;

    public PointDefinition(String code, String alias) {
        this(code, alias, DEFAULT_INTERVAL_MINUTES);
    }

    public PointDefinition(String code, String alias, int intervalMinutes) {
        this.code = requireText(code, "point code");
        this.alias = requireText(alias, "point alias");
        this.intervalMinutes = validateInterval(intervalMinutes);
    }

    public String code() {
        return code;
    }

    public String alias() {
        return alias;
    }

    public int intervalMinutes() {
        return intervalMinutes;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static int validateInterval(int value) {
        if (value < MIN_INTERVAL_MINUTES || value > MAX_INTERVAL_MINUTES) {
            throw new IllegalArgumentException("intervalMinutes must be between 1 and 1440");
        }
        return value;
    }
}


