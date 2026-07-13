package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExportServiceTest {
    private CsvExportServiceTest() {
    }

    public static void main(String[] args) throws Exception {
        writesUtf8BomChineseAndRfc4180Escaping();
        preventsSpreadsheetFormulaExecution();
        preservesAlreadySanitizedRowsWithoutReintroducingSecrets();
        System.out.println("CsvExportServiceTest PASS");
    }

    private static void writesUtf8BomChineseAndRfc4180Escaping() throws Exception {
        Path output = Files.createTempDirectory("csv-export").resolve("当前页.csv");
        CsvExportService.writeUtf8(
                output,
                List.of("点位编码", "说明"),
                List.of(
                        List.of("USE_POINT_001", "中文,逗号"),
                        List.of("BACKUP_POINT_001", "含\"引号\""),
                        List.of("BACKUP_POINT_002", "两行\n文本")));

        byte[] bytes = Files.readAllBytes(output);
        assertEquals((byte) 0xEF, bytes[0], "UTF-8 BOM byte 1");
        assertEquals((byte) 0xBB, bytes[1], "UTF-8 BOM byte 2");
        assertEquals((byte) 0xBF, bytes[2], "UTF-8 BOM byte 3");
        String text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertContains(text, "点位编码,说明\r\n", "header and CRLF");
        assertContains(text, "USE_POINT_001,\"中文,逗号\"", "comma escaping");
        assertContains(text, "BACKUP_POINT_001,\"含\"\"引号\"\"\"", "quote escaping");
        assertContains(text, "BACKUP_POINT_002,\"两行\n文本\"", "newline escaping");
    }

    private static void preventsSpreadsheetFormulaExecution() throws Exception {
        Path output = Files.createTempFile("csv-formula", ".csv");
        CsvExportService.writeUtf8(
                output,
                List.of("值"),
                List.of(List.of("=2+2"), List.of("+cmd"), List.of("-10+20"), List.of("@SUM(A1:A2)")));
        String text = Files.readString(output, StandardCharsets.UTF_8);
        assertContains(text, "'=2+2", "equals formula must be prefixed");
        assertContains(text, "'+cmd", "plus formula must be prefixed");
        assertContains(text, "'-10+20", "minus formula must be prefixed");
        assertContains(text, "'@SUM(A1:A2)", "at formula must be prefixed");
    }

    private static void preservesAlreadySanitizedRowsWithoutReintroducingSecrets() throws Exception {
        String safe = ShelfPointMonitorApp.sanitizeVisibleLog(
                "user=field-user password=Secret-123 jdbc:postgresql://192.0.2.10:5432/site");
        Path output = Files.createTempFile("csv-safe", ".csv");
        CsvExportService.writeUtf8(output, List.of("状态"), List.of(List.of(safe)));
        String text = Files.readString(output, StandardCharsets.UTF_8);
        assertNotContains(text, "field-user", "username must stay hidden");
        assertNotContains(text, "Secret-123", "password must stay hidden");
        assertNotContains(text, "192.0.2.10", "IP must stay hidden");
        assertNotContains(text, "jdbc:postgresql:", "JDBC URL must stay hidden");
    }

    private static void assertContains(String text, String expected, String message) {
        if (!text.contains(expected)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + text);
        }
    }

    private static void assertNotContains(String text, String unexpected, String message) {
        if (text.contains(unexpected)) {
            throw new AssertionError(message + " unexpected=" + unexpected + " actual=" + text);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
