package com.local.monitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DbMetadataRepositoryTest {
    public static void main(String[] args) throws Exception {
        localTestDatabaseMetadataCanBeBrowsed();
        previewRejectsInvalidIdentifiers();
        System.out.println("DbMetadataRepositoryTest PASS");
    }

    private static void localTestDatabaseMetadataCanBeBrowsed() throws Exception {
        Path dbPath = Files.createTempDirectory("metadata-db-test").resolve("local-test-db");
        DbConfig config = DbConfig.localTest(dbPath.toString(), 10);
        LocalTestDatabase.reset(config);
        DbMetadataRepository repository = new DbMetadataRepository();

        List<SchemaInfo> schemas = repository.listSchemas(config, new char[0]);
        List<TableInfo> tables = repository.listTables(config, new char[0], "public");
        List<ColumnInfo> columns = repository.listColumns(config, new char[0], "public", "shelf_point_status");
        TablePreview preview = repository.previewTable(config, new char[0], "public", "shelf_point_status", 100);

        TestSupport.assertTrue(containsSchema(schemas, "public"), "schemas should contain public");
        TestSupport.assertTrue(containsTable(tables, "shelf_point_status"), "tables should contain shelf_point_status");
        TestSupport.assertTrue(containsColumn(columns, "point_code"), "columns should contain point_code");
        TestSupport.assertTrue(preview.columnNames().contains("point_code"), "preview should expose column names");
        TestSupport.assertTrue(preview.rows().size() >= 2, "preview should return seeded rows");
    }

    private static void previewRejectsInvalidIdentifiers() throws Exception {
        Path dbPath = Files.createTempDirectory("metadata-invalid-test").resolve("local-test-db");
        DbConfig config = DbConfig.localTest(dbPath.toString(), 10);
        LocalTestDatabase.reset(config);
        DbMetadataRepository repository = new DbMetadataRepository();

        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> repository.previewTable(config, new char[0], "public;drop", "shelf_point_status", 100),
                "invalid schema must be rejected");
        TestSupport.assertThrows(IllegalArgumentException.class,
                () -> repository.previewTable(config, new char[0], "public", "shelf_point_status;drop", 100),
                "invalid table must be rejected");
    }

    private static boolean containsSchema(List<SchemaInfo> schemas, String schema) {
        for (SchemaInfo item : schemas) {
            if (schema.equalsIgnoreCase(item.name())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTable(List<TableInfo> tables, String table) {
        for (TableInfo item : tables) {
            if (table.equalsIgnoreCase(item.name())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsColumn(List<ColumnInfo> columns, String column) {
        for (ColumnInfo item : columns) {
            if (column.equalsIgnoreCase(item.name())) {
                return true;
            }
        }
        return false;
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertThrows(Class<? extends Throwable> type, ThrowingRunnable action, String message) {
            try {
                action.run();
            } catch (Throwable t) {
                if (type.isInstance(t)) {
                    return;
                }
                throw new AssertionError(message + " wrong exception=" + t);
            }
            throw new AssertionError(message + " no exception thrown");
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}


