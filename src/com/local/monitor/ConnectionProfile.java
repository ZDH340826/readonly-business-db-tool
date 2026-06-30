package com.local.monitor;

public final class ConnectionProfile {
    private final String id;
    private final String name;
    private final String dbType;
    private final String host;
    private final int port;
    private final String database;
    private final String schema;
    private final String user;
    private final String sslMode;
    private final String localPath;

    public ConnectionProfile(String id, String name, String dbType, String host, int port, String database,
            String schema, String user, String sslMode, String localPath) {
        this.id = requireText(id, "profile id");
        this.name = requireText(name, "profile name");
        this.dbType = validateDbType(requireText(dbType, "dbType"));
        this.host = requireText(host, "host");
        this.port = port;
        this.database = requireText(database, "database");
        this.schema = requireText(schema, "schema");
        this.user = requireText(user, "user");
        this.sslMode = requireText(sslMode, "sslmode");
        this.localPath = localPath == null || localPath.trim().isEmpty() ? "data/local-test-db" : localPath.trim();
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public DbConfig toDbConfig(int intervalSeconds) {
        if ("h2".equals(dbType)) {
            return DbConfig.localTest(localPath, intervalSeconds);
        }
        return new DbConfig(host, port, database, schema, user, sslMode, intervalSeconds);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String dbType() {
        return dbType;
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

    public String localPath() {
        return localPath;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static String validateDbType(String value) {
        if (!"postgres".equals(value) && !"h2".equals(value)) {
            throw new IllegalArgumentException("dbType must be postgres or h2");
        }
        return value;
    }
}


