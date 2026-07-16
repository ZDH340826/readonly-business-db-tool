# v0.5.0-rc.1 现场试运行交付设计

## 设计批准与基线

本设计以用户提供的自主执行规范为批准来源，不再等待逐阶段确认。

- 项目：Windows Java Swing 只读业务数据库工具。
- 基线提交：`f1443c7132b1c18ba5662d2ef94d58545e7acc29`。
- 开发分支：`codex/final-field-delivery`。
- 目标版本：`0.5.0-rc.1`。
- 目标发布包：`dist/ReadonlyBusinessDbTool-v0.5.0-rc.1.zip`。
- 禁止访问真实生产数据库、推送远程仓库或创建 Pull Request。

## 设计路线

采用增量抽取与统一主题，不整体重写 `ShelfPointMonitorApp`：

1. 保留查询失败、报警状态机、MonitoringSession、执行器分离和脱敏入口。
2. 把 Swing 视觉令牌、通用组件和页面布局逐步移出主类。
3. 页面继续调用现有真实方法和 Repository，不复制业务逻辑，不创建静态假页。
4. 每次抽取先增加失败测试，再做最小实现，并运行完整构建。
5. 发布工程通过项目内 Java/PowerShell/批处理实现，不增加 Maven、Gradle或第三方 UI 框架。

不采用全量 UI 重写，因为它会放大竞态、密码生命周期和业务状态回归风险；不采用仅换色方案，因为它无法满足可维护架构和最终页面布局要求。

## 安全不变量

### 数据库

- 生产访问只允许参数化 `SELECT` 和 JDBC 元数据读取。
- PostgreSQL 连接继续由 `ReadOnlyConnectionFactory` 设置只读事务和超时。
- 不提供 SQL 编辑器、自由表名、自由字段名或自由排序表达式。
- `LocalTestDatabase` 中的写操作只用于本地 H2 演示库初始化和场景切换。
- 所有自动验证只使用本地 H2、fake、stub 或 mock。

### 密码和敏感信息

- 密码只存在当前运行内存和短生命周期 `char[]` 副本。
- 停止监控、切换连接、任务结束和窗口关闭必须清零密码副本。
- UI、日志、诊断包和报告不得泄露密码、Token、数据库用户名、IPv4、主机名、完整 JDBC URL、绝对路径、Java 栈帧或 `.java:行号`。
- 允许保留连接超时、认证失败、连接被拒绝、查询失败等可操作分类。

### Git 和发布

- 不提交 `build/`、`dist/`、`lib/`、`logs/`、`data/`、`diagnostics/`、`.obsidian/`、`.worktrees/` 或数据库文件。
- 只创建本地提交，不推送、不创建 PR、不重写历史。

## 目标架构

### 业务层

现有模型、状态机和 Repository 保持 Swing 无关：

- `GroupMonitorLogic`：缺料与查询失败语义。
- `GroupRuntimeState`：持续时间、确认和恢复状态。
- `ReadOnlyConnectionFactory`：只读连接边界。
- `PointRepository`、`PointDataQueryRepository`、`DbMetadataRepository`：固定只读查询。
- `GroupConfigStore`、`ConnectionProfileStore`、`UiPreferencesStore`：本地配置，不保存密码。

### UI 基础层

新增以下小型组件，统一视觉而不承载业务状态：

- `AppTheme`：颜色、字体、间距、尺寸、UIManager 默认值。
- `UiFactory`：按钮、表格、滚动区、空状态、表单标签和分隔面板。
- `SectionCard`：标题、说明与内容容器。
- `MetricCard`：统计标题、数值和状态强调色。
- `StatusBadge`：颜色与中文文字同时表达状态。
- `AppShell`：210px 导航、56px 顶栏、28px 底栏和中心 CardLayout。

### 页面层

八个页面各自负责布局和组件装配，真实动作仍通过回调进入 `ShelfPointMonitorApp` 的现有方法：

- `OverviewPage`
- `GroupManagementPage`
- `AlertCenterPage`
- `ConnectionManagementPage`
- `DataQueryPage`
- `DataSourceBrowserPage`
- `LogsSystemPage`
- `SystemSettingsPage`

页面类不创建数据库连接、不拼 SQL、不读取可变密码字段。页面事件只触发明确回调；耗时回调由主类现有 `monitorExecutor` 或 `ioExecutor` 调度，Swing 更新回到 EDT。

## 应用外壳和视觉规范

- 推荐窗口：`1440 × 900`；最小窗口：`1180 × 760`。
- 左导航：约 `210px`；顶栏：约 `56px`；底栏：`28px`。
- 页面背景：`#F4F7FB`；卡片：白色；边框：`#DDE4EE`。
- 主色：`#2563EB`；正常：`#16803A`；观察中：`#C56A00`；报警：`#C62828`；查询失败：`#A13A74`；已关注：`#52657A`；恢复：`#087F8C`；停用：`#6B7280`。
- 字体优先 `Microsoft YaHei`，回退 `SansSerif`。
- 表格统一表头、行高、选中背景、只读视觉和空状态。
- 按钮统一主要、次要、危险三级；禁用状态必须可辨识。
- 所有状态同时显示颜色和中文文字，不只依赖颜色。

