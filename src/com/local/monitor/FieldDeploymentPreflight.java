package com.local.monitor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class FieldDeploymentPreflight {
    private static final List<String> FORBIDDEN_KEY_PARTS = List.of(
            "password", "passwd", "pwd", "token", "secret");

    private FieldDeploymentPreflight() {
    }

    public static void main(String[] args) {
        Path root = args.length == 0 ? Path.of("") : Path.of(args[0]);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int code;
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            code = run(root, capture);
        }
        String report = bytes.toString(StandardCharsets.UTF_8);
        System.out.print(report);
        try {
            Path diagnostics = root.toAbsolutePath().normalize().resolve("diagnostics");
            Files.createDirectories(diagnostics);
            Files.writeString(diagnostics.resolve("preflight-latest.txt"), report, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            System.err.println("[警告] 无法保存预检报告");
        }
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int run(Path appRoot, PrintStream output) {
        CheckState state = new CheckState(output);
        Path root = appRoot.toAbsolutePath().normalize();
        state.output().println("现场部署检查开始");

        state.file("内嵌运行时", root.resolve("runtime/bin/java.exe"));
        state.file("应用 JAR", root.resolve("ShelfPointMonitor.jar"));
        state.file("PostgreSQL 驱动", root.resolve("lib/postgresql-42.2.25.jar"));
        state.file("H2 驱动", root.resolve("lib/h2-2.2.224.jar"));
        state.file("版本文件", root.resolve("VERSION"));
        state.writableDirectory("data 目录可写", root.resolve("data"));
        state.writableDirectory("logs 目录可写", root.resolve("logs"));
        state.writableDirectory("diagnostics 目录可写", root.resolve("diagnostics"));

        Path configPath = root.resolve("data/config.properties");
        Path connectionsPath = root.resolve("data/connections.properties");
        Path groupsPath = root.resolve("data/group-config.properties");
        Properties config = loadConfiguration(state, configPath, "主配置格式");
        Properties connections = loadConfiguration(state, connectionsPath, "连接配置格式");
        Properties groups = loadConfiguration(state, groupsPath, "点位组配置格式");
        checkSensitiveKeys(state, List.of(config, connections, groups));
        checkGroupRules(state, groupsPath, groups);
        checkLocalReadOnlyQuery(state, root, config);

        if (state.failures() == 0) {
            state.output().println("现场部署检查完成：全部通过");
            return 0;
        }
        state.output().println("现场部署检查完成：存在 " + state.failures() + " 项失败");
        return 2;
    }

    private static Properties loadConfiguration(CheckState state, Path path, String label) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) {
            state.fail(label, "文件缺失");
            return properties;
        }
        try (InputStream input = Files.newInputStream(path);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            properties.load(reader);
            state.pass(label);
        } catch (Exception exception) {
            properties.clear();
            state.fail(label, "文件损坏或不可读");
        }
        return properties;
    }

    private static void checkSensitiveKeys(CheckState state, List<Properties> configurations) {
        for (Properties properties : configurations) {
            for (String key : properties.stringPropertyNames()) {
                String normalized = key.toLowerCase(Locale.ROOT);
                for (String forbidden : FORBIDDEN_KEY_PARTS) {
                    if (normalized.contains(forbidden)) {
                        state.fail("配置敏感键", "发现禁止持久化的敏感字段");
                        return;
                    }
                }
            }
        }
        state.pass("配置敏感键");
    }

    private static void checkGroupRules(CheckState state, Path groupsPath, Properties rawGroups) {
        if (rawGroups.isEmpty()) {
            state.fail("点位组配置规则", "配置不可用");
            return;
        }
        try {
            List<PointGroupDefinition> groups = new GroupConfigStore(groupsPath).load();
            if (groups.isEmpty()) {
                throw new IllegalArgumentException("empty groups");
            }
            state.pass("点位组配置规则");
        } catch (Exception exception) {
            state.fail("点位组配置规则", "存在重复、缺失或越界规则");
        }
    }

    private static void checkLocalReadOnlyQuery(CheckState state, Path root, Properties config) {
        String localPath = config.getProperty("localPath", "data/local-test-db");
        Path database = root.resolve(localPath).normalize();
        if (!database.startsWith(root)) {
            state.fail("本地 H2 只读查询", "本地库路径越界");
            return;
        }
        try (Connection connection = ReadOnlyConnectionFactory.open(
                DbConfig.localTest(database.toString(), 10), new char[0]);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("select count(*) from public.tcs_map_data")) {
            if (!connection.isReadOnly() || !rows.next()) {
                throw new IllegalStateException("read-only query did not complete");
            }
            state.pass("本地 H2 只读查询");
        } catch (Exception exception) {
            state.fail("本地 H2 只读查询", "本地验证库不可用");
        }
    }

    private static final class CheckState {
        private final PrintStream output;
        private int failures;

        private CheckState(PrintStream output) {
            this.output = output;
        }

        private PrintStream output() {
            return output;
        }

        private int failures() {
            return failures;
        }

        private void file(String label, Path path) {
            if (Files.isRegularFile(path)) {
                pass(label);
            } else {
                fail(label, "文件缺失");
            }
        }

        private void writableDirectory(String label, Path directory) {
            if (!Files.isDirectory(directory) || !Files.isWritable(directory)) {
                fail(label, "目录缺失或不可写");
                return;
            }
            try {
                Path probe = Files.createTempFile(directory, ".write-check-", ".tmp");
                Files.delete(probe);
                pass(label);
            } catch (Exception exception) {
                fail(label, "目录缺失或不可写");
            }
        }

        private void pass(String label) {
            output.println("[通过] " + label);
        }

        private void fail(String label, String reason) {
            failures++;
            output.println("[失败] " + label + "：" + reason);
        }
    }
}
