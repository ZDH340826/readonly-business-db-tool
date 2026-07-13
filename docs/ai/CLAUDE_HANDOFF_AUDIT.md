# Claude 现场交付审计报告

审计范围：只读分析、本地构建验证、生成本报告。**本轮未修改任何代码、未删除文件、未创建提交、未连接真实生产数据库、未访问外网、未改版本号、未生成新的发布声明。**

审计目录：`<PROJECT_ROOT>`（该目录之外还存在同名兄弟目录 `shelf-point-monitor`，二者代码状态差异很大；本轮审计范围经用户确认限定为 `-public` 目录，`shelf-point-monitor` 未纳入本次结论）。

---

## 1. 当前分支和工作区状态

- 分支：`fix/field-readiness-stability`
- `git status --short`（审计前后完全一致，本轮未产生新的改动）：

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
?? CLAUDE.md
?? docs/ai/01-project-baseline.md
?? docs/ai/02-query-failure-diff.patch
?? docs/ai/02-query-failure-implementation.md
?? docs/ai/04-full-ui-delivery-diff.patch
?? docs/ai/05-ui-acceptance-fix-diff.patch
?? docs/ai/06-field-readiness-stability-diff.patch
```

**结论：上一轮（06 报告，2026-07-06）的所有修复工作从未提交（commit），当前工作区仍处于"已修改未提交"状态。** `CLAUDE.md` 本身也是未跟踪文件（`??`），说明这份规则文档本身尚未纳入版本控制。

`git diff --check` 仅有 4 条行尾（CRLF/LF）警告，无冲突标记、无尾随空白错误，退出码 `0`。

---

## 2. 项目架构摘要

- Windows 桌面工具：Java Swing + JDBC，无 Maven/Gradle，`build.ps1` 直接调用 JDK 自带 `javac`/`jar`/`jlink`。
- 包：`com.local.monitor`，`src/` 下约 26 个类文件（含本轮未提交新增的 `PointDataQuery*`、`UiPreferences*`）。
- 核心分层：
  - `ReadOnlyConnectionFactory` — 统一只读连接创建（`setReadOnly(true)` + `SET TRANSACTION READ ONLY` + 8s 语句超时）。
  - `PointRepository` / `DbMetadataRepository` / `PointDataQueryRepository` — 只读查询层，均使用参数化 `PreparedStatement`。
  - `GroupMonitorLogic` / `GroupRuntimeState` / `GroupAlertStatus` / `GroupConfigStore` — 点位组缺料判断状态机。
  - `ShelfPointMonitorApp`（3376 行）— 主窗口，左侧导航 + 8 个卡片页面。
  - `LocalTestDatabase` — 本地 H2 测试库，用于离线验证，不依赖现场数据库。
- 版本：`VERSION` = `0.4.0`，与 `CHANGELOG.md` 最新条目一致。

---

## 3. 构建结果与退出码

命令：`.\build.ps1`（本轮实际执行，PowerShell）

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
Built: .../dist/ShelfPointMonitor
Version: 0.4.0
Zip:   .../dist/ReadonlyBusinessDbTool-v0.4.0.zip
```

**退出码：`0`。全部 19 个测试套件 PASS，无失败。** `git diff --check` 退出码同为 `0`。构建产物（`build/`、`dist/`）未纳入 Git，本轮亦未提交。

---

## 4. 当前已完成的功能

- 连接管理：多连接配置、PostgreSQL + 本地 H2，密码仅运行期内存持有。
- 只读数据库浏览器：schema/table/column 元数据 + 前 100 行预览。
- 点位组缺料报警：按区域/物料组配置使用位+备用位、独立检测周期、持续时长判断。
- 报警确认（已关注）与查询失败（`QUERY_FAILED`）作为独立状态，与恢复语义分离。
- 结构化数据查询页（无 SQL 编辑器）。
- 日志与系统健康页、系统设置页。
- 监控会话密码快照 + 清零（`MonitoringSession`）、`monitorExecutor`/`ioExecutor` 执行器分离、统一错误脱敏入口。
- 8 个 UI 页面均为真实功能页（非静态占位），点位组管理与数据源浏览器均已实现左右/三栏布局。

---

## 5. 当前阻塞问题

1. **`ShelfPointMonitorApp.java` 存在未清理的反编译风格代码**（详见第 6 节），与 `CLAUDE.md` "禁止机械化重写核心类"的铁律冲突，是本轮发现的最高优先级问题。
2. 上一轮全部修复工作停留在工作区，未提交，存在丢失风险（无 commit 保护）。
3. `CLAUDE.md` 的"Current Priority"第 5 项（UI 布局缺口）描述已过时——实际布局已完成，但文档未同步，可能误导下一位接手者重复劳动或误判进度。
4. `scheduledTask.cancel(false)` 本身仍不能中断正在执行的任务，防护完全依赖 `MonitoringSession.generation` 校验，尚未见到覆盖所有副作用点（DB 查询/写报警/写 UI）的针对性证据链，仅有单元测试名称佐证，建议下一轮做一次端到端追踪确认。

