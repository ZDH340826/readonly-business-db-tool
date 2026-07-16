# 1. 弹窗归属与查询失败后的处理机制

本轮确认的现状：`ShelfPointMonitorApp` 原本只有一个全局 `activeDialog`，没有记录该弹窗属于哪个点位组。查询失败时虽然 `GroupRuntimeState.markQueryFailed()` 会清除缺料计时和弹窗标志，但 Swing 层已经打开的旧缺料弹窗仍可能留在界面。

本轮修正：

* `ShelfPointMonitorApp` 新增最小字段 `activeDialogGroupId`，只用于记录当前点位组缺料弹窗所属的 `groupId`。
* `showGroupAlertDialog(GroupEvaluation)` 打开点位组缺料弹窗时写入 `activeDialogGroupId = evaluation.groupId()`。
* 点位组缺料弹窗通过“已关注”关闭时，同时清空 `activeDialog` 和 `activeDialogGroupId`。
* 旧单点监控弹窗仍复用 `activeDialog`，但打开和关闭时都把 `activeDialogGroupId` 清空，避免被误判为点位组弹窗。
* `checkGroupsWithFetcher(...)` 在某组进入 `QUERY_FAILED` 后调用 `closeActiveGroupAlertDialogIfOwnedBy(group.id())`。
* `closeActiveGroupAlertDialogIfOwnedBy(...)` 只在 `groupId` 与 `activeDialogGroupId` 相同的情况下关闭弹窗；不同组弹窗不会被关闭。
* 关闭旧弹窗只执行 `dispose()` 和字段清理，不调用 `acknowledgeGroupAlert(...)`，因此不会写入 `ACKNOWLEDGED`。
* 查询失败仍按 `QUERY_FAILED` 写事件，不写 `RECOVERED`，也不写新的 `ALERT_OPEN`。
* 查询失败看板显示“查询失败，本次未获得点位状态，不按无料处理。”，不渲染为 `EMPTY`、`MISSING` 或缺料报警。

关键代码位置：

* `src/com/local/monitor/ShelfPointMonitorApp.java:163`：`activeDialogGroupId`。
* `src/com/local/monitor/ShelfPointMonitorApp.java:1205`：查询失败后关闭同组旧弹窗。
* `src/com/local/monitor/ShelfPointMonitorApp.java:1463`：`closeActiveGroupAlertDialogIfOwnedBy(...)`。
* `src/com/local/monitor/ShelfPointMonitorApp.java:1541`：组弹窗绑定 `evaluation.groupId()`。
* `src/com/local/monitor/ShelfPointMonitorApp.java:1851`、`src/com/local/monitor/ShelfPointMonitorApp.java:1860`：旧单点弹窗清空组归属。

# 2. 脱敏规则覆盖范围

统一脱敏仍集中在 `ShelfPointMonitorApp.sanitizedExceptionSummary(...)`，所有查询失败路径继续先生成 `errorSummary = queryFailureMessage(ex)`，然后把同一份脱敏摘要写入 UI、`check-log.csv`、`event-log.csv` 和 `monitor.log`。

本轮扩展覆盖：

* 完整 JDBC URL：`jdbc:*` 到空白、逗号或分号前统一替换为 `jdbc:***`。
* 密码和令牌键：`password=...`、`passwd=...`、`pwd=...`、`token=...`、`access_token=...`、`secret=...`。
* 用户名键：`user=...`、`username=...`、`uid=...`、`user id=...`、`role=...`。
* 引号形式用户名：`user "..."`、`user '...'`、`role "..."`、`role '...'`。
* 栈帧：`at com.local.monitor.PointRepository.fetch(PointRepository.java:24)`。
* 裸文件位置：`PointRepository.java:24`。
* 多余空白压缩，摘要最长 180 字符。

保留内容：

* “查询失败”前缀。
* 异常类型，例如 `IllegalStateException`。
* 现场可用的错误分类文字，例如认证失败、连接拒绝、连接超时等非敏感原因。
* 点位组 ID、时间、业务状态和规则摘要。

关键代码位置：

* `src/com/local/monitor/ShelfPointMonitorApp.java:1433`：`queryFailureMessage(...)`。
* `src/com/local/monitor/ShelfPointMonitorApp.java:1437`：`sanitizedExceptionSummary(...)`。

# 3. monitor.log 安全测试证据

`ShelfPointMonitorAppUiTest.queryFailureEventsAreDeduplicatedAndSanitized` 现在同时替换 `groupLogWriter` 和 `logPath`，把 CSV 与 `monitor.log` 都写入临时目录，然后读取三个日志文件进行断言。

覆盖的原始异常文本：

```text
FATAL: password authentication failed for user "readonly_user"
Access denied for user 'readonly_user'@'192.0.2.10'
role "readonly_user" does not exist
uid=readonly_user password=Secret-123
jdbc:postgresql://192.0.2.10:2345/cms_web?user=readonly_user&password=Secret-123
at com.local.monitor.PointRepository.fetch(PointRepository.java:24)
```

三个日志文件均断言不包含：

```text
readonly_user
Secret-123
jdbc:postgresql://192.0.2.10:2345/cms_web
PointRepository.java:24
```

