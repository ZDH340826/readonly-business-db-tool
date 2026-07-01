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
        return "select map_data_code as point_code, pod_code as shelf_code, pod_status as shelf_status, "
                + "status, ind_lock as lock_state, area_code, relate_area_code as next_area_code, "
                + "date_chg as updated_at, date_cr as marked_at "
                + "from " + safeSchema + ".tcs_map_data "
                + "where map_data_code in (" + placeholders + ")";
    }

    public static String validateIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(label + "只能包含字母、数字、下划线，且不能以数字开头");
        }
        return value;
    }
}


