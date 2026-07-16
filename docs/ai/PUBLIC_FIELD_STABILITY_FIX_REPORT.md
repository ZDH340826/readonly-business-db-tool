# Public 现场稳定性修复报告

## 1. 本轮基线目录确认

本轮实际开发目录：

~~~text
<PROJECT_ROOT>
~~~

历史目录 <LEGACY_PROJECT_ROOT> 未访问、未修改，仅作为冻结备份。

当前分支为 fix/public-field-stability。工作区在本轮开始前已包含未提交内容；本报告只说明本轮稳定性修复涉及的文件和行为，完整工作区状态见第 12 节。本轮没有访问真实生产数据库，没有增加数据库写入 SQL，没有修改版本号，也不进入 RC。

## 2. 修复内容

### 2.1 ShelfPointMonitorApp.java 源码质量与会话边界

- src/com/local/monitor/ShelfPointMonitorApp.java
  - MonitoringSession 保持配置快照、密码快照、代次和关闭状态；任务通过 copyPasswordForTask() 获得独立副本，并在组监控、旧单点监控结束时统一调用 clearTaskPassword() 清零。
  - stopMonitoring() 在 groupMonitorLock 内先递增监控代次、关闭当前会话、清空组运行状态和最近检测状态，再取消调度任务。检测任务在进入运行状态修改前持同一锁二次验证会话代次，避免停止或切换连接后的旧任务写入组状态。
  - 旧单点监控链路仍保留；其 MonitorLogic.evaluate() 与调度标记同样纳入会话有效性和同步边界。

### 2.2 异步测试连接不会覆盖新连接

- ShelfPointMonitorApp.testSelectedProfile()、applyTestConnectionSuccess()、applyTestConnectionFailure()
  - 新增 connectionOperationGeneration。
  - 每次测试连接创建独立操作代次；保存、删除、设为当前连接、停止监控、切换连接和关闭窗口都会使旧操作失效。
  - 异步回调仅在操作代次仍有效时应用连接或展示失败；失效回调只清零密码快照，不覆盖当前连接、运行状态或健康状态。

### 2.3 弹窗与停止/切换一致

- ShelfPointMonitorApp.stopMonitoring()、closeActiveAlertDialog()、isCurrentActiveDialog(...)
  - 停止监控或切换连接会关闭当前报警弹窗并清空归属，不写入“已关注”或“恢复”事件。
  - 点位组弹窗和旧单点弹窗均记录创建时的监控代次；过期弹窗的“已关注”操作不会再修改新的运行状态或写事件。

### 2.4 执行器隔离

- ShelfPointMonitorApp.refreshSystemHealthStatus(...)
  - 健康检查始终投递到 ioExecutor，文件存在性、可读性和可写性检查不再占用 monitorExecutor。
  - UI 标签更新仍经 Swing EDT。
- appendStatus()、检测 CSV 日志、事件 CSV 日志继续经 ioExecutor 投递；日志失败只更新内存 UI，不递归调用 appendStatus()。

### 2.5 统一脱敏扩展

- ShelfPointMonitorApp.sanitizeSensitiveText(...)
  - 新增裸用户名、Bearer token、空格分隔的 password/token/secret、正斜杠 Windows 路径和 java.base/... 栈帧脱敏。
  - 保留可操作分类：连接超时、认证失败、连接被拒绝、配置读取失败、日志目录不可写、查询失败。
  - 弹窗、状态栏、运行日志、检查 CSV、事件 CSV、日志页和诊断文本均复用同一脱敏入口或其已脱敏输出。

## 3. ShelfPointMonitorApp.java 源码完整性证据

test/com/local/monitor/SourceHygieneTest.java 读取生产源码并断言以下痕迹不存在：

- WARNING - Removed try catching itself
- possible behaviour change
- new String[0]、new LinkOption[0]、new FileAttribute[0]、new OpenOption[0]
- 空 catch 块
- JPanel jPanel、JButton jButton、JLabel jLabel
- String string、Object object、char[] cArray、int n

