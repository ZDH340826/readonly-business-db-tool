# v0.5.0-rc.1 自主现场交付最终报告

报告基于交付内容冻结提交 `41252aaae05f81833f3398b09fb207759040b3ee`。本报告和最终补丁随后作为纯证据文件提交，不改变应用源码、发布脚本或已验证 ZIP 内容。

## 1. 基线提交 `f1443c7`

- 基线：`f1443c7132b1c18ba5662d2ef94d58545e7acc29`。
- 备份分支：`backup/checkpoint-f1443c7`。
- 检查点标签：`checkpoint-before-ui`、`checkpoint-before-final-delivery`。
- 开发分支：`codex/final-field-delivery`。
- 全部后续修改均建立在该基线之后，没有重写或回退五个已完成基线提交。

## 2. 最终提交

最终交付内容冻结提交为：

```text
41252aaae05f81833f3398b09fb207759040b3ee docs: align field operations acceptance evidence
```

它对应当前 `0.5.0-rc.1` ZIP 的源码、构建脚本和包内文档。报告与补丁的后续提交仅保存验收证据；交付时的实际仓库 HEAD 由最终 `git rev-parse HEAD` 输出记录。

## 3. 全部阶段提交

从基线到交付内容冻结点共 17 个本地提交，按时间顺序如下：

```text
06cb0b7 docs: define v0.5.0 rc1 field delivery
37312e1 docs: plan v0.5.0 rc1 implementation
04e00ed fix: close final field stability gaps
3cea9d0 refactor: add shared swing theme components
ca5435d refactor: establish maintainable application UI architecture
73bab85 refactor: extract overview and alert center pages
a0a9ac8 refactor: complete group and connection layouts
5ad9dd3 feat: finish read-only data pages
07fa849 feat: add sanitized diagnostics and finish system pages
e07526d test: harden field delivery scenarios
22f445a build: add offline field deployment tooling
7fc4959 docs: add final UI design references
55cd3e1 docs: add field operation and acceptance manuals
e7e084c build: generate release checksum
76093a8 docs: record autonomous rc delivery evidence
f985e21 docs: normalize final delivery evidence
41252aa docs: align field operations acceptance evidence
```

## 4. 最终架构

- 生命周期与装配：`ShelfPointMonitorApp` 负责窗口生命周期、页面装配、会话切换、后台任务投递和 EDT 回调。
- UI 基础：`AppTheme`、`UiFactory`、`AppShell`、`NavigationSidebar`、`TopStatusBar`、`BottomStatusBar`、`StatusBadge`、`MetricCard`、`SectionCard`。
- 页面层：八个独立页面类只接收 Swing 组件和明确回调，不接收密码、不拼 SQL、不直接创建数据库连接。
- 业务与会话：`GroupMonitorLogic`、`GroupRuntimeState`、`MonitoringSession`、状态/事件模型均保持 Swing 无关。
- 数据访问：`ReadOnlyConnectionFactory`、`PointRepository`、`PointDataQueryRepository`、`DbMetadataRepository` 统一承担固定只读访问。
- 文件与运维：`CsvExportService`、`SensitiveTextSanitizer`、`DiagnosticBundleService`、`FieldDeploymentPreflight`、`DiagnosticBundleTool` 独立处理导出、脱敏、诊断和离线预检。
- 线程边界：监控查询、文件 I/O 与 EDT 分离；UI 更新只回到 EDT。

## 5. 八个页面完成情况

