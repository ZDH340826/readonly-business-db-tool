package com.local.monitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public final class ReadOnlyConnectionFactory {
    private ReadOnlyConnectionFactory() {
    }

    public static Connection open(DbConfig config, char[] password) throws Exception {
        if (config.isLocalTest()) {
            LocalTestDatabase.createIfMissing(config);
        }
        Class.forName(config.isLocalTest() ? "org.h2.Driver" : "org.postgresql.Driver");
        Properties props = new Properties();
        props.setProperty("user", config.isLocalTest() ? "sa" : config.user());
        props.setProperty("password", config.isLocalTest() ? "" : new String(password));
        String jdbcUrl = config.isLocalTest()
                ? config.jdbcUrl() + ";ACCESS_MODE_DATA=r"
                : config.jdbcUrl();
        Connection conn = DriverManager.getConnection(jdbcUrl, props);
        conn.setReadOnly(true);
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            if (!config.isLocalTest()) {
                st.execute("SET TRANSACTION READ ONLY");
                st.execute("SET statement_timeout='8s'");
            }
        }
        return conn;
    }
}


