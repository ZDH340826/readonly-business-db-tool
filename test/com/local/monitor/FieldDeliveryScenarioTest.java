package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FieldDeliveryScenarioTest {
    private FieldDeliveryScenarioTest() {
    }

    public static void main(String[] args) throws Exception {
        requiredScenarioEvidenceRemainsMapped();
        damagedConfigurationFallsBackWithoutPartialData();
        damagedGroupAndConnectionConfigurationFallsBackSafely();
        System.out.println("FieldDeliveryScenarioTest passed");
    }

    private static void requiredScenarioEvidenceRemainsMapped() throws Exception {
        List<ScenarioEvidence> scenarios = List.of(
                evidence("本地测试库正常场景", "LocalTestDatabaseTest", "seededDatabaseSupportsGroupShortageScenario"),
                evidence("使用位缺料场景", "GroupMonitorLogicTest", "firstMatchingRuleStartsPendingWithZeroElapsedSeconds"),
                evidence("备用位不足场景", "GroupMonitorLogicTest", "matchingRuleCreatesActiveAlertAfterFiveRealMinutes"),
                evidence("健康组正常", "GroupMonitorLogicTest", "healthyGroupCreatesNormalEvaluation"),
                evidence("首次异常进入观察", "GroupMonitorLogicTest", "firstMatchingRuleStartsPendingWithZeroElapsedSeconds"),
                evidence("未满持续时间保持观察", "GroupMonitorLogicTest", "matchingRuleStaysPendingAfterFourRealMinutes"),
                evidence("满持续时间触发报警", "GroupMonitorLogicTest", "matchingRuleCreatesActiveAlertAfterFiveRealMinutes"),
                evidence("同次短缺只弹一次", "GroupMonitorLogicTest", "activeDialogIsShownOnlyOnceDuringSameShortage"),
                evidence("已关注抑制重复弹窗", "GroupMonitorLogicTest", "acknowledgedAlertStaysSuppressedUntilRecovery"),
                evidence("补料恢复清理状态", "GroupMonitorLogicTest", "recoveredGroupClearsRuntimeState"),
                evidence("查询失败独立语义", "GroupMonitorLogicTest", "queryFailureClearsShortageTimingAndDoesNotAlert"),
                evidence("查询恢复后短缺重新计时", "GroupMonitorLogicTest", "recoveredShortageAfterQueryFailureStartsTimingAgain"),
                evidence("查询恢复后健康正常", "GroupMonitorLogicTest", "recoveredHealthyGroupAfterQueryFailureReturnsNormal"),
                evidence("停止监控丢弃旧结果", "MonitoringSessionRaceTest", "stoppedSessionCannotRecordQueryFailureAfterFetchReturns"),
                evidence("切换连接丢弃旧任务", "MonitoringSessionRaceTest", "switchedConnectionCannotBeOverwrittenByOldTask"),
                evidence("连接测试丢弃旧回调", "MonitoringSessionRaceTest", "staleConnectionTestResultCannotOverwriteNewConnection"),
                evidence("会话密码快照并清零", "MonitoringSessionTest", "monitoringSessionCopiesClosesAndProtectsTaskPassword"),
                evidence("日志 IO 与监控执行器隔离", "ExecutorSeparationTest", "slowLogReadDoesNotOccupyMonitorExecutor"),
                evidence("损坏配置安全回退", "FieldDeliveryScenarioTest", "damagedConfigurationFallsBackWithoutPartialData"),
                evidence("非法点位组配置拒绝", "GroupConfigStoreTest", "rejectsInvalidFinalUiGroupConfiguration"),
                evidence("连接配置不保存密码", "ConnectionProfileStoreTest", "savesMultipleProfilesWithoutPassword"),
                evidence("结构化查询仅允许固定只读源", "PointDataQueryRepositoryTest", "rejectsFreeSqlAndInvalidIdentifiers"),
                evidence("元数据标识符白名单", "DbMetadataRepositoryTest", "previewRejectsInvalidIdentifiers"),
                evidence("CSV UTF-8 与公式注入防护", "CsvExportServiceTest", "preventsSpreadsheetFormulaExecution"),
                evidence("诊断包白名单与脱敏", "DiagnosticBundleServiceTest", "bundleContainsOnlyAllowlistedSanitizedEntries"),
                evidence("八个真实页面可导航", "ShelfPointMonitorAppUiTest", "finalNavigationPagesCanSwitchAndAreNotBlank"),
                evidence("中文空格路径可运行", "WindowsPathPackagingTest", "chineseAndSpacedPathSupportsFieldArtifacts"));

        assertEquals(27, scenarios.size(), "必须保留 27 项现场场景证据");
        for (ScenarioEvidence scenario : scenarios) {
            String source = Files.readString(scenario.source(), StandardCharsets.UTF_8);
            assertContains(source, "void " + scenario.method() + "(",
                    "现场场景缺少真实测试映射：" + scenario.name());
        }
    }

    private static void damagedConfigurationFallsBackWithoutPartialData() throws Exception {
        Path root = Files.createTempDirectory("field-damaged-config-");
        Path config = root.resolve("config.properties");
        Files.writeString(config, "host=partial.example\nport=9999\nbroken=\\uZZZZ\n", StandardCharsets.UTF_8);

        ConfigStore.StoredConfig loaded = new ConfigStore(config).load();

        assertEquals("127.0.0.1", loaded.host, "损坏配置不得保留半载入主机");
        assertEquals(5432, loaded.port, "损坏配置必须回退默认端口");
    }

    private static void damagedGroupAndConnectionConfigurationFallsBackSafely() throws Exception {
        Path root = Files.createTempDirectory("field-damaged-stores-");
        Path groups = root.resolve("groups.properties");
        Path connections = root.resolve("connections.properties");
        String malformed = "partial=value\nbroken=\\uZZZZ\n";
        Files.writeString(groups, malformed, StandardCharsets.UTF_8);
        Files.writeString(connections, malformed, StandardCharsets.UTF_8);

        List<PointGroupDefinition> loadedGroups = new GroupConfigStore(groups).load();
        ConnectionProfileStore.StoredProfiles loadedProfiles = new ConnectionProfileStore(connections).load();

        assertEquals("sample-group-001", loadedGroups.get(0).id(), "损坏组配置必须回退示例组");
        assertEquals("prod", loadedProfiles.currentId(), "损坏连接配置必须回退默认连接");
        assertEquals(2, loadedProfiles.profiles().size(), "默认连接清单必须完整");
    }

    private static ScenarioEvidence evidence(String name, String className, String method) {
        return new ScenarioEvidence(name, Path.of("test", "com", "local", "monitor", className + ".java"), method);
    }

    private static void assertContains(String text, String expected, String message) {
        if (!text.contains(expected)) {
            throw new AssertionError(message + "，缺少：" + expected);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，expected=" + expected + ", actual=" + actual);
        }
    }

    private record ScenarioEvidence(String name, Path source, String method) {
    }
}
