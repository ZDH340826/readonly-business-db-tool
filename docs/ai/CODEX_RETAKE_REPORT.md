# Codex 项目重新接管报告

接管日期：2026-07-14
项目目录：`<PROJECT_ROOT>`

## 1. 当前项目版本

- 当前版本：`0.4.0`。
- 版本证据：根目录 `VERSION`、`README.md`、`CHANGELOG.md` 和 `docs/releases/v0.4.0.md` 一致。
- 当前 `HEAD`：`a6cde7b Strengthen packaged self-test`。
- `HEAD` 同时是标签 `v0.4.0`、`main` 和 `origin/main` 指向的提交。
- 项目是 Windows Java Swing + JDBC 只读业务数据库工具；构建由 `build.ps1` 直接调用 JDK 完成，无 Maven/Gradle。

## 2. 当前 Git 状态

接管检查时执行 `git status --short`，输出如下：

```text
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
```

说明：

- 工作区非常脏；相对 `HEAD` 的已跟踪/intent-to-add 差异为 30 个文件、`6525` 行新增、`1301` 行删除。
- `git diff --cached --stat` 无输出，说明没有实际暂存内容；状态中的 `A` 是前序任务用 `git add -N` 建立的 intent-to-add。
- 接管检查时另有 11 个未跟踪文件；本报告生成后会再新增 `docs/ai/CODEX_RETAKE_REPORT.md` 这一未跟踪文档。
- `build/`、`dist/`、`lib/`、`logs/`、`data/` 等本地产物仍被忽略，未进入状态列表。
- `git diff --check` 退出码为 `0`，只有 `.gitignore`、`build.ps1`、`ColumnInfo.java`、`DbMetadataRepository.java` 的既有 CRLF/LF 提示，没有 whitespace error。

## 3. 当前分支

- 当前分支：`fix/public-field-stability`。
- 该分支没有配置上游跟踪分支。
- 分支自身没有新提交，仍停在 `a6cde7b`，与 `main`、`origin/main`、`v0.4.0` 相同。
- reflog 显示阶段分支切换顺序为：
  `main` → `fix/query-failure-visibility` → `feature/full-ui-delivery` → `fix/full-ui-acceptance` → `fix/field-readiness-stability` → `fix/public-field-stability`。
- 这些阶段分支目前全部仍指向同一个 v0.4.0 提交；阶段成果主要存在于共享的未提交工作区，而不在各分支提交历史中。

## 4. 最近提交记录

执行 `git log --oneline -20`，输出如下：

```text
a6cde7b Strengthen packaged self-test
bcbae12 Prepare v0.4.0 release docs
c895068 Clarify group alert actions
949412e Use operator alert language
a8c49f3 Prevent point dashboard card overlap
caee54e Render point status dashboard
b6e639a Keep group checks off Swing state
0f45ad5 Harden group monitoring scheduler
e8c7003 Schedule groups by check interval
110a05f Use Chinese group alert UI text
afe395c Preserve real-time group evaluation semantics
86bd0b9 Use elapsed time for group alerts
04a69f1 Keep group config fixture changes scoped
7f6cf80 Add backup threshold rule compatibility
ddcefcc Cover abnormal point status mapping
edbb280 Add point material status mapper
913df61 Ignore local worktrees
29af186 Add v0.4.0 dashboard implementation plan
df3687d Add v0.4.0 dashboard design spec
90ab335 Prepare v0.3.0 release docs
```

## 5. 已完成阶段

### 已提交并发布的阶段

1. `v0.1.0`：只读数据库工具第一阶段，包含连接管理、数据库浏览器、点位报警和本地 H2。
2. `v0.2.0`：点位组规则报警、确认/恢复状态、CSV 日志和中文说明书。
3. `v0.3.0`：现场连接配置、`public.tcs_map_data` 只读映射、中文连接 UI 和凭据本地化边界。
4. `v0.4.0`：点位状态看板、每组检测周期、真实经过时间报警、中文业务状态和打包自检；已打标签 `v0.4.0`。

### 已实现但尚未提交的阶段

