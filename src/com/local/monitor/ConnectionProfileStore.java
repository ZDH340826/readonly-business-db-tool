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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class ConnectionProfileStore {
    private final Path path;

    public ConnectionProfileStore(Path path) {
        this.path = path;
    }

    public StoredProfiles load() {
        if (!Files.exists(path)) {
            return defaultProfiles();
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return defaultProfiles();
        }

        int count = parseInt(p.getProperty("profile.count"), 0);
        List<ConnectionProfile> profiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = "profile." + i + ".";
            String id = p.getProperty(prefix + "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            profiles.add(new ConnectionProfile(
                    id,
                    p.getProperty(prefix + "name", id),
                    p.getProperty(prefix + "dbType", "postgres"),
                    p.getProperty(prefix + "host", "__SITE_HOST__"),
                    parseInt(p.getProperty(prefix + "port"), 2345),
                    p.getProperty(prefix + "database", "cms_web"),
                    p.getProperty(prefix + "schema", "public"),
                    p.getProperty(prefix + "user", "__SITE_USER__"),
                    p.getProperty(prefix + "sslmode", "disable"),
                    p.getProperty(prefix + "localPath", "data/local-test-db")));
        }
        if (profiles.isEmpty()) {
            return defaultProfiles();
        }
        String currentId = p.getProperty("currentProfile", profiles.get(0).id());
        if (findById(profiles, currentId) == null) {
            currentId = profiles.get(0).id();
        }
        return new StoredProfiles(currentId, profiles);
    }

    public void save(String currentId, List<ConnectionProfile> profiles) throws IOException {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("at least one connection profile is required");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties p = new Properties();
        p.setProperty("currentProfile", currentId == null ? profiles.get(0).id() : currentId);
        p.setProperty("profile.count", String.valueOf(profiles.size()));
        for (int i = 0; i < profiles.size(); i++) {
            ConnectionProfile profile = profiles.get(i);
            String prefix = "profile." + i + ".";
            p.setProperty(prefix + "id", profile.id());
            p.setProperty(prefix + "name", profile.name());
            p.setProperty(prefix + "dbType", profile.dbType());
            p.setProperty(prefix + "host", profile.host());
            p.setProperty(prefix + "port", String.valueOf(profile.port()));
            p.setProperty(prefix + "database", profile.database());
            p.setProperty(prefix + "schema", profile.schema());
            p.setProperty(prefix + "user", profile.user());
            p.setProperty(prefix + "sslmode", profile.sslMode());
            p.setProperty(prefix + "localPath", profile.localPath());
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "Connection profiles.");
        }
    }

    private static StoredProfiles defaultProfiles() {
        return new StoredProfiles("prod", List.of(
                new ConnectionProfile("prod", "现场数据库", "postgres", "__SITE_HOST__", 2345,
                        "cms_web", "public", "__SITE_USER__", "disable", "data/local-test-db"),
                new ConnectionProfile("local", "本地测试库", "h2", "local", 1,
                        "local-test", "public", "sa", "disable", "data/local-test-db")));
    }

    private static ConnectionProfile findById(List<ConnectionProfile> profiles, String id) {
        for (ConnectionProfile profile : profiles) {
            if (profile.id().equals(id)) {
                return profile;
            }
        }
        return null;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static final class StoredProfiles {
        private final String currentId;
        private final List<ConnectionProfile> profiles;

        StoredProfiles(String currentId, List<ConnectionProfile> profiles) {
            this.currentId = currentId;
            this.profiles = Collections.unmodifiableList(new ArrayList<>(profiles));
        }

        public String currentId() {
            return currentId;
        }

        public List<ConnectionProfile> profiles() {
            return profiles;
        }
    }
}


