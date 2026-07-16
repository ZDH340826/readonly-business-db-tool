package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WindowsPathPackagingTest {
    private WindowsPathPackagingTest() {
    }

    public static void main(String[] args) throws Exception {
        chineseAndSpacedPathSupportsFieldArtifacts();
        System.out.println("WindowsPathPackagingTest passed");
    }

    private static void chineseAndSpacedPathSupportsFieldArtifacts() throws Exception {
        Path root = Files.createTempDirectory("field-path-").resolve("现场 交付 目录");
        Files.createDirectories(root.resolve("logs"));
        Files.writeString(root.resolve("logs/monitor.log"), "连接超时 host=site-db password=dummy\n",
                StandardCharsets.UTF_8);

        DbConfig config = DbConfig.localTest(root.resolve("data/本地 测试库").toString(), 10);
        LocalTestDatabase.reset(config);
        try (Connection connection = ReadOnlyConnectionFactory.open(config, new char[0]);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("select count(*) from public.tcs_map_data")) {
            assertTrue(rows.next() && rows.getInt(1) == LocalDemoCatalog.databaseRows().size(),
                    "中文空格路径中的本地库必须完整且可只读查询");
        }

        Path csv = root.resolve("exports/当前 查询结果.csv");
        CsvExportService.writeUtf8(csv, List.of("点位", "状态"), List.of(List.of("使用位", "正常")));
        assertTrue(Files.size(csv) > 3, "中文空格路径中的 CSV 必须生成");

        Path diagnostic = DiagnosticBundleService.create(root, root.resolve("diagnostics"), "0.5.0-rc.1");
        assertTrue(Files.isRegularFile(diagnostic), "中文空格路径中的诊断包必须生成");
        try (ZipFile zip = new ZipFile(diagnostic.toFile(), StandardCharsets.UTF_8)) {
            Set<String> entries = new HashSet<>();
            var enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                entries.add(entry.getName());
                assertTrue(!entry.getName().contains(":") && !entry.getName().startsWith("/")
                                && !entry.getName().startsWith("\\"),
                        "归档条目必须使用安全相对路径");
            }
            assertEquals(Set.of("VERSION", "preflight-latest.txt", "check-log.csv", "event-log.csv",
                    "monitor.log", "environment.txt"), entries, "诊断包清单必须稳定");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，expected=" + expected + ", actual=" + actual);
        }
    }
}
