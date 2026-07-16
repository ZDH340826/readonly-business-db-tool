# 1. UI 架构与新增类说明

本轮在保留入口 `com.local.monitor.ShelfPointMonitorApp` 的前提下，将应用外壳改为 8 页 `CardLayout` 桌面 UI。窗口最小尺寸调整为 `1180 x 760`，左侧固定导航，顶部显示当前连接、监控状态、上次检测、下次检测，底部显示运行状态。

新增类：

| 文件 | 类 | 作用 |
|---|---|---|
| `src/com/local/monitor/UiPreferences.java` | `UiPreferences` | 本地 UI 偏好模型，包含默认首页、总览刷新间隔、弹窗/声音、日志保留、显示密度、自检和自动清理设置。 |
| `src/com/local/monitor/UiPreferencesStore.java` | `UiPreferencesStore` | 读写 `data/ui-settings.properties`，不保存密码和数据库机密。 |
| `src/com/local/monitor/PointDataQuery.java` | `PointDataQuery` | 固定来源表 `tcs_map_data` 的结构化只读查询条件与 SELECT 模板。 |
| `src/com/local/monitor/PointDataQueryRepository.java` | `PointDataQueryRepository` | 通过 `ReadOnlyConnectionFactory` 执行参数化 SELECT 查询。 |
| `src/com/local/monitor/PointDataQueryResult.java` | `PointDataQueryResult` | 返回查询结果、SQL 类型和 SQL 模板。 |

修改类：

| 文件 | 主要变化 |
|---|---|
| `src/com/local/monitor/ShelfPointMonitorApp.java` | 落地 8 页 UI、顶部/底部状态栏、总览/报警中心/日志/设置/数据查询页面、查询结果刷新和偏好设置生效逻辑。 |
| `src/com/local/monitor/ColumnInfo.java` | 增加默认值、备注字段。 |
| `src/com/local/monitor/DbMetadataRepository.java` | 元数据读取增加 `COLUMN_DEF` 与 `REMARKS`。 |
| `src/com/local/monitor/GroupConfigStore.java` | 增强点位组保存前校验。 |

# 2. 八个页面分别实现了什么真实功能

| 页面 | 已实现功能 |
|---|---|
| 监控总览 | 统计监控点位组、缺料报警、观察中、数据异常；显示点位组监控表；右侧显示规则、持续时间、上次检测和点位明细；支持开始/停止监控、立即检测、查看报警详情、已关注。 |
| 点位组管理 | 复用现有点位组配置模型；支持新增/删除组、新增/删除点位、保存配置、放弃修改、验证配置、开始/停止监控、立即检测；继续展示点位状态看板。 |
| 报警中心 | 显示活跃报警、已关注、观察中、查询失败、已恢复；读取本次运行评估和本地 `event-log.csv`；支持标记已关注、查看点位详情、立即检测该组、查看连接状态。 |
| 连接管理 | 保留连接列表、配置表单、保存/删除/测试功能；新增只读安全说明和“设为当前连接”；密码仍只保存在运行内存。 |
| 数据查询 | 固定查询 `schema.tcs_map_data`；支持点位编码、货架编号、区域、关联区域、更新时间起止、行数上限；结果表和详情面板只读；不提供 SQL 编辑器。 |
| 数据源浏览器 | 保留 Schema 刷新、表/视图加载、字段元数据、前 100 行预览；新增统计卡片；字段元数据包含列名、类型、长度、可空、默认值、备注。 |
| 日志与系统 | 显示健康卡片；读取本地 `event-log.csv`、`check-log.csv`、`monitor.log`；支持事件类型、时间范围、点位组、关键字筛选；支持打开日志目录、执行自检、导出诊断信息。 |
| 系统设置 | 支持默认首页、总览刷新间隔、报警弹窗、声音提示、日志保留天数、显示密度、启动自检、日志自动清理；固定显示不可关闭的只读和脱敏安全项。 |

# 3. 页面对应的业务模型或新增安全查询模型

