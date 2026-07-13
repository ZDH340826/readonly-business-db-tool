package com.local.monitor;

public final class ColumnInfo {
    private final String name;
    private final String typeName;
    private final int size;
    private final boolean nullable;
    private final String defaultValue;
    private final String remarks;

    public ColumnInfo(String name, String typeName, int size, boolean nullable) {
        this(name, typeName, size, nullable, "", "");
    }

    public ColumnInfo(String name, String typeName, int size, boolean nullable, String defaultValue, String remarks) {
        this.name = name;
        this.typeName = typeName;
        this.size = size;
        this.nullable = nullable;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.remarks = remarks == null ? "" : remarks;
    }

    public String name() {
        return name;
    }

    public String typeName() {
        return typeName;
    }

    public int size() {
        return size;
    }

    public boolean nullable() {
        return nullable;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String remarks() {
        return remarks;
    }
}


