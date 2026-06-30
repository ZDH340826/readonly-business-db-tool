package com.local.monitor;

public final class GroupMonitorPoint {
    private final String id;
    private final String code;
    private final String alias;
    private final PointRole role;
    private final boolean enabled;
    private final int sortOrder;

    public GroupMonitorPoint(String id, String code, String alias, PointRole role, boolean enabled, int sortOrder) {
        this.id = requireText(id, "point id");
        this.code = requireText(code, "point code");
        this.alias = requireText(alias, "point alias");
        if (role == null) {
            throw new IllegalArgumentException("point role is required");
        }
        this.role = role;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    public String id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String alias() {
        return alias;
    }

    public PointRole role() {
        return role;
    }

    public boolean enabled() {
        return enabled;
    }

    public int sortOrder() {
        return sortOrder;
    }

    static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
