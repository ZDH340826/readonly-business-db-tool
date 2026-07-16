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
                st.execute("delete from public.tcs_map_data");
            }
            insertDefaultPoints(conn);
        }
    }

    public static void setScenario(DbConfig config, String scenario) throws Exception {
        ensureLocal(config);
        createIfMissing(config);
        try (Connection conn = DriverManager.getConnection(config.jdbcUrl(), "sa", "")) {
            if ("normal".equalsIgnoreCase(scenario)) {
                updatePod(conn, "USE_POINT_001", "SHELF_USE_001");
                updatePod(conn, "USE_POINT_002", "SHELF_USE_002");
                updatePod(conn, "BACKUP_POINT_001", "SHELF_BACKUP_001");
                updatePod(conn, "BACKUP_POINT_002", "SHELF_BACKUP_002");
                updatePod(conn, "BACKUP_POINT_003", "SHELF_BACKUP_003");
                updatePod(conn, "BACKUP_POINT_004", "SHELF_BACKUP_004");
            } else if ("missing-use".equalsIgnoreCase(scenario)) {
                updatePod(conn, "USE_POINT_001", null);
                updatePod(conn, "USE_POINT_002", null);
                updatePod(conn, "BACKUP_POINT_001", "SHELF_BACKUP_001");
                updatePod(conn, "BACKUP_POINT_002", "SHELF_BACKUP_002");
                updatePod(conn, "BACKUP_POINT_003", null);
                updatePod(conn, "BACKUP_POINT_004", null);
            } else if ("missing-backup".equalsIgnoreCase(scenario)) {
                updatePod(conn, "USE_POINT_001", "SHELF_USE_001");
                updatePod(conn, "BACKUP_POINT_001", null);
                updatePod(conn, "BACKUP_POINT_002", "SHELF_BACKUP_002");
                updatePod(conn, "BACKUP_POINT_003", "SHELF_BACKUP_003");
                updatePod(conn, "BACKUP_POINT_004", "SHELF_BACKUP_004");
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
                var rs = st.executeQuery("select count(*) from public.tcs_map_data");
                rs.next();
                if (rs.getInt(1) == 0) {
                    insertDefaultPoints(conn);
                }
            }
        }
    }

    private static void createSchema(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("create schema if not exists public");
            st.execute("create table if not exists public.tcs_map_data ("
                    + "map_data_code varchar(64) primary key,"
                    + "pod_code varchar(64),"
                    + "pod_status varchar(16),"
                    + "status integer,"
                    + "ind_lock integer,"
                    + "area_code varchar(64),"
                    + "relate_area_code varchar(64),"
                    + "date_chg timestamp)");
        }
    }

    private static void insertDefaultPoints(Connection conn) throws Exception {
        for (LocalDemoCatalog.DatabaseRow row : LocalDemoCatalog.databaseRows()) {
            insertPoint(conn, row);
        }
    }

    private static void insertPoint(Connection conn, LocalDemoCatalog.DatabaseRow row) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("insert into public.tcs_map_data "
                + "(map_data_code,pod_code,pod_status,status,ind_lock,area_code,relate_area_code,date_chg) "
                + "values (?,?,?,?,?,?,?,current_timestamp)")) {
            ps.setString(1, row.code());
            ps.setString(2, row.podCode());
            ps.setString(3, row.podStatus());
            ps.setInt(4, row.status());
            ps.setInt(5, row.lock());
            ps.setString(6, row.areaCode());
            ps.setString(7, row.relateAreaCode());
            ps.executeUpdate();
        }
    }

    private static void updatePod(Connection conn, String code, String podCode) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "update public.tcs_map_data set pod_code=?, date_chg=current_timestamp where map_data_code=?")) {
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
