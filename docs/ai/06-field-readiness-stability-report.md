# 现场交付前 P0/P1 稳定性修正报告

结论：本轮只做稳定性修正，不进入 RC，不修改版本号，不创建发布声明。按用户要求执行 `.\build.ps1` 时，脚本自身仍会生成 `dist/` 和 zip 产物；这些产物未纳入 Git。

## 1. ShelfPointMonitorApp.java 恢复策略与源码完整性证据

修复策略：

* 保留现有业务逻辑，不重构 Swing 主类，不删除旧单点监控链路。
* 清除反编译器残留注释。
* 清除机械化空数组调用：`new String[0]`、`new LinkOption[0]`、`new FileAttribute[0]`。
* 修复原始泛型初始化导致的 `javac unchecked / unsafe operation` 警告。
* 将单一后台执行器拆分为 `monitorExecutor` 与 `ioExecutor`，但不改变数据库只读访问边界。

源码完整性验证：

```powershell
Select-String -Path src\com\local\monitor\ShelfPointMonitorApp.java -Pattern "WARNING - Removed try catching itself|possible behaviour change|new String\[0\]|new LinkOption\[0\]|new FileAttribute\[0\]"
```

输出：无匹配。

```powershell
javac -Xlint:unchecked -encoding UTF-8 -cp "lib\postgresql-42.2.25.jar;lib\h2-2.2.224.jar" -d build\lint-classes <全部 src Java 文件>
```

输出：无 unchecked / unsafe warning。

## 2. 机械化反编译痕迹搜索结果

已执行 P0 指定搜索，最终无输出，说明以下文本和机械化调用均未残留：

* `WARNING - Removed try catching itself`
* `possible behaviour change`
* `new String[0]`
* `new LinkOption[0]`
* `new FileAttribute[0]`

## 3. 监控会话快照与密码清零机制

新增 `ShelfPointMonitorApp.MonitoringSession`，包含：

* `DbConfig config`
* `char[] passwordSnapshot`
* `long generation`

行为：

* 点击开始监控时，在 EDT 复制当前 `DbConfig` 和 `currentPassword`，生成不可变会话快照。
* 自动监控任务只接收 `MonitoringSession`，不再读取 `currentProfile` 或 `currentPassword`。
* 手动检测在 EDT 复制 `DbConfig`、点位组配置和密码快照，后台任务结束后清零该临时密码数组。
* 停止监控、连接切换、删除当前连接、窗口关闭、`dispose()` 均会停止旧会话并清零会话密码。
* 旧单点监控链路保留，仅将调度器从旧单执行器迁移到 `monitorExecutor`。

新增测试：

* `MonitoringSessionTest.monitoringSessionCopiesAndClearsPassword`
* `MonitoringSessionTest.startMonitoringUsesImmutablePasswordSnapshot`
* `MonitoringSessionTest.switchingConnectionStopsSessionAndClearsPassword`

## 4. 错误脱敏统一入口与覆盖范围

统一入口：

* `ShelfPointMonitorApp.userVisibleErrorMessage(Exception)`
* `ShelfPointMonitorApp.sanitizedExceptionSummary(Exception)`
* `ShelfPointMonitorApp.sanitizeVisibleLog(String)`

覆盖范围：

* 弹窗正文与标题
* 底部状态栏
* `monitor.log`
* `check-log.csv`
* `event-log.csv`
* 日志页展示
* 诊断导出中的连接文本
* 后台执行异常处理
* 自检失败控制台摘要

脱敏规则覆盖：

* 密码、token、secret
* 数据库用户名，包括 `user`、`uid`、`user id`、`role "用户名"`、`for user '用户名'`
* IPv4 地址
* 完整 JDBC URL
* Windows 本地绝对路径
* Java 栈帧与 `.java:行号`

新增测试：

* `ErrorSanitizationTest.userVisibleErrorUsesSanitizedSummary`
* `ErrorSanitizationTest.queryFailureMessageKeepsCategoryAndHidesSecrets`
* `ErrorSanitizationTest.visibleLogSanitizerCoversMonitorLogInputs`

## 5. monitorExecutor 与 ioExecutor 职责划分

`monitorExecutor`：

* 点位组自动监控调度。
* 点位组手动检测。
* 报警中心针对单个点位组的手动检测。
* 保留旧单点监控链路的调度执行。

`ioExecutor`：

* 日志读取。
* 日志清理。
* 诊断导出。
* 自检。
* 系统健康页文件检查。
* 非监控类后台读取，例如数据库浏览器元数据读取沿用 `runOnceInBackground()`，该入口现在委托给 `ioExecutor`。

日志刷新修正：

* `appendStatus()` 只更新底部状态、追加内存状态区、写入 `monitor.log`。
* `appendStatus()` 不再调用 `loadSystemLogs()`。
* 日志刷新仅由进入日志页、点击刷新、筛选条件变更防抖触发。
* 筛选条件防抖最短间隔为 5 秒。

新增测试：

