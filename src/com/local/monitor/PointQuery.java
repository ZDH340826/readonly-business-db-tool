package com.local.monitor;

public final class PointQuery {
    private PointQuery() {
    }

    public static String buildSelectSql(String schema, int pointCount) {
        if (pointCount <= 0) {
            throw new IllegalArgumentException("至少需要一个点位");
        }
        String safeSchema = validateIdentifier(schema, "schema");
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < pointCount; i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        return "select point_code, shelf_code, shelf_status, status, lock_state, area_code, "
                + "next_area_code, updated_at, marked_at "
                + "from " + safeSchema + ".shelf_point_status "
                + "where point_code in (" + placeholders + ")";
    }

    public static String validateIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(label + "只能包含字母、数字、下划线，且不能以数字开头");
        }
        return value;
    }
}


