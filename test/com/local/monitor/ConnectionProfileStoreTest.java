package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConnectionProfileStoreTest {
    public static void main(String[] args) throws Exception {
        savesMultipleProfilesWithoutPassword();
        defaultsContainPostgresAndLocalProfiles();
        System.out.println("ConnectionProfileStoreTest PASS");
    }

    private static void savesMultipleProfilesWithoutPassword() throws Exception {
        Path configPath = Files.createTempDirectory("connection-profile-test").resolve("connections.properties");
        ConnectionProfileStore store = new ConnectionProfileStore(configPath);

        store.save("local", List.of(
                new ConnectionProfile("prod", "示例 PostgreSQL", "postgres", "127.0.0.1", 5432,
                        "example_db", "public", "readonly_user", "disable", "data/local-test-db"),
                new ConnectionProfile("local", "本地测试库", "h2", "local", 1,
                        "local-test", "public", "sa", "disable", "data/local-test-db")));

        ConnectionProfileStore.StoredProfiles loaded = store.load();
        String raw = Files.readString(configPath);

        TestSupport.assertEquals("local", loaded.currentId(), "current profile should persist");
        TestSupport.assertEquals(2, loaded.profiles().size(), "two profiles should load");
        TestSupport.assertEquals("示例 PostgreSQL", loaded.profiles().get(0).name(), "profile name should load");
        TestSupport.assertEquals("h2", loaded.profiles().get(1).dbType(), "local profile type should load");
        TestSupport.assertFalse(raw.toLowerCase().contains("password"), "connection file must not contain password keys");
    }

    private static void defaultsContainPostgresAndLocalProfiles() throws Exception {
        Path configPath = Files.createTempDirectory("connection-profile-default-test").resolve("connections.properties");
        ConnectionProfileStore.StoredProfiles loaded = new ConnectionProfileStore(configPath).load();

        TestSupport.assertEquals(2, loaded.profiles().size(), "default profiles should include postgres and local");
        TestSupport.assertEquals("prod", loaded.currentId(), "default current profile should be prod");
        TestSupport.assertEquals("postgres", loaded.profiles().get(0).dbType(), "first default should be postgres");
        TestSupport.assertEquals("现场数据库", loaded.profiles().get(0).name(), "default postgres should be site profile");
        TestSupport.assertEquals("__SITE_HOST__", loaded.profiles().get(0).host(), "default host should be placeholder");
        TestSupport.assertEquals(2345, loaded.profiles().get(0).port(), "default site port should be 2345");
        TestSupport.assertEquals("cms_web", loaded.profiles().get(0).database(), "default site database should be cms_web");
        TestSupport.assertEquals("public", loaded.profiles().get(0).schema(), "default site schema should be public");
        TestSupport.assertEquals("__SITE_USER__", loaded.profiles().get(0).user(), "default user should be placeholder");
        TestSupport.assertEquals("disable", loaded.profiles().get(0).sslMode(), "default sslmode should be disable");
        TestSupport.assertEquals("h2", loaded.profiles().get(1).dbType(), "second default should be local h2");
    }

    private static final class TestSupport {
        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        static void assertFalse(boolean condition, String message) {
            if (condition) {
                throw new AssertionError(message);
            }
        }
    }
}