| 页面 | 最终布局与真实能力 |
|---|---|
| 监控总览 | 四个统计卡、组状态表、右侧详情、开始/停止/立即检测/查看报警/标记已关注；`QUERY_FAILED` 独立显示。 |
| 点位组管理 | 左侧区域/点位组树、中部基本信息、右侧报警规则、下方点位配置表；七个配置动作连接真实存储和校验。 |
| 报警中心 | 活跃报警、已关注、观察中、查询失败、已恢复五类真实筛选；所有动作按 `groupId` 定位。 |
| 连接管理 | 左侧连接列表、中部连接配置、右侧测试结果和安全说明；新建、删除、测试、保存、设为当前连接均真实生效。 |
| 数据查询 | 固定数据源的结构化条件、参数化查询、COUNT、分页、LIKE 转义、超时/上限和当前页 CSV 导出，无自由 SQL。 |
| 数据源浏览器 | 左侧 Schema/表/视图对象树、中部元数据/列、右侧前 100 行只读预览，标识符受白名单校验。 |
| 日志与系统 | 六个动态健康卡、日志筛选和详情、1000/1000/200 读取上限、自检、日志目录和脱敏诊断导出。 |
| 系统设置 | 八项真实生效设置，以及脱敏、只读、无 SQL 编辑、禁用密码持久化四项不可关闭安全声明。 |

所有页面使用真实组件、模型、Repository/service 或本地配置回调；只有本地 H2 演示模式使用公开样例数据，现场连接模式不伪造数据。

## 6. UI 主题和视觉规范

- 推荐窗口 `1440 × 900`，最小窗口 `1180 × 760`；导航约 `210px`、顶栏 `56px`、底栏 `28px`。
- 页面浅灰白、卡片白色、蓝色主色；正常/观察/报警/查询失败/已关注/恢复/停用均有独立语义色和中文文字。
- 字体优先 Microsoft YaHei，回退 SansSerif；表格标准行高 `32px`。
- 按钮分主要、次要、危险角色；表格、卡片、徽标、边框、间距和选中状态统一。
- 八张设计基线图均为 `1440 × 900`，用于视觉参考；Swing 组件测试验证窗口、外壳、页面结构和操作回调。

## 7. 业务状态语义

- `NORMAL`：业务正常。
- `PENDING_ALERT`：缺料条件出现但持续时间未到，显示“观察中”。
- `ACTIVE_ALERT`：持续时间达到规则阈值，显示活跃报警。
- `ACKED_ALERT`：本轮报警已关注，恢复前抑制重复弹窗。
- 真实 `RECOVERED`：仅曾经业务报警后恢复才生成并进入“已恢复”。
- `QUERY_FAILED`：固定显示“查询失败，数据不可用”，不是缺料、正常或恢复，不触发业务报警。
- `QUERY_RECOVERED`：仅表示查询通道恢复，不进入业务已恢复；若仍缺料则从零重新计时。
- 查询失败只关闭同组旧弹窗，不影响其他组。

## 8. 数据库只读证据

- `ReadOnlyConnectionFactory` 对 PostgreSQL 设置 JDBC 只读、关闭自动提交、执行只读事务和 `statement_timeout`。
- H2 监控连接使用 `ACCESS_MODE_DATA=r`；`ReadOnlyConnectionTest` 实测 DDL 被拒绝。
- `PointRepository`、`PointDataQueryRepository` 只执行固定参数化 `SELECT`/`COUNT`。
- `DbMetadataRepository` 只读取 JDBC metadata 和白名单对象的固定前 100 行预览。
- `ReadOnlyConnectionTest` 扫描生产数据库源码，禁止写 SQL 字面量。
- `LocalTestDatabase` 的建表/写入只用于隔离的本地 H2 初始化和演示场景，不进入生产路径。
- 没有 SQL 编辑器、自由表名、自由字段名或自由排序表达式。

## 9. 密码生命周期

- 密码只从当前密码框复制到 `MonitoringSession` 的运行期 `char[]` 快照。
- 每个后台任务通过 `copyPasswordForTask()` 获得独立副本。
- 任务 `finally`、一次性检测结束、停止监控、切换连接和窗口关闭均清零副本。
- 配置存储不含密码字段；连接配置测试验证保存文件不存在密码。
- 诊断、日志、报告和发布包不写密码；程序重启后必须重新输入。

## 10. 监控任务竞态处理

- `monitoringGeneration` 和 `MonitoringSession.publicationAllowed` 在查询前、结果发布前及 EDT 回调时共同门禁。
- 停止监控后的旧任务不能写事件、修改状态、刷新 UI 或弹出报警。
- 切换连接会关闭旧会话；旧任务不能覆盖新连接和新状态。
- `connectionOperationGeneration` 丢弃旧连接测试结果，旧密码副本仍被清零。
- 同组查询失败关闭同组旧报警窗口，其他组窗口保持不变。
- `MonitoringSessionRaceTest` 和 `ShelfPointMonitorAppUiTest` 覆盖上述竞态。

