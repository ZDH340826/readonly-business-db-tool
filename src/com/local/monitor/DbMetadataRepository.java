package com.local.monitor;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DbMetadataRepository {
    public List<SchemaInfo> listSchemas(DbConfig config, char[] password) throws Exception {
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password)) {
            DatabaseMetaData metadata = conn.getMetaData();
            Map<String, SchemaInfo> schemas = new LinkedHashMap<>();
            try (ResultSet rs = metadata.getSchemas()) {
                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    if (schema != null && !isSystemSchema(schema)) {
                        schemas.put(schema.toLowerCase(Locale.ROOT), new SchemaInfo(schema));
                    }
                }
            }
            conn.rollback();
            List<SchemaInfo> result = new ArrayList<>(schemas.values());
            result.sort(Comparator.comparing(SchemaInfo::name, String.CASE_INSENSITIVE_ORDER));
            return result;
        }
    }

    public List<TableInfo> listTables(DbConfig config, char[] password, String schema) throws Exception {
        String safeSchema = PointQuery.validateIdentifier(schema, "schema");
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password)) {
            DatabaseMetaData metadata = conn.getMetaData();
            Map<String, TableInfo> tables = new LinkedHashMap<>();
            for (String pattern : schemaPatterns(safeSchema)) {
                try (ResultSet rs = metadata.getTables(null, pattern, "%", new String[] {"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        String tableSchema = rs.getString("TABLE_SCHEM");
                        String tableName = rs.getString("TABLE_NAME");
                        String tableType = rs.getString("TABLE_TYPE");
                        if (tableSchema != null && tableName != null && safeSchema.equalsIgnoreCase(tableSchema)) {
                            tables.put(tableName.toLowerCase(Locale.ROOT),
                                    new TableInfo(tableSchema, tableName, tableType));
                        }
                    }
                }
            }
            conn.rollback();
            List<TableInfo> result = new ArrayList<>(tables.values());
            result.sort(Comparator.comparing(TableInfo::name, String.CASE_INSENSITIVE_ORDER));
            return result;
        }
    }

    public List<ColumnInfo> listColumns(DbConfig config, char[] password, String schema, String table) throws Exception {
        String safeSchema = PointQuery.validateIdentifier(schema, "schema");
        String safeTable = PointQuery.validateIdentifier(table, "table");
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password)) {
            DatabaseMetaData metadata = conn.getMetaData();
            Map<String, ColumnInfo> columns = new LinkedHashMap<>();
            for (String schemaPattern : schemaPatterns(safeSchema)) {
                for (String tablePattern : schemaPatterns(safeTable)) {
                    try (ResultSet rs = metadata.getColumns(null, schemaPattern, tablePattern, "%")) {
                        while (rs.next()) {
                            String columnName = rs.getString("COLUMN_NAME");
                            if (columnName != null) {
                                columns.put(columnName.toLowerCase(Locale.ROOT), new ColumnInfo(
                                        columnName,
                                        rs.getString("TYPE_NAME"),
                                        rs.getInt("COLUMN_SIZE"),
                                        rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable));
                            }
                        }
                    }
                }
            }
            conn.rollback();
            return new ArrayList<>(columns.values());
        }
    }

    public TablePreview previewTable(DbConfig config, char[] password, String schema, String table, int limit)
            throws Exception {
        String safeSchema = PointQuery.validateIdentifier(schema, "schema");
        String safeTable = PointQuery.validateIdentifier(table, "table");
        int safeLimit = Math.max(1, Math.min(500, limit));
        try (Connection conn = ReadOnlyConnectionFactory.open(config, password);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("select * from " + safeSchema + "." + safeTable + " limit " + safeLimit)) {
            ResultSetMetaData metadata = rs.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                columns.add(metadata.getColumnLabel(i));
            }
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    row.add(rs.getString(i));
                }
                rows.add(row);
            }
            conn.rollback();
            return new TablePreview(columns, rows);
        }
    }

    private static List<String> schemaPatterns(String value) {
        List<String> patterns = new ArrayList<>();
        addUnique(patterns, value);
        addUnique(patterns, value.toLowerCase(Locale.ROOT));
        addUnique(patterns, value.toUpperCase(Locale.ROOT));
        return patterns;
    }

    private static void addUnique(List<String> values, String value) {
        for (String existing : values) {
            if (existing.equals(value)) {
                return;
            }
        }
        values.add(value);
    }

    private static boolean isSystemSchema(String schema) {
        String s = schema.toLowerCase(Locale.ROOT);
        return s.equals("information_schema")
                || s.equals("pg_catalog")
                || s.startsWith("pg_toast")
                || s.startsWith("pg_temp");
    }
}


