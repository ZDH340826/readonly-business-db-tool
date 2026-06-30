package com.local.monitor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PointRepository {
    public String testConnection(DbConfig config, char[] password) throws Exception {
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password)) {
            try (java.sql.Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("select current_database(), current_user")) {
                rs.next();
                conn.rollback();
                return "database=" + rs.getString(1) + ", user=" + rs.getString(2);
            }
        }
    }

    public List<PointRecord> fetch(DbConfig config, char[] password, List<PointDefinition> points) throws Exception {
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password)) {
            String sql = PointQuery.buildSelectSql(config.schema(), points.size());
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < points.size(); i++) {
                    ps.setString(i + 1, points.get(i).code());
                }
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
                return records;
            }
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}


