# 自主执行状态

- 基线提交：`f1443c7132b1c18ba5662d2ef94d58545e7acc29`
- 当前提交：`22f445a7d3fd2c2108808d491a128e1199e447ab`（版本与运维文档提交前）
- 当前分支：`codex/final-field-delivery`
- 当前阶段：阶段 9 — RC 版本、运维文档与最终证据
- 当前版本：`0.5.0-rc.1`
- 当前构建结果：通过，退出码 0
- 当前测试结果：35/35 测试入口通过
- `git diff --check`：通过
- 已完成项：备份分支与标签、设计规格与计划、20 项 P0/P1 复审、H2 数据库级只读保护、统一主题与应用外壳、八个真实页面、CSV 导出、脱敏诊断包、27 项现场场景、损坏配置回退、中文/空格路径、离线预检、三个中文脚本、包内自检和内嵌运行时
- 未完成项：完成 RC 文档构建验证、最终中文/空格路径解压验收、ZIP 清单与敏感扫描、SHA256、最终报告与补丁
- 阻塞项：无
- 下一步：完成 RC 文档提交并执行最终交付证据审计
- 最后更新时间：2026-07-14（Asia/Shanghai）
- 是否访问生产数据库：否
- 是否推送远程仓库：否
- 是否创建 Pull Request：否

## 阶段 1：20 项安全复审证据

| # | 结论 | 证据 |
|---:|---|---|
| 1 | 通过 | 自动监控调度捕获 `MonitoringSession`；后台查询只读取 `session.config()` 和任务密码副本。`MonitoringSessionTest` 验证启动后修改 `currentPassword` 不影响会话。 |
| 2 | 通过 | 自动任务不读取变化中的 `currentProfile`；`startMonitoring()` 在 EDT 创建 `DbConfig` 快照并交给会话。 |
| 3 | 通过 | 两处“立即检测”按钮直接在 EDT 调用 `checkNow()`；该方法在提交后台任务前创建配置、组和 `MonitoringSession` 快照。 |
| 4 | 通过 | `MonitoringSession.copyPasswordForTask()` 每次返回独立数组；`MonitoringSessionTest` 验证任务间不能互相修改密码。 |
| 5 | 通过 | 新、旧两条检测路径都在 `finally` 调用 `clearTaskPassword()`；一次性会话也在 `finally` 清零。 |
| 6 | 通过 | `MonitoringSessionRaceTest.stoppedSessionCannotRecordQueryFailureAfterFetchReturns` 验证停止后不写事件日志。 |
| 7 | 通过 | 同一竞态测试验证旧任务不写 `lastGroupStatuses` 或 `lastGroupEvaluations`。 |
| 8 | 通过 | 所有发布点均检查 `publicationAllowed`；竞态测试验证停止后不追加状态日志，UI 更新通过相同代际门禁。 |
| 9 | 通过 | `switchedConnectionCannotBeOverwrittenByOldTask` 验证旧任务关闭且不能覆盖新连接或新状态。 |
| 10 | 通过 | 连接测试使用 `connectionOperationGeneration`；`staleConnectionTestResultCannotOverwriteNewConnection` 验证旧回调被丢弃且密码清零。 |
| 11 | 通过 | `appendStatus()` 只更新 Swing 并通过 `enqueueIoOperation()` 投递文件写入；`ExecutorSeparationTest` 验证 100 次调用均进入 `ioExecutor`。 |
| 12 | 通过 | 日志加载、写入、健康文件检查、清理、打开日志和诊断导出均由 `runIoInBackground()` 或 `enqueueIoOperation()` 进入 `ioExecutor`。 |
| 13 | 通过 | `slowLogReadDoesNotOccupyMonitorExecutor` 用阻塞 IO 任务验证监控执行器仍能立即运行。 |
| 14 | 通过 | `windowClosing` 和 `dispose()` 均调用安全停止；`disposeStopsBothExecutors` 验证两个执行器都关闭。 |
| 15 | 通过 | `GroupMonitorLogicTest` 验证查询失败为独立 `QUERY_FAILED`，不匹配缺料规则、不弹业务报警并清除旧计时。 |
| 16 | 通过 | 事件生成先单独写 `QUERY_RECOVERED`，仅真实业务恢复写 `RECOVERED`；UI 回归测试验证普通正常和查询恢复不进入业务恢复筛选。 |
| 17 | 通过 | `ErrorSanitizationTest` 与新增 `SensitiveDataSanitizationTest` 覆盖用户名、密码、令牌、IPv4、主机名、JDBC URL、绝对路径和 Java 帧，同时保留中文错误分类。 |
| 18 | 通过 | 新增 `ReadOnlyConnectionTest` 扫描生产数据库源码无写 SQL；PostgreSQL 保留 JDBC 只读和只读事务，H2 监控连接新增 `ACCESS_MODE_DATA=r` 并实测拒绝 DDL。 |
| 19 | 通过 | 发现并修复 `ConfigStore` 空 `catch`；`SourceHygieneTest` 现扫描全部生产 Java 源码并禁止空 `catch`。 |
| 20 | 通过 | `SourceHygieneTest` 禁止已知反编译注释、机械变量名和数组样板；全源码扫描无命中。超大 UI 类的可维护性改进已列入阶段 2–4，不改变本阶段安全结论。 |

## 阶段 1 最小修复

- `ReadOnlyConnectionFactory`：本地 H2 监控连接在初始化完成后以 `ACCESS_MODE_DATA=r` 重新打开；场景重置仍只存在于隔离的 `LocalTestDatabase` 工具路径。
- `ConfigStore`：读取失败时清空可能已部分载入的属性并回落到既有默认配置，不再吞掉异常后使用半成品状态。
- 构建证据：`build.ps1` 退出码 0，23/23 测试入口通过；`git diff --check` 通过。

## 阶段 2–8 完成摘要

- UI 基础与外壳：共享主题、按钮/表格工厂、卡片、状态徽标、固定导航/顶栏/底栏和窗口尺寸。
- 八页交付：总览、点位组、报警、连接、数据查询、数据源浏览、日志与系统、系统设置均使用真实组件与回调。
- 数据与诊断：结构化只读查询、当前页 UTF-8 CSV、100 行预览、六文件白名单脱敏诊断 ZIP。
- 现场加固：27 项场景证据、配置损坏安全回退、中文/空格路径 H2/CSV/ZIP、预检失败语义和批处理无 BOM/CRLF。
- 发布工程：内嵌 runtime 优先、系统 Java 明确回退、三个中文脚本、退出码保持、包内自检/预检/诊断实际执行。
- 最新构建证据：`build.ps1` 退出码 0，35/35 测试入口通过；包内自检、预检、诊断命令通过；`cmd.exe` 直接运行 `现场部署检查.bat` 退出码 0。
