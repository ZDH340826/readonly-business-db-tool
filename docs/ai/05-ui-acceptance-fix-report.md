# 1. P1 问题修复方法

本轮在 `ShelfPointMonitorApp`、`PointDataQuery`、`PointDataQueryRepository`、`PointDataQueryResult`、`GroupConfigStore` 和相关测试中完成验收修正。当前版本仍是验收修正版，不定义为可发布 RC。

- Swing 线程边界：结构化点位查询改为 EDT 采集字段、构造 `PointDataQuery`、复制密码快照，后台任务只接收 `DbConfig`、`PointDataQuery`、`passwordSnapshot`，查询结束后 `finally` 清零密码快照，表格和分页状态回 EDT 更新。
- 报警中心语义：新增内部行模型 `AlertCenterEntry`，当前运行态筛选只来自 `lastGroupEvaluations`；已恢复只来自 `event-log.csv` 的真实 `RECOVERED` 事件；操作按 `groupId` 定位。
- 查询分页：新增固定 `COUNT(*)` 查询，分页显示上一页、下一页、当前页、总页数、总记录数；LIKE 中 `%`、`_`、`\` 按字面量转义。
- 日志与健康页：日志读取改为后台读取，event/check 限制 1000 行，monitor 限制 200 行；健康卡改为动态刷新字段。
- UI 关键布局：数据库浏览器增加“Schema / 表 / 视图对象树”“对象元数据与列信息”“前 100 行只读预览”；点位组管理暴露“基本信息”“报警规则”；报警中心增加可点击筛选按钮。
- 中文和脱敏：`GroupConfigStore` 残留英文校验改为中文；异常摘要和日志可见文本扩展脱敏规则，`monitor.log` 写入脱敏后的行。

# 2. EDT 与后台任务边界

- EDT：读取 Swing 表单字段、读取当前选择、更新 Swing 表格、更新标签、弹窗和错误提示。
- 后台：数据库查询、日志读取、日志清理、诊断导出、健康状态文件检查。
- 密码处理：数据库查询前在 EDT 复制 `currentPassword`，后台只使用快照，查询结束后清零。
- 关闭处理：后台 executor 改为 daemon thread，并在 `dispose()` 中关闭，避免 UI 测试和窗口关闭后遗留线程。

# 3. 报警中心五类状态语义

- 活跃报警：当前 `ACTIVE_ALERT`，来自实时 `lastGroupEvaluations`，允许标记已关注。
- 已关注：当前 `ACKED_ALERT`，来自实时状态，只展示已人工关注但未恢复的组。
- 观察中：当前 `PENDING_ALERT`，来自实时状态，不允许当成已报警处理。
- 查询失败：当前 `QUERY_FAILED`，表示本次未获得点位状态，不按无料处理，不允许确认缺料报警。
- 已恢复：只展示 `event-log.csv` 中真实 `RECOVERED` 事件；`QUERY_RECOVERED` 不属于缺料恢复。

# 4. 数据查询分页与 SQL 安全设计

- 数据查询固定来源仍为 `<schema>.tcs_map_data`。
- 用户不能输入表名、字段名、排序表达式或 SQL。
- SELECT 和 COUNT 使用同一组参数化过滤条件。
- 页大小由 `JSpinner(1..500)` 限制，offset 由当前页和页大小计算。
- `PointDataQuery.fixedSelectSql()` 与 `fixedCountSql()` 均为 SELECT；测试覆盖无 INSERT/UPDATE/DELETE/DDL。
- LIKE 参数通过 `escapeLikeLiteral()` 转义 `%`、`_`、`\`，SQL 模板不拼接查询值。

# 5. 三个关键布局实现

- 点位组管理：保留左侧组列表和真实新增/删除/保存/验证操作；表单暴露“基本信息”和“报警规则”；下方仍为点位配置表与状态看板。
- 数据源浏览器：左侧对象树，中部字段元数据，右侧前 100 行只读预览；保留刷新 Schema、加载表/视图、预览功能。
- 报警中心：五类筛选增加 `JToggleButton`，点击后同步下拉框并立即刷新真实数据。

# 6. 动态健康卡刷新机制

健康项包括监控调度器、当前连接、最近一次检测、配置文件、日志目录、自检状态。刷新入口包括连接状态更新、检测结束、进入日志页、手动刷新日志、自检结束；日志读取完成后也触发健康刷新。

# 7. 新增测试与构建结果

新增或扩展测试：

- `PointDataQueryRepositoryTest`：分页总数、offset、COUNT SQL、LIKE 字面量转义、SQL 只读模板。
- `GroupConfigStoreTest`：备用位阈值校验中文文案。
- `ShelfPointMonitorAppUiTest`：关键布局文本、已恢复筛选不展示未报警 NORMAL 组、报警中心操作按 `groupId`。

构建命令：

```powershell
.\build.ps1
```

构建结果：通过，退出码 0。输出摘要：

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
Built: <PROJECT_ROOT>\dist\ShelfPointMonitor
Version: 0.4.0
Zip:   <PROJECT_ROOT>\dist\ReadonlyBusinessDbTool-v0.4.0.zip
```

备注：`javac` 对反编译恢复后的 `ShelfPointMonitorApp.java` 报未检查/不安全操作提示，但构建通过。

# 8. 未确认的现场风险

- 真实 PostgreSQL 大表 COUNT 性能需要现场库验证；本地 H2 只能验证语义。
- 真实网络超时、断线、认证失败的 JDBC 异常文本种类无法完全由本地 H2 覆盖。
- 数据库浏览器对象树目前和表格选择并存，现场大量 schema/table 时的交互效率需要实机观察。

# 9. 未跟踪文件说明

工作区已有历史未跟踪文档：

```text
?? docs/ai/01-project-baseline.md
?? docs/ai/02-query-failure-diff.patch
?? docs/ai/02-query-failure-implementation.md
?? docs/ai/04-full-ui-delivery-diff.patch
```

本轮新增：

```text
docs/ai/05-ui-acceptance-fix-report.md
docs/ai/05-ui-acceptance-fix-diff.patch
```

# 10. git diff --check 结果

命令：

```powershell
git diff --check
```

结果：退出码 0。PowerShell 输出仅包含 Git 换行符提示，无 whitespace error。

# 11. git status --short 输出

```text
 M .gitignore
 M build.ps1
 A docs/ai/04-full-ui-delivery-report.md
 A docs/ai/05-ui-acceptance-fix-report.md
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
?? docs/ai/05-ui-acceptance-fix-diff.patch
```
