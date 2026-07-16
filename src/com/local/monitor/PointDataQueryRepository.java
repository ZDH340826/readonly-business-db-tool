package com.local.monitor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PointDataQueryRepository {
    public PointDataQueryResult query(DbConfig config, char[] password, PointDataQuery query) throws Exception {
        String sql = PointDataQuery.fixedSelectSql(config.schema());
        String countSql = PointDataQuery.fixedCountSql(config.schema());
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password);
             PreparedStatement countPs = conn.prepareStatement(countSql);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            countPs.setQueryTimeout(8);
            ps.setQueryTimeout(8);
            bindFilters(countPs, query);
            int totalCount;
            try (ResultSet rs = countPs.executeQuery()) {
                rs.next();
                totalCount = rs.getInt(1);
            }
            int index = bindFilters(ps, query);
            ps.setInt(index++, query.limit());
            ps.setInt(index, query.offset());

            List<PointRecord> records = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new PointRecord(
                            rs.getString("point_code"),
                            rs.getString("shelf_code"),
                            rs.getString("shelf_status"),
                            rs.getInt("status"),
                            rs.getInt("lock_state"),
                            rs.getString("area_code"),
                            rs.getString("next_area_code"),
                            toLocalDateTime(rs.getTimestamp("updated_at")),
                            toLocalDateTime(rs.getTimestamp("marked_at"))));
                }
            }
            conn.rollback();
            return new PointDataQueryResult(
                    "select",
                    sql,
                    countSql,
                    totalCount,
                    query.limit(),
                    query.offset(),
                    records);
        }
    }

    private static int bindFilters(PreparedStatement ps, PointDataQuery query) throws Exception {
        int index = 1;
        ps.setString(index++, query.pointKeyword());
        ps.setString(index++, query.pointLike());
        ps.setString(index++, query.shelfKeyword());
        ps.setString(index++, query.shelfLike());
        ps.setString(index++, query.areaCode());
        ps.setString(index++, query.areaCode());
        ps.setString(index++, query.relateAreaCode());
        ps.setString(index++, query.relateAreaCode());
        ps.setString(index++, query.updatedFrom());
        ps.setTimestamp(index++, query.fromTimestampOrEpoch());
        ps.setString(index++, query.updatedTo());
        ps.setTimestamp(index++, query.toTimestampOrFuture());
        return index;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
