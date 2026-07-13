package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ShelfPointMonitorSelfTestTest {
    public static void main(String[] args) throws Exception {
        packagedSelfTestValidatesReleaseLayout();
        missingVersionFailsSelfTest();
        missingOperationsDocumentFailsSelfTest();
        System.out.println("ShelfPointMonitorSelfTestTest PASS");
    }

    private static void packagedSelfTestValidatesReleaseLayout() throws Exception {
        Path root = Files.createTempDirectory("spm-self-test-ok");
        writeValidPackageLayout(root);

        ShelfPointMonitorApp.runSelfTestForTest(root);

        TestSupport.assertTrue(Files.exists(root.resolve("data/local-test-db.mv.db")),
                "self-test should initialize the packaged local H2 database path");
    }

    private static void missingVersionFailsSelfTest() throws Exception {
        Path root = Files.createTempDirectory("spm-self-test-missing-version");
        writeValidPackageLayout(root);
        Files.delete(root.resolve("VERSION"));

        TestSupport.assertThrows(IllegalStateException.class,
                () -> ShelfPointMonitorApp.runSelfTestForTest(root),
                "missing VERSION should fail self-test");
    }

    private static void missingOperationsDocumentFailsSelfTest() throws Exception {
        Path root = Files.createTempDirectory("spm-self-test-missing-manual");
        writeValidPackageLayout(root);
        Files.delete(root.resolve("现场运维交付手册.md"));

        TestSupport.assertThrows(IllegalStateException.class,
                () -> ShelfPointMonitorApp.runSelfTestForTest(root),
                "missing operations manual should fail self-test");
    }

    private static void writeValidPackageLayout(Path root) throws Exception {
        Files.createDirectories(root.resolve("lib"));
        Files.createDirectories(root.resolve("data"));
        Files.createDirectories(root.resolve("logs"));
        Files.createDirectories(root.resolve("diagnostics"));
        Files.createDirectories(root.resolve("runtime/bin"));
        Files.writeString(root.resolve("VERSION"), "0.5.0-rc.1" + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(root.resolve("ShelfPointMonitor.jar"), "test jar placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("lib/postgresql-42.2.25.jar"), "test postgres jar placeholder",
                StandardCharsets.UTF_8);
        Files.writeString(root.resolve("lib/h2-2.2.224.jar"), "test h2 jar placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("runtime/bin/java.exe"), "test runtime placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("启动工具.bat"), "test launcher placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("现场部署检查.bat"), "test preflight placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("生成诊断包.bat"), "test diagnostic placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("现场运维交付手册.md"), "test manual placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("现场验收清单.md"), "test checklist placeholder", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("回滚说明.md"), "test rollback placeholder", StandardCharsets.UTF_8);

        Files.writeString(root.resolve("data/config.properties"), String.join(System.lineSeparator(),
                "host=__SITE_HOST__",
                "dbType=postgres",
                "localPath=data/local-test-db",
                "port=2345",
                "database=cms_web",
                "schema=public",
                "user=__SITE_USER__",
                "sslmode=disable",
                "intervalSeconds=10",
                "points=Use=USE_POINT_001=1;Backup 1=BACKUP_POINT_001=10"), StandardCharsets.UTF_8);

        Files.writeString(root.resolve("data/connections.properties"), String.join(System.lineSeparator(),
                "currentProfile=prod",
                "profile.count=2",
                "profile.0.id=prod",
                "profile.0.name=现场数据库",
                "profile.0.dbType=postgres",
                "profile.0.host=__SITE_HOST__",
                "profile.0.port=2345",
                "profile.0.database=cms_web",
                "profile.0.schema=public",
                "profile.0.user=__SITE_USER__",
                "profile.0.sslmode=disable",
                "profile.0.localPath=data/local-test-db",
                "profile.1.id=local",
                "profile.1.name=本地测试库",
                "profile.1.dbType=h2",
                "profile.1.host=local",
                "profile.1.port=1",
                "profile.1.database=local-test",
                "profile.1.schema=public",
                "profile.1.user=sa",
                "profile.1.sslmode=disable",
                "profile.1.localPath=data/local-test-db"), StandardCharsets.UTF_8);

        Files.writeString(root.resolve("data/group-config.properties"), String.join(System.lineSeparator(),
                "group.count=1",
                "group.0.id=sample-group-001",
                "group.0.areaName=Area A",
                "group.0.groupName=Sample Material Group",
                "group.0.materialName=Sample Material",
                "group.0.enabled=true",
                "group.0.checkIntervalSeconds=60",
                "group.0.rule.enabled=true",
                "group.0.rule.requireUsePointEmpty=true",
                "group.0.rule.minBackupAvailable=1",
                "group.0.rule.durationMinutes=5",
                "group.0.rule.backupThresholdParticipates=true",
                "group.0.point.count=2",
                "group.0.point.0.id=sample-use-001",
                "group.0.point.0.code=USE_POINT_001",
                "group.0.point.0.alias=Use",
                "group.0.point.0.role=USE",
                "group.0.point.0.enabled=true",
                "group.0.point.0.sortOrder=1",
                "group.0.point.1.id=sample-backup-001",
                "group.0.point.1.code=BACKUP_POINT_001",
                "group.0.point.1.alias=Backup 1",
                "group.0.point.1.role=BACKUP",
                "group.0.point.1.enabled=true",
                "group.0.point.1.sortOrder=2"), StandardCharsets.UTF_8);
    }

    private static final class TestSupport {
        static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertThrows(Class<? extends Throwable> expected, ThrowingRunnable runnable, String message)
                throws Exception {
            try {
                runnable.run();
            } catch (Throwable actual) {
                if (expected.isInstance(actual)) {
                    return;
                }
                throw new AssertionError(message + " expected=" + expected.getName()
                        + " actual=" + actual.getClass().getName(), actual);
            }
            throw new AssertionError(message + " expected exception " + expected.getName());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