## 11. 执行器职责

- `monitorExecutor`：自动检测和手动检测的数据库监控任务。
- `ioExecutor`：日志读取/写入/清理、健康检查、打开日志目录、CSV 和诊断 I/O。
- EDT：创建不可变 UI 输入快照、装配页面和发布最终 Swing 更新。
- `appendStatus()` 先更新 UI，再把文件写入排入 `ioExecutor`，不在监控线程做文件 I/O。
- `ExecutorSeparationTest` 验证慢日志读取不占用监控调度，并验证关闭窗口时两个执行器均停止。

## 12. 脱敏范围

统一脱敏器覆盖密码、token、secret、数据库用户名/角色、IPv4、主机名、完整 JDBC URL、Windows/Unix/UNC 绝对路径、Java 栈帧和 `.java:行号`。覆盖入口包括：

- 用户可见错误摘要和查询失败分类；
- 状态栏、运行日志、检测日志和事件日志；
- CSV 导出前的可见数据；
- 六文件诊断 ZIP 和环境摘要；
- 最终发布包项目自有文本扫描。

脱敏后仍保留“查询失败、连接超时、认证失败、连接被拒绝、配置读取失败、日志目录不可写”等可操作分类。

## 13. 测试清单和结果

最终完整构建运行 35/35 测试入口，全部退出 0：

```text
MonitorLogicTest
GroupMonitorLogicTest
GroupCheckPlannerTest
PointStatusMapperTest
LocalTestDatabaseTest
PointScheduleTest
ConfigStoreTest
GroupConfigStoreTest
GroupLogWriterTest
ConnectionProfileStoreTest
DbMetadataRepositoryTest
PointDataQueryRepositoryTest
UiPreferencesStoreTest
AppThemeTest
AppShellTest
OverviewAlertPageTest
GroupConnectionPageTest
CsvExportServiceTest
DataPagesTest
DiagnosticBundleServiceTest
LogsSettingsPageTest
FieldDeliveryScenarioTest
WindowsPathPackagingTest
FieldDeploymentPreflightTest
WindowsLauncherScriptTest
MonitoringSessionTest
MonitoringSessionRaceTest
ErrorSanitizationTest
SensitiveDataSanitizationTest
ReadOnlyConnectionTest
ExecutorSeparationTest
SystemHealthStatusTest
SourceHygieneTest
ShelfPointMonitorAppUiTest
ShelfPointMonitorSelfTestTest
```

`FieldDeliveryScenarioTest` 维护附件要求的 27 项现场场景映射。全部数据库自动测试只使用本地 H2、fake、stub 或 mock。

## 14. 构建结果与退出码

最终执行 `build.ps1` 退出码为 `0`。构建完成了 UTF-8 编译、35 个测试入口、JAR、jlink runtime、包内 H2 初始化、应用自检、离线预检、诊断命令、ZIP 和 SHA256SUMS。关键输出为：

```text
ShelfPointMonitor SELF_TEST_OK
现场部署检查完成：全部通过
Version: 0.5.0-rc.1
Zip: dist/ReadonlyBusinessDbTool-v0.5.0-rc.1.zip
```

## 15. 发布目录树

```text
ShelfPointMonitor/
├─ 启动工具.bat
├─ 现场部署检查.bat
├─ 生成诊断包.bat
├─ ShelfPointMonitor.jar
├─ VERSION
├─ 现场运维交付手册.md
├─ 现场验收清单.md
├─ 回滚说明.md
├─ runtime/
│  └─ bin/java.exe
├─ lib/
│  ├─ h2-2.2.224.jar
│  └─ postgresql-42.2.25.jar
├─ data/
│  ├─ config.properties
│  ├─ connections.properties
│  ├─ group-config.properties
│  └─ local-test-db.mv.db
├─ logs/
└─ diagnostics/
   └─ preflight-latest.txt
```

