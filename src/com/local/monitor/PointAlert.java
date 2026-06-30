package com.local.monitor;

public final class PointAlert {
    private final String code;
    private final String alias;
    private final String message;

    public PointAlert(String code, String alias, String message) {
        this.code = code;
        this.alias = alias;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String alias() {
        return alias;
    }

    public String message() {
        return message;
    }
}


