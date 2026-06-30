package com.local.monitor;

public final class ColumnInfo {
    private final String name;
    private final String typeName;
    private final int size;
    private final boolean nullable;

    public ColumnInfo(String name, String typeName, int size, boolean nullable) {
        this.name = name;
        this.typeName = typeName;
        this.size = size;
        this.nullable = nullable;
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
}


