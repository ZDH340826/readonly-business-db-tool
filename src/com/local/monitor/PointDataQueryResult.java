package com.local.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PointDataQueryResult {
    private final String sqlKind;
    private final String sqlTemplate;
    private final String countSqlTemplate;
    private final int totalCount;
    private final int limit;
    private final int offset;
    private final List<PointRecord> records;

    public PointDataQueryResult(
            String sqlKind,
            String sqlTemplate,
            String countSqlTemplate,
            int totalCount,
            int limit,
            int offset,
            List<PointRecord> records) {
        this.sqlKind = sqlKind;
        this.sqlTemplate = sqlTemplate;
        this.countSqlTemplate = countSqlTemplate;
        this.totalCount = totalCount;
        this.limit = limit;
        this.offset = offset;
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
    }

    public String sqlKind() {
        return sqlKind;
    }

    public String sqlTemplate() {
        return sqlTemplate;
    }

    public String countSqlTemplate() {
        return countSqlTemplate;
    }

    public int totalCount() {
        return totalCount;
    }

    public int limit() {
        return limit;
    }

    public int offset() {
        return offset;
    }

    public List<PointRecord> records() {
        return records;
    }
}
