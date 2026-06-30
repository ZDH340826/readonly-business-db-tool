package com.local.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TablePreview {
    private final List<String> columnNames;
    private final List<List<String>> rows;

    public TablePreview(List<String> columnNames, List<List<String>> rows) {
        this.columnNames = Collections.unmodifiableList(new ArrayList<>(columnNames));
        List<List<String>> copiedRows = new ArrayList<>();
        for (List<String> row : rows) {
            copiedRows.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        this.rows = Collections.unmodifiableList(copiedRows);
    }

    public List<String> columnNames() {
        return columnNames;
    }

    public List<List<String>> rows() {
        return rows;
    }
}


