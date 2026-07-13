package com.local.monitor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PointDataQuery {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String pointKeyword;
    private final String shelfKeyword;
    private final String areaCode;
    private final String relateAreaCode;
    private final String updatedFrom;
    private final String updatedTo;
    private final int limit;
    private final int offset;

    public PointDataQuery(
            String pointKeyword,
            String shelfKeyword,
            String areaCode,
            String relateAreaCode,
            String updatedFrom,
            String updatedTo,
            int limit,
            int offset) {
        this.pointKeyword = normalize(pointKeyword);
        this.shelfKeyword = normalize(shelfKeyword);
        this.areaCode = normalize(areaCode);
        this.relateAreaCode = normalize(relateAreaCode);
        this.updatedFrom = normalize(updatedFrom);
        this.updatedTo = normalize(updatedTo);
        this.limit = Math.max(1, Math.min(MAX_LIMIT, limit <= 0 ? DEFAULT_LIMIT : limit));
        this.offset = Math.max(0, offset);
    }

    public static String fixedSelectSql(String schema) {
        String safeSchema = PointQuery.validateIdentifier(schema, "schema");
        return "select map_data_code as point_code, pod_code as shelf_code, pod_status as shelf_status, "
                + "status, ind_lock as lock_state, area_code, relate_area_code as next_area_code, "
                + "date_chg as updated_at, date_chg as marked_at "
                + "from " + safeSchema + ".tcs_map_data "
                + fixedWhereSql()
                + "order by date_chg desc, map_data_code "
                + "limit ? offset ?";
    }

    public static String fixedCountSql(String schema) {
        String safeSchema = PointQuery.validateIdentifier(schema, "schema");
        return "select count(*) from " + safeSchema + ".tcs_map_data " + fixedWhereSql();
    }

    private static String fixedWhereSql() {
        return "where (? = '' or lower(map_data_code) like lower(?) escape '\\') "
                + "and (? = '' or lower(coalesce(pod_code, '')) like lower(?) escape '\\') "
                + "and (? = '' or area_code = ?) "
                + "and (? = '' or relate_area_code = ?) "
                + "and (? = '' or date_chg >= ?) "
                + "and (? = '' or date_chg <= ?) ";
    }

    public String pointKeyword() {
        return pointKeyword;
    }

    public String shelfKeyword() {
        return shelfKeyword;
    }

    public String areaCode() {
        return areaCode;
    }

    public String relateAreaCode() {
        return relateAreaCode;
    }

    public String updatedFrom() {
        return updatedFrom;
    }

    public String updatedTo() {
        return updatedTo;
    }

    public int limit() {
        return limit;
    }

    public int offset() {
        return offset;
    }

    Timestamp fromTimestampOrEpoch() {
        if (updatedFrom.isBlank()) {
            return Timestamp.valueOf(LocalDateTime.of(1970, 1, 1, 0, 0));
        }
        return Timestamp.valueOf(parseDateTime(updatedFrom, "更新时间起始"));
    }

    Timestamp toTimestampOrFuture() {
        if (updatedTo.isBlank()) {
            return Timestamp.valueOf(LocalDateTime.of(2999, 12, 31, 23, 59, 59));
        }
        return Timestamp.valueOf(parseDateTime(updatedTo, "更新时间结束"));
    }

    String pointLike() {
        return like(pointKeyword);
    }

    String shelfLike() {
        return like(shelfKeyword);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String like(String value) {
        return "%" + escapeLikeLiteral(value) + "%";
    }

    private static String escapeLikeLiteral(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '%' || ch == '_') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    private static LocalDateTime parseDateTime(String value, String label) {
        try {
            String normalized = value.trim();
            if (normalized.length() == 10) {
                normalized = normalized + " 00:00:00";
            }
            return LocalDateTime.parse(normalized, INPUT_FORMAT);
        } catch (Exception ex) {
            throw new IllegalArgumentException(label + "必须使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
