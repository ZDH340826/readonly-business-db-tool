package com.local.monitor;

import java.nio.file.Path;

public final class DbConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String schema;
    private final String user;
    private final String sslMode;
    private final int intervalSeconds;
    private final String dbType;
    private final String localPath;

    public DbConfig(String host, int port, String database, String schema, String user, String sslMode, int intervalSeconds) {
        this("postgres", host, port, database, schema, user, sslMode, intervalSeconds, null);
    }

    private DbConfig(String dbType, String host, int port, String database, String schema, String user, String sslMode,
            int intervalSeconds, String localPath) {
        this.dbType = dbType;
        this.host = requireText(host, "host");
        this.port = port;
        this.database = requireText(database, "database");
        this.schema = PointQuery.validateIdentifier(requireText(schema, "schema"), "schema");
        this.user = requireText(user, "user");
        this.sslMode = validateSslMode(requireText(sslMode, "sslmode"));
        this.intervalSeconds = Math.max(10, intervalSeconds);
        this.localPath = localPath;
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("端口不合法");
        }
    }

    public static DbConfig localTest(String localPath, int intervalSeconds) {
        if (localPath == null || localPath.trim().isEmpty()) {
            throw new IllegalArgumentException("本地测试库路径不能为空");
        }
        return new DbConfig("h2", "local", 1, "local-test", "public", "sa", "disable", intervalSeconds,
                absoluteLocalPath(localPath.trim()));
    }

    public String dbType() {
        return dbType;
    }

    public boolean isLocalTest() {
        return "h2".equals(dbType);
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String database() {
        return database;
    }

    public String schema() {
        return schema;
    }

    public String user() {
        return user;
    }

    public String sslMode() {
        return sslMode;
    }

    public int intervalSeconds() {
        return intervalSeconds;
    }

    public String jdbcUrl() {
        if (isLocalTest()) {
            return "jdbc:h2:file:" + localPath + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        }
        return "jdbc:postgresql://" + host + ":" + port + "/" + database
                + "?sslmode=" + sslMode + "&connectTimeout=5";
    }

    public String localPath() {
        return localPath;
    }

    private static String validateSslMode(String value) {
        if (!value.equals("disable") && !value.equals("prefer") && !value.equals("require")) {
            throw new IllegalArgumentException("sslmode 只能是 disable/prefer/require");
        }
        return value;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private static String absoluteLocalPath(String value) {
        Path path = Path.of(value);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
        }
        return path.normalize().toString().replace('\\', '/');
    }
}


