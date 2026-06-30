package com.local.monitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class LocalTestDatabase {
    private LocalTestDatabase() {
    }

    public static void reset(DbConfig config) throws Exception {
        ensureLocal(config);
        try (Connection conn = DriverManager.getConnection(config.jdbcUrl(), "sa", "")) {
            createSchema(conn);
            try (Statement st = conn.createStatement()) {
                st.execute("delete from public.shelf_point_status");
            }
            insertPoint(conn, "USE_POINT_001", null, 1, 0, "AREA_USE", "AREA_BUFFER");
            insertPoint(conn, "BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0, "AREA_BUFFER", "AREA_NEXT");
        }
    }

    public static void setScenario(DbConfig config, String scenario) throws Exception {
        ensureLocal(config);
        createIfMissing(config);
        try (Connection conn = DriverManager.getConnection(config.jdbcUrl(), "sa", "")) {
            if ("normal".equalsIgnoreCase(scenario)) {
                updatePod(conn, "USE_POINT_001", "SHELF_USE_001");
                updatePod(conn, "BACKUP_POINT_001", "SHELF_BACKUP_001");
            } else if ("missing-use".equalsIgnoreCase(scenario)) {
                updatePod(conn, "USE_POINT_001", null);
                updatePod(conn, "BACKUP_POINT_001", "SHELF_BACKUP_001");
            } else if ("missing-backup".equalsIgnoreCase(scenario)) {
                updatePod(conn, "USE_POINT_001", "SHELF_USE_001");
                updatePod(conn, "BACKUP_POINT_001", null);
            } else {
                throw new IllegalArgumentException("未知本地测试场景：" + scenario);
            }
        }
    }

    public static void createIfMissing(DbConfig config) throws Exception {
        ensureLocal(config);
        try (Connection conn = DriverManager.getConnection(config.jdbcUrl(), "sa", "")) {
            createSchema(conn);
            try (Statement st = conn.createStatement()) {
                var rs = st.executeQuery("select count(*) from public.shelf_point_status");
                rs.next();
                if (rs.getInt(1) == 0) {
                    insertPoint(conn, "USE_POINT_001", null, 1, 0, "AREA_USE", "AREA_BUFFER");
                    insertPoint(conn, "BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0, "AREA_BUFFER", "AREA_NEXT");
                }
            }
        }
    }

    private static void createSchema(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("create schema if not exists public");
            st.execute("create table if not exists public.shelf_point_status ("
                    + "point_code varchar(64) primary key,"
                    + "shelf_code varchar(64),"
                    + "shelf_status varchar(16),"
                    + "status integer,"
                    + "lock_state integer,"
                    + "area_code varchar(64),"
                    + "next_area_code varchar(64),"
                    + "updated_at timestamp,"
                    + "marked_at timestamp)");
        }
    }

    private static void insertPoint(Connection conn, String code, String podCode, int status, int indLock,
            String areaCode, String relateAreaCode) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("insert into public.shelf_point_status "
                + "(point_code,shelf_code,shelf_status,status,lock_state,area_code,next_area_code,updated_at,marked_at) "
                + "values (?,?,?,?,?,?,?,current_timestamp,current_timestamp)")) {
            ps.setString(1, code);
            ps.setString(2, podCode);
            ps.setString(3, "0");
            ps.setInt(4, status);
            ps.setInt(5, indLock);
            ps.setString(6, areaCode);
            ps.setString(7, relateAreaCode);
            ps.executeUpdate();
        }
    }

    private static void updatePod(Connection conn, String code, String podCode) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "update public.shelf_point_status set shelf_code=?, updated_at=current_timestamp where point_code=?")) {
            ps.setString(1, podCode);
            ps.setString(2, code);
            ps.executeUpdate();
        }
    }

    private static void ensureLocal(DbConfig config) {
        if (!config.isLocalTest()) {
            throw new IllegalArgumentException("只允许对本地测试库执行该操作");
        }
    }
}