---

## 6. `ShelfPointMonitorApp.java` 源码完整性判断

**判断结论：不合格，存在大范围反编译器残留痕迹，未真正达到"人工重写"标准。**

上一份报告 `docs/ai/06-field-readiness-stability-report.md` 第 2 节声称对以下 5 个字符串做过搜索且"无输出"：`WARNING - Removed try catching itself`、`possible behaviour change`、`new String[0]`、`new LinkOption[0]`、`new FileAttribute[0]`。这个结论本身可能是真的，但**搜索范围过窄**，没有覆盖用户本轮审计清单中要求的通用反编译特征。本次用 grep 直接复核 3376 行源码，发现：

| 特征 | 出现次数 | 典型行号示例 |
|---|---|---|
| `int n` / `int n2` / `int n3` 等序列化临时变量名 | 40+ 处 | 629, 738, 875, 1018, 1135, 1221, 1237, 1391, 1412, 1448, 1464, 1505, 1554, 1680-1681, 1769, 1856, 1866, 2141-2143, 2229, 2261, 2292, 2430, 2435, 2492, 2496, 2517, 2545, 2571-2572, 2597, 2601, 2750, 2755, 2933 |
| `String string = ...` 通用命名 | 至少 1 处（含在 108 次同类模式总计内） | 2840 |
| 多余的 `(Component)` 强制类型转换（目标本身已是 `Component` 子类，转换无实际作用） | 约 100 处 | 504, 516-523, 528, 548, 830, 845, 2038 等 |
| `// empty catch block` 字面注释（典型反编译器自动生成注释，人工代码极少这样写） | 4 处 | 2962, 3183, 3226, 3245 |
| `jPanel`/`jButton`/`string`/`object`/`cArray` 类模式总计（grep 组合统计） | 108 处 | 同上 |

**评估**：`int n`/`n2`/`n3` 连续编号变量名、大量无意义 `(Component)` 显式转换、以及"// empty catch block"这种解释性注释，是 Java 反编译器（如 CFR/Vineflower/FernFlower 类工具）的典型输出特征，正常人工编写或人工重构的代码不会大量出现这种模式。这说明该文件在某个阶段很可能经过"反编译再回填"式的机械处理，而不是纯人工维护，与 `CLAUDE.md` 第 59 行"不得机械化重写或从反编译产物重写核心类"的规则相悖。

需要说明：这不代表功能不正确——本轮构建与全部 19 个测试套件均通过，业务逻辑（`QUERY_FAILED`独立状态、已关注≠恢复、只读边界等）经验证是正确的。这是一个**代码可维护性与来源纯净性**问题，不是当前已知的功能缺陷。

---

## 7. 监控会话和密码生命周期风险

- `MonitoringSession`（`ShelfPointMonitorApp.java` 约 3256-3276 行）持有 `DbConfig config`、`char[] passwordSnapshot`、`long generation`，符合"不可变快照"设计。
- 密码字段为 `char[]`（非 `String`），存在多处 `Arrays.fill(..., ' ')` 清零调用，未发现密码写入磁盘/日志/序列化的路径。
- 已实现"开始监控时在 EDT 复制一份快照，后台任务只读快照，不直接读取可变的 `currentProfile`/`currentPassword`"的模式，理论上避免了此前"停止监控后仍读到已清空密码"的问题。
- **残留风险**：`scheduledTask.cancel(false)`（第 1982、3105 行）本身不会中断已经在执行的任务实例；整个防护完全依赖已提交任务在执行时对 `generation` 做比对、发现过期就提前退出。是否所有分支（DB 查询前 / 写报警前 / 写 UI 前）都做了这个校验，本轮未做逐行调用链追踪，只能确认存在对应的单元测试（`MonitoringSessionTest` 3 个用例）且构建通过。**建议下一轮做一次显式的"停止监控 → 立刻切换连接 → 断言无残留查询/写入"集成级验证**，而不仅是依赖现有单测名称。

---

## 8. 日志 I/O 与执行器风险

- 执行器已拆分为 `monitorExecutor`（监控调度、手动检测）与 `ioExecutor`（日志读取/清理、诊断导出、自检、系统健康检查）。
- `appendStatus()` 按 06 报告描述已改为：只更新底部状态栏、追加内存状态区、写 `monitor.log`；不再触发 `loadSystemLogs()`（避免监控线程被日志页刷新阻塞）。
- 本轮未逐行重新走查 `appendStatus()`/`writeLog()` 当前实现细节（该部分本次以 grep 存在性验证 + 06 报告描述为准，未做独立行号级复核），如需强证据建议下一轮单独针对这两个方法做逐行审计。

---

## 9. UI 八页落地情况