| 页面 | 对应模型 |
|---|---|
| 监控总览 | `PointGroupDefinition`、`GroupEvaluation`、`GroupRuntimeState`、`GroupStatusText`。 |
| 点位组管理 | `GroupConfigStore`、`PointGroupDefinition`、`GroupMonitorPoint`、`GroupAlertRule`。 |
| 报警中心 | `GroupEvaluation`、`GroupLogWriter` 写出的 `event-log.csv`。 |
| 连接管理 | `ConnectionProfile`、`ConnectionProfileStore`、`ReadOnlyConnectionFactory`。 |
| 数据查询 | 新增 `PointDataQuery`、`PointDataQueryRepository`、`PointDataQueryResult`。 |
| 数据源浏览器 | `DbMetadataRepository`、`SchemaInfo`、`TableInfo`、`ColumnInfo`、`TablePreview`。 |
| 日志与系统 | `GroupLogWriter` 本地 CSV、`monitor.log`、`ShelfPointMonitorApp.runSelfTest`。 |
| 系统设置 | 新增 `UiPreferences`、`UiPreferencesStore`。 |

# 4. 仍未实现的设计项

无。

说明：数据查询“导出当前结果”为可选项，因需求允许无法安全完成时不显示按钮，本轮未展示导出按钮。

# 5. 新增或修改的测试

新增：

| 测试 | 覆盖 |
|---|---|
| `test/com/local/monitor/PointDataQueryRepositoryTest.java` | 固定 `tcs_map_data` 只读查询、参数化条件、非法 schema 拒绝、SQL 不包含写入/DDL 关键字。 |
| `test/com/local/monitor/UiPreferencesStoreTest.java` | UI 偏好保存、加载、恢复默认、非法默认页/刷新间隔/保留天数/显示密度拒绝。 |

扩展：

| 测试 | 覆盖 |
|---|---|
| `ShelfPointMonitorAppUiTest` | 8 个导航页面、页面非空、顶部/底部状态栏、最终连接按钮文案、查询失败显示、日志脱敏、原有点位组 UI 行为。 |
| `GroupConfigStoreTest` | 点位组 ID 唯一、组名、使用位、重复点位、检测周期、报警持续时间等校验。 |
| `DbMetadataRepositoryTest` | 既有元数据和预览只读能力仍通过。 |

# 6. 数据查询的 SQL 安全边界

`PointDataQuery.fixedSelectSql(schema)` 只生成固定 SELECT：

```sql
select ... from <schema>.tcs_map_data
where (? = '' or lower(map_data_code) like lower(?))
...
limit ? offset ?
```

边界：

* schema 通过 `PointQuery.validateIdentifier` 校验；
* 表名固定为 `tcs_map_data`，用户不能输入表名、SQL、字段表达式或排序表达式；
* 所有过滤值通过 `PreparedStatement` 参数传入；
* 查询通过 `ReadOnlyConnectionFactory.open` 创建连接；
* 设置查询超时 `ps.setQueryTimeout(8)`；
* 行数限制在 `1..500`；
* 不新增 INSERT、UPDATE、DELETE、DDL；
* 本地 H2 测试库写入仍只存在于 `LocalTestDatabase`，用于测试库初始化/场景切换，不属于生产数据库访问路径。

# 7. 系统设置的实际生效行为

| 设置 | 生效方式 |
|---|---|
| 默认首页 | 保存到 `data/ui-settings.properties`，下次启动通过 `selectDefaultPage()` 选择。 |
| 监控总览自动刷新间隔 | 重启 Swing `Timer`，按配置刷新总览表。 |
| 报警弹窗启用 | 检测命中报警时决定是否调用 `showGroupAlertDialog`；关闭后仍写日志和状态。 |
| 报警声音提示 | 弹窗前调用系统 `Toolkit.beep()`。 |
| 日志保留天数 | 自动清理开启时删除超过保留天数的本地日志文件。 |
| 界面显示密度 | 调整主要表格行高。 |
| 启动时执行自检 | 启动后后台执行 `runSelfTest`。 |
| 日志自动清理 | 保存/加载偏好后触发 `cleanupOldLogs()`。 |

