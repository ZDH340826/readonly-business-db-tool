package com.local.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ConfigStore {
    private final Path path;

    public ConfigStore(Path path) {
        this.path = path;
    }

    public StoredConfig load() {
        Properties p = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                p.clear();
            }
        }
        return new StoredConfig(
                p.getProperty("host", "127.0.0.1"),
                parseInt(p.getProperty("port"), 5432),
                p.getProperty("database", "example_db"),
                p.getProperty("schema", "public"),
                p.getProperty("user", "readonly_user"),
                p.getProperty("sslmode", "disable"),
                parseInt(p.getProperty("intervalSeconds"), 10),
                p.getProperty("dbType", "postgres"),
                p.getProperty("localPath", "data/local-test-db"),
                parsePoints(p.getProperty("points", "使用位=USE_POINT_001=1;备用位=BACKUP_POINT_001=10")));
    }

    public void save(DbConfig config, List<PointDefinition> points) throws IOException {
        Files.createDirectories(path.getParent());
        Properties p = new Properties();
        p.setProperty("host", config.host());
        p.setProperty("dbType", config.dbType());
        p.setProperty("localPath", config.localPath() == null ? "data/local-test-db" : config.localPath());
        p.setProperty("port", String.valueOf(config.port()));
        p.setProperty("database", config.database());
        p.setProperty("schema", config.schema());
        p.setProperty("user", config.user());
        p.setProperty("sslmode", config.sslMode());
        p.setProperty("intervalSeconds", String.valueOf(config.intervalSeconds()));
        p.setProperty("points", formatPoints(points));
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                    "Shelf point monitor config. Password is intentionally not saved.");
        }
    }

    private static List<PointDefinition> parsePoints(String raw) {
        List<PointDefinition> points = new ArrayList<>();
        for (String item : raw.split(";")) {
            String[] parts = item.split("=");
            if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                int interval = PointDefinition.DEFAULT_INTERVAL_MINUTES;
                if (parts.length >= 3 && !parts[2].isBlank()) {
                    interval = parseInt(parts[2], PointDefinition.DEFAULT_INTERVAL_MINUTES);
                }
                points.add(new PointDefinition(parts[1].trim(), parts[0].trim(), interval));
            }
        }
        return points;
    }

    private static String formatPoints(List<PointDefinition> points) {
        StringBuilder builder = new StringBuilder();
        for (PointDefinition point : points) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(point.alias())
                    .append('=')
                    .append(point.code())
                    .append('=')
                    .append(point.intervalMinutes());
        }
        return builder.toString();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static final class StoredConfig {
        public final String host;
        public final int port;
        public final String database;
        public final String schema;
        public final String user;
        public final String sslMode;
        public final int intervalSeconds;
        public final String dbType;
        public final String localPath;
        public final List<PointDefinition> points;

        StoredConfig(String host, int port, String database, String schema, String user, String sslMode,
                int intervalSeconds, String dbType, String localPath, List<PointDefinition> points) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.schema = schema;
            this.user = user;
            this.sslMode = sslMode;
            this.intervalSeconds = intervalSeconds;
            this.dbType = dbType;
            this.localPath = localPath;
            this.points = points;
        }
    }
}