本轮最终构建中 SourceHygieneTest PASS，且额外执行的 javac -Xlint:unchecked 对全部生产 Java 源码无输出、退出码为 0。

## 4. 监控会话生命周期

~~~text
开始监控
  -> 复制 DbConfig 和当前内存密码
  -> 创建 MonitoringSession(generation)
  -> 每个后台检测任务复制独立密码
  -> 查询、计算、状态发布前检查 session/generation
  -> finally 清零任务密码

停止监控 / 切换连接 / 关闭窗口
  -> 失效连接测试代次
  -> 递增监控 generation
  -> 关闭 MonitoringSession 并清零会话密码
  -> 关闭所有在途一次性会话
  -> 清空组运行状态和旧报警弹窗
  -> 旧任务的状态、日志和 UI 发布门禁均失效
~~~

不使用 Thread.stop()，不在 EDT 上等待后台任务结束。

## 5. 停止、切换连接、关闭窗口的竞态处理

- 点位组检测：stopMonitoring() 与 GroupRuntimeState、组状态表、最近检测状态共享 groupMonitorLock，会话失效和状态清理先于后续旧任务的状态提交。
- 旧单点检测：旧链路保留，但其告警计算和调度标记同样在锁内重验会话有效性。
- 日志和 UI：队列任务执行前及 EDT 回调中使用同一代次门禁；停止后不会生成新的缺料报警、恢复、查询失败或界面刷新。
- 弹窗：停止、连接切换、窗口 dispose() 都通过 stopMonitoring() 清理旧弹窗；弹窗关闭本身不触发确认事件。

## 6. 执行器隔离设计

| 执行器 | 职责 |
| --- | --- |
| monitorExecutor | 自动检测、立即检测、点位组/旧单点报警计算 |
| ioExecutor | 状态日志写入、CSV 写入、日志读取、日志清理、自检、诊断导出、健康检查、打开日志目录 |

appendStatus() 先安排 EDT UI 更新，再投递文件写入；ioExecutor 关闭后的提交被安全忽略，不会影响关闭流程。日志文件写入失败只显示已脱敏错误，不会再次写日志。

## 7. 错误脱敏规则

以下内容不应进入用户可见弹窗、状态栏、日志页、monitor.log、check-log.csv、event-log.csv 或诊断文本：

- 密码、token、secret、Bearer token；
- 数据库用户名及 uid=、user id=、role ... 形式；
- IPv4、完整 JDBC URL；
- Windows 绝对路径（反斜杠或正斜杠）；
- Java 栈帧、.java:行号、java.base/... 模块栈帧。

脱敏后仍保留现场可操作分类，而不输出原始堆栈。

## 8. 新增或增强测试

| 测试类 | 覆盖内容 |
| --- | --- |
| MonitoringSessionTest | 会话配置/密码复制、关闭清零、任务级密码副本清零、连接切换关闭旧会话 |
| MonitoringSessionRaceTest | 停止后旧任务不写查询失败/日志/状态；切换连接后旧任务不覆盖新状态；停止后旧测试连接回调不覆盖当前连接 |
| ExecutorSeparationTest | appendStatus() 异步写入、写入失败不递归、关闭 I/O 执行器安全、日志和健康检查只走 ioExecutor、真实阻塞 I/O 不占用监控执行器、关闭两个执行器 |
| ErrorSanitizationTest | 裸用户名、密码、token、secret、Bearer token、IPv4、JDBC URL、反斜杠/正斜杠路径、普通和模块化 Java 栈帧脱敏 |
| ShelfPointMonitorAppUiTest | 查询失败关闭同组弹窗、不关闭其他组弹窗、停止监控关闭旧弹窗且不写确认事件 |
| SourceHygieneTest | 指定反编译式源码痕迹不存在 |

既有查询失败、恢复后重新计时、同组弹窗关闭、只读连接和本地 H2 测试均在全量构建中继续通过。

## 9. 构建结果

执行命令：

~~~powershell
.\build.ps1
~~~