## 八页信息架构

### 监控总览

上方四个统计卡；中央状态表；右侧真实详情；底部操作条。`QUERY_FAILED` 固定显示“查询失败，数据不可用”，不显示为无料、正常或已恢复。

### 点位组管理

左侧区域/点位组树；中部基本信息；右侧报警规则；下方点位配置表和状态看板。保存继续使用 `GroupConfigStore.validate` 的中文校验，不绕过 groupId、使用位、阈值、重复点位和周期校验。

### 报警中心

五个状态筛选：活跃报警、已关注、观察中、查询失败、已恢复。实时状态与历史恢复分离；操作目标只使用 `groupId`。

### 连接管理

左连接列表、中配置表单、右测试结果与安全说明。密码框只向当前测试或会话创建短生命周期副本；不显示 JDBC URL。

### 数据查询

固定 `tcs_map_data` 的结构化条件、参数化 SELECT/COUNT、分页、最大行数、LIKE 转义和 UTF-8 CSV 导出。条件变化后回第一页；导出只导出当前结果并经过脱敏和 CSV 转义。

### 数据源浏览器

左对象树、中元数据与列、右前 100 行只读预览。标识符白名单、固定上限、无 SQL 编辑或写入按钮。

### 日志与系统

一套动态健康卡；最近 1000 条事件、1000 条检测、200 行运行日志；真实筛选、自检、日志目录和脱敏诊断包。

### 系统设置

只展示并保存真实生效设置；只读、脱敏、无 SQL 编辑和禁止密码持久化作为不可关闭安全项。

## 诊断包设计

诊断包由独立 `DiagnosticBundleService` 生成到 `diagnostics/diagnostic-YYYYMMDD-HHmmss.zip`，只包含：

- `VERSION`
- `preflight-latest.txt`
- 脱敏后的 `check-log.csv`
- 脱敏后的 `event-log.csv`
- 脱敏后的 `monitor.log`
- 仅含版本、Java、操作系统、架构和时间的 `environment.txt`

服务禁止复制连接配置、点位组配置、H2 文件、JAR、runtime、Git 数据或任何绝对路径。所有文本逐行经过统一脱敏器。

## 离线部署检查设计

新增 Java 入口 `FieldDeploymentPreflight`，不访问 PostgreSQL，只检查发布目录、内嵌 runtime、依赖 JAR、目录读写、配置无密码键、组配置规则和本地 H2 只读查询。中文批处理脚本调用内嵌 Java；失败返回非零退出码并写入 `diagnostics/preflight-latest.txt`。

## 发布目录

`dist/ShelfPointMonitor/` 至少包含：

- `启动工具.bat`
- `现场部署检查.bat`
- `生成诊断包.bat`
- `ShelfPointMonitor.jar`
- `runtime/`
- `lib/`
- `data/`
- `logs/`
- `diagnostics/`
- `VERSION`
- `现场运维交付手册.md`
- `现场验收清单.md`
- `回滚说明.md`

脚本先使用相对路径 `runtime\bin\java.exe`，仅在 runtime 缺失时回退系统 Java；使用 `%~dp0` 支持中文和空格路径；保留退出码；不要求管理员权限或网络。

## 测试与验收

### 自动测试

- 保留现有 21 个入口。
- 新增主题、页面结构、CSV 导出、诊断包、预检、只读 SQL、中文/空格路径脚本与发布清单测试。
- 每项行为按 RED → GREEN → REFACTOR 执行。
- 每阶段完整执行 `./build.ps1` 和 `git diff --check`。

### 本地运行验收

- 使用 H2 演示库验证八页真实操作。
- 把发布目录复制到同时含中文和空格的临时路径，直接调用内嵌 runtime 执行 self-test、预检和诊断包。
- 临时清除 `PATH` 中的系统 Java 后调用中文启动脚本的非交互自检参数，证明不依赖系统 JDK。
- 解压最终 ZIP 后重复清单、哈希、脚本和敏感信息检查。

## 错误处理

- 后台异常先分类，再统一脱敏；UI 只显示可操作分类。
- I/O 失败不得递归写日志或阻塞监控执行器。
- 预检失败明确输出 `[失败]` 并返回非零；警告不掩盖失败。
- 没有独立测试 PostgreSQL 时，相关项在文档标记“现场待确认”，不得声称现场验证通过。

## 完成判定

只有完整构建、全部测试、八页真实功能、统一视觉、只读和脱敏证据、内嵌运行时、中文脚本、中文/空格路径、本地预检、诊断包、文档、RC ZIP、SHA256 和干净工作区全部有直接证据时，才宣告完成。