# 8. 构建结果、退出码、git diff --check 输出

构建命令：

```powershell
.\build.ps1
```

退出码：`0`

最终输出摘要：

```text
MonitorLogicTest PASS
GroupMonitorLogicTest PASS
GroupCheckPlannerTest PASS
PointStatusMapperTest PASS
LocalTestDatabaseTest PASS
PointScheduleTest PASS
ConfigStoreTest PASS
GroupConfigStoreTest PASS
GroupLogWriterTest PASS
ConnectionProfileStoreTest PASS
DbMetadataRepositoryTest PASS
PointDataQueryRepositoryTest PASS
UiPreferencesStoreTest PASS
ShelfPointMonitorAppUiTest PASS
ShelfPointMonitorSelfTestTest PASS
local test database reset: <PROJECT_ROOT>\dist\ShelfPointMonitor\data\local-test-db
Built: <PROJECT_ROOT>\dist\ShelfPointMonitor
Version: 0.4.0
Zip:   <PROJECT_ROOT>\dist\ReadonlyBusinessDbTool-v0.4.0.zip
```

`git diff --check` 退出码：`0`

输出：

```text
warning: in the working copy of '.gitignore', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'build.ps1', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/com/local/monitor/ColumnInfo.java', CRLF will be replaced by LF the next time Git touches it
warning: in the working copy of 'src/com/local/monitor/DbMetadataRepository.java', CRLF will be replaced by LF the next time Git touches it
```

# 9. git status --short 完整输出

```text
 M .gitignore
 M build.ps1
 A docs/ai/04-full-ui-delivery-report.md
 M src/com/local/monitor/ColumnInfo.java
 M src/com/local/monitor/DbMetadataRepository.java
 M src/com/local/monitor/GroupAlertStatus.java
 M src/com/local/monitor/GroupConfigStore.java
 M src/com/local/monitor/GroupMonitorLogic.java
 M src/com/local/monitor/GroupRuntimeState.java
 M src/com/local/monitor/GroupStatusText.java
 A src/com/local/monitor/PointDataQuery.java
 A src/com/local/monitor/PointDataQueryRepository.java
 A src/com/local/monitor/PointDataQueryResult.java
 M src/com/local/monitor/ShelfPointMonitorApp.java
 A src/com/local/monitor/UiPreferences.java
 A src/com/local/monitor/UiPreferencesStore.java
 M test/com/local/monitor/GroupConfigStoreTest.java
 M test/com/local/monitor/GroupMonitorLogicTest.java
 A test/com/local/monitor/PointDataQueryRepositoryTest.java
 M test/com/local/monitor/ShelfPointMonitorAppUiTest.java
 A test/com/local/monitor/UiPreferencesStoreTest.java
?? docs/ai/01-project-baseline.md
?? docs/ai/02-query-failure-diff.patch
?? docs/ai/02-query-failure-implementation.md
?? docs/ai/04-full-ui-delivery-diff.patch
```

说明：`A` 为 `git add -N` 的 intent-to-add，用于让指定 `git diff --no-ext-diff -- src test docs build.ps1 .gitignore VERSION CHANGELOG.md` 包含新增源码、新增测试和本交付报告；未提交，未加入构建产物、日志、本地配置或 H2 数据库文件。

# 10. 未确认的现场风险

* 未连接真实 PostgreSQL，无法证明现场网络断开、账号权限不足、数据库慢查询时的完整 UI 体验。
* `COLUMN_DEF`、`REMARKS` 是否能由现场 PostgreSQL JDBC 元数据返回，需现场验证。
* 日志自动清理遇到被其他程序占用的日志文件时，本地只做忽略处理，现场需要观察是否满足运维习惯。
* 结构化查询固定 `tcs_map_data` 和公开字段名；若现场表结构变化，需要更新映射，但仍应保持只读和参数化。
* 本轮没有做像素级截图测试，Swing 组件级测试已覆盖 8 页构建和关键文案。
