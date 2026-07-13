package com.local.monitor;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DiagnosticBundleServiceTest {
    private static final List<String> EXPECTED_ENTRIES = List.of(
            "VERSION",
            "check-log.csv",
            "environment.txt",
            "event-log.csv",
            "monitor.log",
            "preflight-latest.txt");

    private DiagnosticBundleServiceTest() {
    }

    public static void main(String[] args) throws Exception {
        bundleContainsOnlyAllowlistedSanitizedEntries();
        bundleNeverIncludesConfigurationDatabaseJarOrRuntime();
        System.out.println("DiagnosticBundleServiceTest PASS");
    }

    private static void bundleContainsOnlyAllowlistedSanitizedEntries() throws Exception {
        Path root = Files.createTempDirectory("diagnostic-root");
        Path logs = Files.createDirectories(root.resolve("logs"));
        Path diagnostics = Files.createDirectories(root.resolve("diagnostics"));
        String unsafe = "查询失败 user=readonly_user password=Secret-123 "
                + "jdbc:postgresql://192.0.2.10:5432/site host=database.example.internal "
                + "C:/field/site/config.properties at demo.Driver.run(Driver.java:42)";
        Files.writeString(logs.resolve("check-log.csv"), "checked_at,status,message\n2026-07-14,QUERY_FAILED," + unsafe);
        Files.writeString(logs.resolve("event-log.csv"), "event_at,type,message\n2026-07-14,QUERY_FAILED," + unsafe);
        Files.writeString(logs.resolve("monitor.log"), unsafe);
        Files.writeString(diagnostics.resolve("preflight-latest.txt"), "[失败] " + unsafe);

        Path bundle = DiagnosticBundleService.create(root, diagnostics, "0.5.0-rc.1");
        ZipContents contents = readZip(bundle);

        assertEquals(EXPECTED_ENTRIES, contents.entryNames(), "diagnostic allowlist");
        String text = contents.allText();
        assertContains(text, "0.5.0-rc.1", "bundle version");
        assertContains(text, "查询失败", "operator category should remain");
        assertNotContains(text, "readonly_user", "database username");
        assertNotContains(text, "Secret-123", "password");
        assertNotContains(text, "192.0.2.10", "IP address");
        assertNotContains(text, "database.example.internal", "hostname");
        assertNotContains(text, "jdbc:postgresql:", "JDBC URL");
        assertNotContains(text, "C:/field/site", "absolute path");
        assertNotContains(text, "Driver.java:42", "source location");
        assertNotContains(text, root.toString(), "diagnostic root path");
    }

    private static void bundleNeverIncludesConfigurationDatabaseJarOrRuntime() throws Exception {
        Path root = Files.createTempDirectory("diagnostic-denylist");
        Files.createDirectories(root.resolve("logs"));
        Files.createDirectories(root.resolve("data"));
        Files.createDirectories(root.resolve("runtime/bin"));
        Files.writeString(root.resolve("data/connections.properties"), "password=MustNotLeak");
        Files.writeString(root.resolve("data/group-config.properties"), "groupId=secret-group");
        Files.writeString(root.resolve("data/local-test-db.mv.db"), "database bytes");
        Files.writeString(root.resolve("ShelfPointMonitor.jar"), "jar bytes");
        Files.writeString(root.resolve("runtime/bin/java.exe"), "runtime bytes");

        Path bundle = DiagnosticBundleService.create(root, root.resolve("diagnostics"), "0.5.0-rc.1");
        ZipContents contents = readZip(bundle);
        String names = String.join(" ", contents.entryNames());
        assertNotContains(names, "connections.properties", "connection config entry");
        assertNotContains(names, "group-config.properties", "group config entry");
        assertNotContains(names, ".mv.db", "H2 database entry");
        assertNotContains(names, ".jar", "JAR entry");
        assertNotContains(names, "runtime", "runtime entry");
        assertNotContains(contents.allText(), "MustNotLeak", "configuration contents");
    }

    private static ZipContents readZip(Path zip) throws Exception {
        List<String> names = new ArrayList<>();
        StringBuilder allText = new StringBuilder();
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                names.add(entry.getName());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                input.transferTo(bytes);
                allText.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8)).append('\n');
            }
        }
        names.sort(String::compareTo);
        return new ZipContents(List.copyOf(names), allText.toString());
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

    private record ZipContents(List<String> entryNames, String allText) {
    }
}