| # | 页面 | 状态 |
|---|---|---|
| 1 | 监控总览 | 功能页，实时表格+统计卡+开始/停止/立即检测 |
| 2 | 点位组管理 | 功能页，**左树/中部基本信息/右侧报警规则/下方点位表布局已落地** |
| 3 | 报警中心 | 功能页，筛选+详情+确认/检测按钮 |
| 4 | 连接管理 | 功能页，多连接配置+测试/保存/删除 |
| 5 | 数据查询 | 功能页，结构化查询（无 SQL 编辑器）+分页 |
| 6 | 数据源浏览器 | 功能页，**左对象树/中元数据/右预览三栏布局已落地** |
| 7 | 日志与系统 | 功能页，健康卡+日志表+自检+诊断导出 |
| 8 | 系统设置 | 功能页，偏好设置+保存/重置 |

**注意**：`CLAUDE.md`（当前工作区版本）"Current Priority"第 5 项仍写着"点位组管理/数据源浏览器布局未完成"——**这是过时信息**，实测两个页面的三栏布局均已实现且有真实的选择监听器、后台加载与刷新按钮。建议下一轮同步更新该文档，避免误导。

---

## 10. 数据库只读边界检查

- `ReadOnlyConnectionFactory`：`conn.setReadOnly(true)` + `conn.setAutoCommit(false)` + 生产库执行 `SET TRANSACTION READ ONLY` + `SET statement_timeout='8s'`。
- 全仓库搜索 INSERT/UPDATE/DELETE/DDL，仅在 `LocalTestDatabase.java`（本地 H2 测试数据初始化/重置脚本）中出现，生产查询路径（`PointRepository`/`DbMetadataRepository`/`PointDataQueryRepository`）全部使用参数化 `PreparedStatement` 的 SELECT。
- 数据查询页（数据查询/数据源浏览器）均无自由文本 SQL 输入框，UI 上有"只读查询/不支持 SQL 编辑/不支持数据修改"提示文案。
- 未发现硬编码的真实生产 IP/用户名/密码；`192.0.2.10` 仅作为 UI 默认提示值/文档示例出现，非生产凭据。
- **结论：只读边界在本次审计范围内成立，未发现违规写操作。**

---

## 11. 需要优先处理的任务队列

1. **清理 `ShelfPointMonitorApp.java` 的反编译残留**（`int n*` 变量重命名为语义化名称、去除多余 `(Component)` 转换、去掉"// empty catch block"类注释并补充真实处理或明确的忽略说明）——这是违反 CLAUDE.md 铁律的最高优先级项。
2. 提交（commit）当前工作区已完成但未提交的全部改动，避免持续积压导致丢失或冲突。
3. 更新 `CLAUDE.md` 的"Current Priority"，去掉已完成的 UI 布局项，补充"源码反编译残留清理"为新的最高优先级。
4. 针对"停止监控/切换连接"场景补充一次端到端集成验证，确认 `generation` 校验真正覆盖所有副作用点。
5. 补充对 `appendStatus()`/`writeLog()` 当前实现的独立行号级复核（本轮未做，仅依赖既有报告描述）。

---

## 12. 不应触碰的文件、目录和行为

- `build/`、`dist/`、`lib/`、`logs/`、`data/`、`.obsidian/`、`.worktrees/` — 均为构建产物/本地运行数据/编辑器数据，不应提交或格式化。
- 任何含真实现场 IP、用户名、密码、JDBC URL 的本地文件（如实际使用中的 `data/connections.properties`）。
- H2 本地测试数据库文件（`*.mv.db` 等）。
- 不应连接真实生产数据库进行任何形式的测试。
- 不应修改 `VERSION`、生成新的发布包或发布声明。
- 不应删除 `test/` 下任何测试类以让构建"看起来通过"。
- 不应用静态占位页面替换任何已实现的真实功能页。

---

## 13. 下一轮建议修复方案

1. **反编译残留清理**（范围：仅 `ShelfPointMonitorApp.java`）：
   - 逐个 `int n`/`n2`/`n3` 按上下文重命名为有意义的变量名（如 `selectedIndex`、`rowCount` 等）。
   - 删除所有目标类型已是 `Component` 子类的多余 `(Component)` 转换。
   - 将 4 处 `// empty catch block` 替换为真实的日志记录或明确写出"预期可忽略"的业务原因注释，禁止保留反编译器默认注释原文。
   - 修改后必须保留现有业务逻辑不变，跑通全部 19 个测试套件，且不能引入新的 `git diff --check` 警告之外的问题。
2. 将当前工作区改动拆分为若干语义清晰的提交（例如：UI 交付、错误脱敏、执行器分离分别提交），避免一次性巨型提交。
3. 同步更新 `CLAUDE.md` 的 Current Priority 列表，反映本报告第 11 节的最新优先级排序。
4. 针对监控会话生命周期，补充一个显式的集成测试：停止监控后立即断言不再有新的 DB 查询/报警写入/UI 更新发生（而不仅是验证 `generation` 字段本身被正确设置）。
5. 对 `appendStatus()`/`writeLog()` 做一次独立的行号级复核报告，作为本报告第 8 节的补充证据。
