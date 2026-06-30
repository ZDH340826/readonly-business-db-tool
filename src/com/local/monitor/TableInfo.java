package com.local.monitor;

public final class TableInfo {
    private final String schema;
    private final String name;
    private final String type;

    public TableInfo(String schema, String name, String type) {
        this.schema = schema;
        this.name = name;
        this.type = type;
    }

    public String schema() {
        return schema;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }
}