* `ExecutorSeparationTest.appendStatusDoesNotQueueLogRefresh`
* `ExecutorSeparationTest.logRefreshUsesIoExecutorOnly`
* `ExecutorSeparationTest.slowLogReadDoesNotOccupyMonitorExecutor`
* `ExecutorSeparationTest.disposeStopsBothExecutors`

## 6. 健康卡真实数据来源与状态规则

日志与系统页只保留一套动态健康卡：

* 监控调度器
* 当前连接
* 最近一次检测
* 配置文件
* 日志目录
* 自检状态

配置文件状态：

* 文件不存在：`缺失`
* 存在但不是可读普通文件：`读取失败`
* 可读普通文件：`正常`

日志目录状态：

* 目录不存在：`缺失`
* 存在但不是可写目录：`不可写`
* 可写目录：`可写`

当前连接状态区分：

* 未选择连接
* 已选择但未测试
* 测试成功并正在使用
* 上次连接测试失败

新增测试：

* `SystemHealthStatusTest.configFileStatusDistinguishesMissingReadableAndFailed`
* `SystemHealthStatusTest.logDirectoryStatusDistinguishesMissingWritableAndUnwritable`
* `SystemHealthStatusTest.logsPageHasOnlyOneDynamicHealthCardSet`

## 7. 新增测试

新增独立测试类：

* `test/com/local/monitor/MonitoringSessionTest.java`
* `test/com/local/monitor/ErrorSanitizationTest.java`
* `test/com/local/monitor/ExecutorSeparationTest.java`
* `test/com/local/monitor/SystemHealthStatusTest.java`

扩展既有测试：

* `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`
  * 将旧 `executor` 反射改为 `monitorExecutor` / `ioExecutor`。
  * 继续覆盖查询失败、弹窗归属、日志脱敏、报警中心等既有路径。

## 8. 构建输出与退出码

命令：

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
PointDataQueryRepositoryTest PASS
UiPreferencesStoreTest PASS
MonitoringSessionTest PASS
ErrorSanitizationTest PASS
ExecutorSeparationTest PASS
SystemHealthStatusTest PASS
ShelfPointMonitorAppUiTest PASS
ShelfPointMonitorSelfTestTest PASS
local test database reset: <PROJECT_ROOT>\dist\ShelfPointMonitor\data\local-test-db
Built: <PROJECT_ROOT>\dist\ShelfPointMonitor
Version: 0.4.0
Zip:   <PROJECT_ROOT>\dist\ReadonlyBusinessDbTool-v0.4.0.zip
```

退出码：`0`。

## 9. git diff --check 输出

命令：

```powershell
git diff --check
```

输出：

```text
warning: in the working copy of '.gitignore', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'build.ps1', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/com/local/monitor/ColumnInfo.java', CRLF will be replaced by LF the next time Git touches it
warning: in the working copy of 'src/com/local/monitor/DbMetadataRepository.java', CRLF will be replaced by LF the next time Git touches it
```

退出码：`0`。

## 10. git status --short 完整输出

最终完整输出如下：

```text
 M .gitignore
 M build.ps1
 A docs/ai/04-full-ui-delivery-report.md
 A docs/ai/05-ui-acceptance-fix-report.md
 A docs/ai/06-field-readiness-stability-report.md
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
 A test/com/local/monitor/ErrorSanitizationTest.java
 A test/com/local/monitor/ExecutorSeparationTest.java
 M test/com/local/monitor/GroupConfigStoreTest.java
 M test/com/local/monitor/GroupMonitorLogicTest.java
 A test/com/local/monitor/MonitoringSessionTest.java
 A test/com/local/monitor/PointDataQueryRepositoryTest.java
 M test/com/local/monitor/ShelfPointMonitorAppUiTest.java
 A test/com/local/monitor/SystemHealthStatusTest.java
 A test/com/local/monitor/UiPreferencesStoreTest.java
?? docs/ai/01-project-baseline.md
?? docs/ai/02-query-failure-diff.patch
?? docs/ai/02-query-failure-implementation.md
?? docs/ai/04-full-ui-delivery-diff.patch
?? docs/ai/05-ui-acceptance-fix-diff.patch
?? docs/ai/06-field-readiness-stability-diff.patch
```

说明：

* `build/`、`dist/`、`lib/`、`logs/`、`data/`、`.obsidian/` 未纳入 Git。
* 工作区中存在前序任务留下的未提交/未跟踪文件，本轮未清理、未回滚。
* 为了让指定 `git diff --no-ext-diff -- src test docs build.ps1 .gitignore VERSION CHANGELOG.md` 能包含本轮新增测试与报告，已对本轮新增测试和本报告执行 `git add -N`，仅作为 intent-to-add，不提交内容。

## 11. 尚未完成的 UI 布局项

点位组管理尚未完全实现设计图的左中右三栏布局。

数据源浏览器尚未完全实现左对象树、中元数据、右预览的三栏布局。

本轮不得把当前版本称为可发布 RC，也不得写“全部 UI 设计图已完整落地”。
