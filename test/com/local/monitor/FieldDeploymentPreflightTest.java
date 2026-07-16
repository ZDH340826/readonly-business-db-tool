package com.local.monitor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FieldDeploymentPreflightTest {
    private FieldDeploymentPreflightTest() {
    }

    public static void main(String[] args) throws Exception {
        validOfflinePackagePassesWithoutPostgres();
        missingRuntimeJarAndInvalidDirectoryFail();
        passwordKeyFailsWithoutEchoingItsValue();
        duplicateGroupIdFails();
        enabledGroupWithoutUsePointFails();
        impossibleBackupThresholdFails();
        System.out.println("FieldDeploymentPreflightTest passed");
    }

    private static void validOfflinePackagePassesWithoutPostgres() throws Exception {
        Path root = validRoot("preflight-valid-");
        Result result = run(root);

        assertEquals(0, result.code(), "完整离线包必须通过预检");
        assertContains(result.output(), "[通过] 内嵌运行时", "必须输出中文运行时结果");
        assertContains(result.output(), "[通过] 本地 H2 只读查询", "必须实测本地只读查询");
        assertNotContains(result.output(), root.toString(), "预检不得输出绝对路径");
    }

    private static void missingRuntimeJarAndInvalidDirectoryFail() throws Exception {
        Path root = validRoot("preflight-missing-");
        Files.delete(root.resolve("runtime/bin/java.exe"));
        Files.delete(root.resolve("ShelfPointMonitor.jar"));
        deleteDirectory(root.resolve("logs"));
        Files.writeString(root.resolve("logs"), "not-a-directory", StandardCharsets.UTF_8);

        Result result = run(root);

        assertTrue(result.code() != 0, "缺少运行时/JAR 或日志目录不可用必须失败");
        assertContains(result.output(), "[失败] 内嵌运行时", "必须指出运行时失败");
        assertContains(result.output(), "[失败] 应用 JAR", "必须指出 JAR 失败");
        assertContains(result.output(), "[失败] logs 目录可写", "必须指出日志目录失败");
    }

    private static void passwordKeyFailsWithoutEchoingItsValue() throws Exception {
        Path root = validRoot("preflight-secret-");
        Files.writeString(root.resolve("data/config.properties"), "\npassword=NeverEchoThisValue\n",
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);

        Result result = run(root);

        assertTrue(result.code() != 0, "出现密码键必须失败");
        assertContains(result.output(), "[失败] 配置敏感键", "必须指出敏感键失败");
        assertNotContains(result.output(), "NeverEchoThisValue", "不得回显密码值");
    }

    private static void duplicateGroupIdFails() throws Exception {
        Path root = validRoot("preflight-duplicate-");
        Files.writeString(root.resolve("data/group-config.properties"), groupConfig(2, true, 1, true),
                StandardCharsets.UTF_8);

        Result result = run(root);

        assertTrue(result.code() != 0, "重复 groupId 必须失败");
        assertContains(result.output(), "[失败] 点位组配置规则", "必须指出组规则失败");
    }

    private static void enabledGroupWithoutUsePointFails() throws Exception {
        Path root = validRoot("preflight-no-use-");
        Files.writeString(root.resolve("data/group-config.properties"), groupConfig(1, false, 1, false),
                StandardCharsets.UTF_8);

        Result result = run(root);

        assertTrue(result.code() != 0, "启用组缺少使用位必须失败");
        assertContains(result.output(), "[失败] 点位组配置规则", "必须指出组规则失败");
    }

    private static void impossibleBackupThresholdFails() throws Exception {
        Path root = validRoot("preflight-threshold-");
        Files.writeString(root.resolve("data/group-config.properties"), groupConfig(1, true, 2, false),
                StandardCharsets.UTF_8);

        Result result = run(root);

        assertTrue(result.code() != 0, "备用位阈值超过数量必须失败");
        assertContains(result.output(), "[失败] 点位组配置规则", "必须指出组规则失败");
    }

    private static Path validRoot(String prefix) throws Exception {
        Path root = Files.createTempDirectory(prefix).resolve("现场 包");
        Files.createDirectories(root.resolve("runtime/bin"));
        Files.createDirectories(root.resolve("lib"));
        Files.createDirectories(root.resolve("data"));
        Files.createDirectories(root.resolve("logs"));
        Files.createDirectories(root.resolve("diagnostics"));
        Files.writeString(root.resolve("runtime/bin/java.exe"), "placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("ShelfPointMonitor.jar"), "placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("lib/postgresql-42.2.25.jar"), "placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("lib/h2-2.2.224.jar"), "placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("VERSION"), "0.5.0-rc.1\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("data/config.properties"), String.join("\n",
                "host=192.0.2.10", "dbType=postgres", "localPath=data/local-test-db", "port=5432",
                "database=example", "schema=public", "user=readonly_placeholder", "sslmode=disable",
                "intervalSeconds=10", "points=Use=USE_POINT_001=1"), StandardCharsets.UTF_8);
        Files.writeString(root.resolve("data/connections.properties"), String.join("\n",
                "currentProfile=local", "profile.count=1", "profile.0.id=local", "profile.0.name=本地测试库",
                "profile.0.dbType=h2", "profile.0.host=local", "profile.0.port=1",
                "profile.0.database=local-test", "profile.0.schema=public", "profile.0.user=sa",
                "profile.0.sslmode=disable", "profile.0.localPath=data/local-test-db"), StandardCharsets.UTF_8);
        Files.writeString(root.resolve("data/group-config.properties"), groupConfig(1, true, 1, false),
                StandardCharsets.UTF_8);
        LocalTestDatabase.reset(DbConfig.localTest(root.resolve("data/local-test-db").toString(), 10));
        return root;
    }

    private static String groupConfig(int count, boolean useEnabled, int threshold, boolean duplicateId) {
        StringBuilder text = new StringBuilder("group.count=").append(count).append('\n');
        for (int i = 0; i < count; i++) {
            String prefix = "group." + i + ".";
            text.append(prefix).append("id=").append(duplicateId ? "duplicate" : "group-").append(duplicateId ? "" : i).append('\n');
            text.append(prefix).append("areaName=Area\n");
            text.append(prefix).append("groupName=Group\n");
            text.append(prefix).append("materialName=Material\n");
            text.append(prefix).append("enabled=true\n");
            text.append(prefix).append("checkIntervalSeconds=60\n");
            text.append(prefix).append("rule.enabled=true\n");
            text.append(prefix).append("rule.requireUsePointEmpty=true\n");
            text.append(prefix).append("rule.minBackupAvailable=").append(threshold).append('\n');
            text.append(prefix).append("rule.durationMinutes=5\n");
            text.append(prefix).append("rule.backupThresholdParticipates=true\n");
            text.append(prefix).append("point.count=2\n");
            text.append(prefix).append("point.0.id=use-").append(i).append('\n');
            text.append(prefix).append("point.0.code=USE_POINT_").append(i).append('\n');
            text.append(prefix).append("point.0.alias=Use\n");
            text.append(prefix).append("point.0.role=USE\n");
            text.append(prefix).append("point.0.enabled=").append(useEnabled).append('\n');
            text.append(prefix).append("point.0.sortOrder=1\n");
            text.append(prefix).append("point.1.id=backup-").append(i).append('\n');
            text.append(prefix).append("point.1.code=BACKUP_POINT_").append(i).append('\n');
            text.append(prefix).append("point.1.alias=Backup\n");
            text.append(prefix).append("point.1.role=BACKUP\n");
            text.append(prefix).append("point.1.enabled=true\n");
            text.append(prefix).append("point.1.sortOrder=2\n");
        }
        return text.toString();
    }

    private static Result run(Path root) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int code;
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            code = FieldDeploymentPreflight.run(root, output);
        }
        return new Result(code, bytes.toString(StandardCharsets.UTF_8));
    }

    private static void deleteDirectory(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertContains(String text, String expected, String message) {
        if (!text.contains(expected)) {
            throw new AssertionError(message + "，output=" + text);
        }
    }

    private static void assertNotContains(String text, String unexpected, String message) {
        if (text.contains(unexpected)) {
            throw new AssertionError(message + "，unexpected=" + unexpected);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，expected=" + expected + ", actual=" + actual);
        }
    }

    private record Result(int code, String output) {
    }
}