发布 ZIP 共 180 个条目。源码、测试、Git 数据、运行日志和生成后的诊断现场 ZIP 均未进入发布包。

## 16. ZIP 文件名

```text
dist/ReadonlyBusinessDbTool-v0.5.0-rc.1.zip
```

## 17. ZIP 大小

```text
31,091,284 bytes
```

## 18. SHA-256

```text
B40BF61D258AB0136B1568024A550E86E88BE797A2782472B803BA5F2A54B0FD
```

`dist/SHA256SUMS.txt` 只有一行相同哈希和目标 ZIP 文件名；独立 `Get-FileHash` 重算一致。

## 19. 未执行的生产数据库验证

- 未连接任何真实生产 PostgreSQL。
- 未验证现场网络、认证、SSL、防火墙或 `pg_hba.conf`。
- 未用真实账号确认当前数据库用户、表权限或数据库侧写权限拒绝。
- 未读取真实业务表、真实点位、真实日志或真实查询结果。
- 未执行真实环境长时间运行、断线、维护窗口或故障切换验证。
- 因此本报告不声称 PostgreSQL 现场验证或生产验收通过。

## 20. 现场待确认项

- 数据库服务地址解析、目标端口、SSL、网络和防火墙路径。
- DBA 预置的 `pg_hba.conf` 规则、认证方式和生效流程。
- 专用只读账号连接、当前数据库用户和最小权限。
- 固定表及八个字段的业务含义、状态/锁定值、时区和数据新鲜度。
- 真实点位组、使用位/备用位关系、检测周期、持续时间和备用位阈值。
- 有料、无料、持续报警、已关注、恢复和非破坏性查询失败现场场景。
- 目标 Windows、安全软件、磁盘配额、中文字体、DPI、长时间稳定性和回滚演练。

## 21. 遗留风险

- 这是 RC 试运行包，不是生产正式版；最大剩余风险是未完成真实 PostgreSQL 和现场字段语义验证。
- 八张设计图是视觉基线，不是所有目标 DPI 下的 Swing 实机截图；目标 Windows 上仍需按验收清单检查字体和布局。
- 自动化证明了受控竞态，但现场网络抖动、数据库维护和长时间运行仍需试运行观察。
- 内嵌 Java 运行时含供应方标准本机网络默认配置；项目自有配置、文档、脚本和诊断内容的敏感模式扫描为 0。
- 构建机在缺少依赖 JAR 时会下载公开依赖；已生成的现场 ZIP 包含 runtime 和依赖，现场运行与离线预检不需要联网。

## 22. `git status --short` 完整输出

在交付内容冻结提交 `41252aa` 后执行 `git status --short`，标准输出为空。完整输出代码块如下（空代码块即零行输出）：

```text
```

`build/`、`dist/`、`lib/`、运行状态目录和数据库文件由 `.gitignore` 排除，不存在未解释修改。

## 23. 未访问生产数据库声明

明确声明：本轮自主开发、测试、构建、预检和最终验收未访问真实生产数据库。所有自动数据库验证只使用本地 H2、fake、stub 或 mock。

## 24. 未推送远程仓库声明

明确声明：本轮没有执行 `git push`、force push 或任何远程写操作。`codex/final-field-delivery` 没有配置 upstream，所有新提交仅存在本地。

## 25. 未创建 Pull Request 声明

明确声明：本轮没有创建 Pull Request，也没有调用远程 PR 工作流。根据用户限制，最终分支保留在本地，不合并、不推送、不删除。

## 最终包运行审计补充

修正版 ZIP 已解压到 `build/final-completion-audit-20260714-043439/中文 现场 交付/ShelfPointMonitor`。临时移除系统 Java 后：

- 内嵌 runtime 的应用自检、离线预检、诊断入口：3/3 通过；
- `启动工具.bat --self-test`、`现场部署检查.bat`、`生成诊断包.bat`：3/3 通过；
- 诊断 ZIP 六项白名单精确匹配，敏感模式命中 0；
- 发布包必需条目齐全，禁止条目 0，项目自有文本敏感模式命中 0。