1. 查询失败可见性与语义修正：`QUERY_FAILED` 独立状态、同组弹窗归属、恢复后重新计时、统一错误脱敏。
2. 完整 8 页 UI：监控总览、点位组管理、报警中心、连接管理、结构化数据查询、数据源浏览器、日志与系统、系统设置。
3. UI 验收修正：EDT/后台边界、查询分页、报警中心五类状态、动态健康卡、关键三栏布局。
4. 现场稳定性修正：监控会话密码快照、停止/切换连接竞态、`monitorExecutor`/`ioExecutor` 分离、错误脱敏、健康状态检查。
5. Public 现场稳定性修正：会话代次门禁、异步测试连接防旧回调覆盖、停止/切换时弹窗一致性、异步日志写入、源码卫生检查。

### 当前构建状态

- 2026-07-14 实际执行 `./build.ps1`：退出码 `0`。
- 21 个测试入口全部输出 `PASS`，包括 `MonitoringSessionRaceTest`、`SourceHygieneTest` 和 `ShelfPointMonitorAppUiTest`。
- 构建成功生成 `dist/ShelfPointMonitor` 和 `dist/ReadonlyBusinessDbTool-v0.4.0.zip`。
- 额外执行发布包 `--self-test`：输出 `ShelfPointMonitor SELF_TEST_OK`，退出码 `0`。
- 指定反编译残留模式复查无匹配。

结论：之前的自主开发计划已经执行到“Public 现场稳定性修复实现与本地验证完成”阶段；功能实现和自动化验证已完成，但版本控制收尾、现场验收和 RC 流程尚未完成。

## 6. 未完成阶段

1. 后续五个修复/交付阶段均未形成提交，也未推送、合并或创建 PR；当前分支在提交图上仍等同于 v0.4.0。
2. `MonitoringSessionRaceTest.java` 和 `SourceHygieneTest.java` 仍未跟踪；它们虽被当前 `build.ps1` 执行，但不在 Git 提交或现有累计补丁中得到可靠保护。
3. 点位组管理和数据源浏览器的最终视觉美化未完成；整体 UI 美化尚未开始。
4. RC 打包/候选发布流程尚未开始；当前生成的 zip 只是本地构建产物，不应视为新的 RC。
5. 尚未在安全的非生产 PostgreSQL 环境验证网络断线、认证失败、超时、元数据兼容性和大表 `COUNT(*)` 性能。
6. 尚未由现场数据库管理员确认生产账号在数据库权限层面强制只读。
7. `CLAUDE.md` 的 Current Priority 和 v0.4.0 实施计划复选框已经落后于实际进度，后续需要在获得修改授权后同步。

## 7. 当前最大风险

最大风险是**大量已完成成果只存在于单一未提交工作区**。

当前 30 个差异文件包含约 `6525` 行新增和 `1301` 行删除，核心 `ShelfPointMonitorApp.java` 本身改动约 `3285` 行新增、`1278` 行删除；同时存在未跟踪的竞态测试和源码卫生测试。因为当前分支没有任何新增提交，也没有上游分支保护，一次误操作、工作区损坏或后续冲突就可能丢失或混淆多阶段成果。构建通过只能证明当前目录快照可用，不能替代版本控制保护和逐阶段审查。

次要风险是本地 H2 与单元/UI 测试无法覆盖真实 PostgreSQL、现场网络和数据库权限配置；因此当前状态不能直接宣称为可发布 RC。

## 8. 下一步建议

1. 在不改业务逻辑的前提下，先完整审查当前差异和未跟踪文件，确认没有敏感信息、生成物或跨阶段污染。
2. 将当前成果按“查询失败修复 / UI 交付与验收 / 现场稳定性 / Public 竞态与源码卫生 / 文档”拆成可审查提交；优先把两个未跟踪关键测试纳入版本控制。
3. 每个提交后重新运行 `./build.ps1`、发布包 `--self-test`、`git diff --check` 和公开仓库隐私扫描。
4. 对 `ShelfPointMonitorApp.java` 的大规模差异做专项代码审查，重点检查停止监控、切换连接、异步回调、日志副作用和密码数组清零的所有路径。
5. 在隔离的非生产 PostgreSQL 测试目标上完成断线、认证失败、超时、元数据和大表分页/计数验证；不要连接或扫描真实生产数据库。
6. 完成视觉验收、更新过时开发文档后，再决定是否进入 RC、推送分支和创建 PR。
