package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class TableColumnLayoutStoreTest {
    private TableColumnLayoutStoreTest() {
    }

    public static void main(String[] args) throws Exception {
        roundTripsUnicodeAndPunctuationWithoutSecrets();
        clearAffectsOnlyTheSelectedTable();
        damagedEntriesDoNotBlockTablePreview();
        System.out.println("TableColumnLayoutStoreTest PASS");
    }

    private static void roundTripsUnicodeAndPunctuationWithoutSecrets() throws Exception {
        Path path = Files.createTempDirectory("column-layout").resolve("table-column-layout.properties");
        TableColumnLayoutStore store = new TableColumnLayoutStore(path);
        List<String> columns = List.of("map_data_code", "pod_code", "状态,说明");
        store.save("公共,区", "tcs.map=数据", columns);

        TestSupport.assertEquals(
                columns,
                store.load("公共,区", "tcs.map=数据"),
                "layout must round-trip Unicode and punctuation safely");
        String text = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        TestSupport.assertFalse(text.contains("password"),
                "layout file must not contain database password fields");
        TestSupport.assertFalse(text.contains("jdbc:"),
                "layout file must not contain database connection strings");
    }

    private static void clearAffectsOnlyTheSelectedTable() throws Exception {
        Path path = Files.createTempDirectory("column-layout-clear")
                .resolve("table-column-layout.properties");
        TableColumnLayoutStore store = new TableColumnLayoutStore(path);
        store.save("public", "first_table", List.of("map_data_code", "pod_code"));
        store.save("public", "second_table", List.of("status"));

        store.clear("public", "first_table");

        TestSupport.assertEquals(List.of(), store.load("public", "first_table"),
                "restore original order must clear the selected table");
        TestSupport.assertEquals(List.of("status"), store.load("public", "second_table"),
                "clearing one table must preserve another table layout");
    }

    private static void damagedEntriesDoNotBlockTablePreview() throws Exception {
        Path path = Files.createTempDirectory("column-layout-damaged")
                .resolve("table-column-layout.properties");
        TableColumnLayoutStore store = new TableColumnLayoutStore(path);
        store.save("public", "tcs_map_data", List.of("map_data_code"));
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).startsWith("layout.")) {
                int separator = lines.get(index).indexOf('=');
                lines.set(index, lines.get(index).substring(0, separator + 1) + "%%%not-base64%%%");
            }
        }
        Files.write(path, lines, StandardCharsets.UTF_8);

        TestSupport.assertEquals(List.of(), store.load("public", "tcs_map_data"),
                "a damaged layout file must fall back to the original table order");
    }

    private static final class TestSupport {
        private static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }

        private static void assertFalse(boolean condition, String message) {
            if (condition) {
                throw new AssertionError(message);
            }
        }
    }
}