最终退出码：0。

最终输出摘要：

~~~text
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
MonitoringSessionRaceTest PASS
ErrorSanitizationTest PASS
ExecutorSeparationTest PASS
SystemHealthStatusTest PASS
SourceHygieneTest PASS
ShelfPointMonitorAppUiTest PASS
ShelfPointMonitorSelfTestTest PASS
local test database reset: ...\dist\ShelfPointMonitor\data\local-test-db
Built: ...\dist\ShelfPointMonitor
Version: 0.4.0
Zip: ...\dist\ReadonlyBusinessDbTool-v0.4.0.zip
~~~

build.ps1 的既有行为会重建本地 dist 目录和 zip；这些产物未加入 Git 暂存区，也不作为本轮 RC 交付。

## 10. git diff --check 结果

~~~text
warning: in the working copy of '.gitignore', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'build.ps1', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/com/local/monitor/ColumnInfo.java', CRLF will be replaced by LF the next time Git touches it
warning: in the working copy of 'src/com/local/monitor/DbMetadataRepository.java', CRLF will be replaced by LF the next time Git touches it
~~~

退出码：0。上述为既有工作区换行符提示，未发现空白错误。

## 11. 未确认项与边界

- 本地 H2 测试不能证明真实 PostgreSQL 网络断线、驱动超时和认证错误的精确时序；本轮没有连接或探测真实生产数据库。
- 本轮未修改数据库账户权限。代码继续通过 ReadOnlyConnectionFactory 访问生产数据，但数据库账户是否强制只读仍需由现场数据库管理员配置和验证。
- LocalTestDatabase 的写入仅用于本地 H2 测试库初始化；未新增任何生产数据库写入路径。

## 12. git status --short 完整输出

~~~text
 M .gitignore
 M build.ps1
 A docs/ai/04-full-ui-delivery-report.md
 A docs/ai/05-ui-acceptance-fix-report.md
 A docs/ai/06-field-readiness-stability-report.md
 A docs/ai/PUBLIC_FIELD_STABILITY_FIX_REPORT.md
 M src/com/local/monitor/ColumnInfo.java
 M src/com/local/monitor/DbMetadataRepository.java
 M src/com/local/monitor/GroupAlertStatus.java
 M src/com/local/monitor/GroupConfigStore.java
 M src/com/local/monitor/GroupLogWriter.java
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
 M test/com/local/monitor/GroupLogWriterTest.java
 M test/com/local/monitor/GroupMonitorLogicTest.java
 A test/com/local/monitor/MonitoringSessionTest.java
 A test/com/local/monitor/PointDataQueryRepositoryTest.java
 M test/com/local/monitor/ShelfPointMonitorAppUiTest.java
 A test/com/local/monitor/SystemHealthStatusTest.java
 A test/com/local/monitor/UiPreferencesStoreTest.java
?? CLAUDE.md
?? docs/ai/01-project-baseline.md
?? docs/ai/02-query-failure-diff.patch
?? docs/ai/02-query-failure-implementation.md
?? docs/ai/04-full-ui-delivery-diff.patch
?? docs/ai/05-ui-acceptance-fix-diff.patch
?? docs/ai/06-field-readiness-stability-diff.patch
?? docs/ai/CLAUDE_HANDOFF_AUDIT.md
?? docs/ai/PUBLIC_FIELD_STABILITY_FIX_DIFF.patch
?? test/com/local/monitor/MonitoringSessionRaceTest.java
?? test/com/local/monitor/SourceHygieneTest.java
~~~

PUBLIC_FIELD_STABILITY_FIX_REPORT.md 以 intent-to-add 方式仅用于将报告纳入本轮原始 diff；未提交任何内容，且 build、dist、lib、logs、data、diagnostics、.obsidian 均未加入暂存区。

## 13. 尚未完成的 UI 布局项

点位组管理尚未完成最终视觉美化。
数据源浏览器尚未完成最终视觉美化。
整体 UI 美化尚未开始。
RC 打包尚未开始。