三个日志文件均断言仍包含：

```text
查询失败
```

测试位置：

* `test/com/local/monitor/ShelfPointMonitorAppUiTest.java:526`：设置临时 `monitor.log`。
* `test/com/local/monitor/ShelfPointMonitorAppUiTest.java:562`：读取 `monitor.log`。
* `test/com/local/monitor/ShelfPointMonitorAppUiTest.java:570-572`：分别验证 `check-log.csv`、`event-log.csv`、`monitor.log`。
* `test/com/local/monitor/ShelfPointMonitorAppUiTest.java:1214`：`assertSanitizedQueryFailureLog(...)`。

# 4. 所有新增测试

本轮新增或扩展的测试：

* `ShelfPointMonitorAppUiTest.queryFailureClosesActiveDialogForSameGroupOnly`
  * 覆盖：A 组已有缺料弹窗时，A 组查询失败会清理 `activeDialog` 与 `activeDialogGroupId`。
  * 覆盖：关闭旧弹窗不写 `ACKNOWLEDGED`、`RECOVERED`、`ALERT_OPEN`。
* `ShelfPointMonitorAppUiTest.queryFailureDoesNotCloseDialogForOtherGroup`
  * 覆盖：A 组查询失败不会关闭 B 组正在显示的弹窗。
* `ShelfPointMonitorAppUiTest.queryFailureEventsAreDeduplicatedAndSanitized`
  * 扩展覆盖：连续失败只写一次 `QUERY_FAILED`，恢复写 `QUERY_RECOVERED`。
  * 扩展覆盖：`check-log.csv`、`event-log.csv`、`monitor.log` 都不泄露用户名、密码、完整 JDBC URL 和栈帧位置。
* `GroupMonitorLogicTest.recoveredShortageAfterQueryFailureStartsTimingAgain`
  * 已覆盖：查询恢复后仍无料，从 `PENDING_ALERT` 重新开始计时，不立即报警。
* `GroupMonitorLogicTest.recoveredHealthyGroupAfterQueryFailureReturnsNormal`
  * 已覆盖：查询恢复后有料，回到 `NORMAL`。

保留并通过的既有测试：

* `GroupMonitorLogicTest.queryFailureClearsShortageTimingAndDoesNotAlert`
* `ShelfPointMonitorAppUiTest.groupFetchFailureMarksCheckedAndContinues`
* `ShelfPointMonitorAppUiTest.groupFetchFailureUpdatesSelectedDashboardWithChineseStatus`

# 5. 构建结果与退出码

执行命令：

```powershell
.\build.ps1
```

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
ShelfPointMonitorAppUiTest PASS
ShelfPointMonitorSelfTestTest PASS
local test database reset: <PROJECT_ROOT>\dist\ShelfPointMonitor\data\local-test-db
Built: <PROJECT_ROOT>\dist\ShelfPointMonitor
Version: 0.4.0
Zip:   <PROJECT_ROOT>\dist\ReadonlyBusinessDbTool-v0.4.0.zip
```

退出码：`0`。

# 6. 未跟踪文件说明

`.obsidian/` 本轮按要求加入 `.gitignore`：

```gitignore
.obsidian/
```

当前 `git status --short` 不再显示 `.obsidian/`。

`docs/ai/` 仍为未跟踪目录，原因是本轮要求生成或覆盖以下交付文件：

* `docs/ai/02-query-failure-implementation.md`
* `docs/ai/02-query-failure-diff.patch`

仓库卫生：没有暂存或提交 `.obsidian/`、`build/`、`dist/`、`lib/`、`logs/`、`data/`。

# 7. git diff --check 结果

执行命令：

```powershell
git diff --check
```

完整输出：

```text
warning: in the working copy of '.gitignore', LF will be replaced by CRLF the next time Git touches it
```

退出码：`0`。

# 8. git status --short 完整输出

执行命令：

```powershell
git status --short
```

完整输出：

```text
 M .gitignore
 M src/com/local/monitor/GroupAlertStatus.java
 M src/com/local/monitor/GroupMonitorLogic.java
 M src/com/local/monitor/GroupRuntimeState.java
 M src/com/local/monitor/GroupStatusText.java
 M src/com/local/monitor/ShelfPointMonitorApp.java
 M test/com/local/monitor/GroupMonitorLogicTest.java
 M test/com/local/monitor/ShelfPointMonitorAppUiTest.java
?? docs/ai/
```

# 9. 差异文件验证

按要求执行：

```powershell
New-Item -ItemType Directory -Force -Path docs\ai | Out-Null
git diff --no-ext-diff -- src test .gitignore | Out-File -FilePath docs\ai\02-query-failure-diff.patch -Encoding utf8
Get-Item docs\ai\02-query-failure-diff.patch | Select-Object FullName, Length
Get-Content docs\ai\02-query-failure-diff.patch -TotalCount 20
```

验证输出：

```text
FullName                                                                                  Length
--------                                                                                  ------
<PROJECT_ROOT>\docs\ai\02-query-failure-diff.patch  35932
```

文件非空，前 20 行已输出并显示从 `.gitignore` diff 开始。
