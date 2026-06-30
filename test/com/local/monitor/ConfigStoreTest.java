package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigStoreTest {
    public static void main(String[] args) throws Exception {
        savesAndLoadsPerPointIntervals();
        oldPointFormatDefaultsToFiveMinutes();
        System.out.println("ConfigStoreTest PASS");
    }

    private static void savesAndLoadsPerPointIntervals() throws Exception {
        Path configPath = Files.createTempDirectory("shelf-config-test").resolve("config.properties");
        ConfigStore store = new ConfigStore(configPath);
        DbConfig config = new DbConfig("127.0.0.1", 5432, "example_db", "public", "reader", "disable", 10);

        store.save(config, List.of(
                new PointDefinition("USE_POINT_001", "use", 1),
                new PointDefinition("BACKUP_POINT_001", "backup", 10)));

        ConfigStore.StoredConfig loaded = store.load();
        TestSupport.assertEquals(2, loaded.points.size(), "two points should load");
        TestSupport.assertEquals(1, loaded.points.get(0).intervalMinutes(), "use point interval should load");
        TestSupport.assertEquals(10, loaded.points.get(1).intervalMinutes(), "backup point interval should load");
    }

    private static void oldPointFormatDefaultsToFiveMinutes() throws Exception {
        Path configPath = Files.createTempDirectory("shelf-config-old-test").resolve("config.properties");
        Files.writeString(configPath, "points=use=USE_POINT_001;backup=BACKUP_POINT_001\n");

        ConfigStore.StoredConfig loaded = new ConfigStore(configPath).load();

        TestSupport.assertEquals(5, loaded.points.get(0).intervalMinutes(), "old config format should default interval");
        TestSupport.assertEquals(5, loaded.points.get(1).intervalMinutes(), "old config format should default interval");
    }

    private static final class TestSupport {
        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }
    }
}


