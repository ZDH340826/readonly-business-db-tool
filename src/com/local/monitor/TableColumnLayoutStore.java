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
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class TableColumnLayoutStore {
    private static final String KEY_PREFIX = "layout.";
    private final Path path;

    public TableColumnLayoutStore(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("表格布局文件路径不能为空");
        }
        this.path = path;
    }

    public List<String> load(String schema, String table) {
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            Properties properties = readProperties();
            String value = properties.getProperty(key(schema, table));
            if (value == null || value.isBlank()) {
                return List.of();
            }
            List<String> columns = new ArrayList<>();
            for (String encoded : value.split(",", -1)) {
                if (encoded.isBlank()) {
                    return List.of();
                }
                columns.add(decode(encoded));
            }
            return List.copyOf(columns);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public void save(String schema, String table, List<String> columns) throws IOException {
        List<String> safeColumns = safeColumns(columns);
        Properties properties = readPropertiesOrEmpty();
        String key = key(schema, table);
        if (safeColumns.isEmpty()) {
            properties.remove(key);
        } else {
            properties.setProperty(key, encodeColumns(safeColumns));
        }
        writeProperties(properties);
    }

    public void clear(String schema, String table) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Properties properties = readPropertiesOrEmpty();
        properties.remove(key(schema, table));
        writeProperties(properties);
    }

    private Properties readPropertiesOrEmpty() {
        try {
            return readProperties();
        } catch (Exception ignored) {
            return new Properties();
        }
    }

    private Properties readProperties() throws IOException {
        Properties properties = new Properties();
        if (!Files.exists(path)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(path);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private void writeProperties(Properties properties) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream output = Files.newOutputStream(path);
                OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            properties.store(writer, "Table column layout. No database content or secrets.");
        }
    }

    private static String key(String schema, String table) {
        return KEY_PREFIX + encode(requiredName(schema, "Schema")) + "." + encode(requiredName(table, "表名"));
    }

    private static List<String> safeColumns(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String column : columns) {
            unique.add(requiredName(column, "列名"));
        }
        if (unique.size() > PinnedColumnLayout.MAX_PINNED_COLUMNS) {
            throw new IllegalArgumentException("固定列太多，请先取消一列");
        }
        return List.copyOf(unique);
    }

    private static String requiredName(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value;
    }

    private static String encodeColumns(List<String> columns) {
        List<String> encoded = new ArrayList<>();
        for (String column : columns) {
            encoded.add(encode(column));
        }
        return String.join(",", encoded);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
