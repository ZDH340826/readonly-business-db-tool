# 1. 仓库概览

* 项目名称：只读业务数据库工具。证据：`README.md:1`。
* 当前版本：`0.4.0`。证据：`VERSION:1`；`README.md:3`；`build.ps1:4-10` 读取并校验 `VERSION`。
* 当前分支：`main`；远端默认分支：`origin/main`。证据：本次只读命令 `git status --short --branch` 输出 `## main...origin/main`；`git symbolic-ref --short refs/remotes/origin/HEAD` 输出 `origin/main`。
* 编程语言：Java。证据：`src/com/local/monitor/ShelfPointMonitorApp.java:62` 类 `ShelfPointMonitorApp`；`build.ps1:55-61` 使用 `javac` 编译源码和测试。
* UI 框架：Java Swing。证据：`src/com/local/monitor/ShelfPointMonitorApp.java:62` 继承 `JFrame`；`src/com/local/monitor/ShelfPointMonitorApp.java:48` 使用 `JPasswordField`。
* 构建方式：PowerShell 脚本 `build.ps1` 下载依赖、编译、测试、打包、可选 jlink、初始化本地测试库、压缩 zip。证据：`build.ps1:19-22` 依赖；`build.ps1:55-61` 编译；`build.ps1:63-88` 测试；`build.ps1:96-108` jar/jlink；`build.ps1:253-258` 初始化 H2 和压缩。
* 运行平台：Windows 本地 Java/Swing 桌面工具。证据：`README.md:5`；`build.ps1:111-163` 生成 `.bat` 启动脚本。

完整目录树，排除 `.git/`、`build/`、`dist/`、`lib/` 内二进制 jar；另有 `.worktrees/` 为本地隔离工作树，已被 `.gitignore` 排除，不作为当前仓库源码树展开。证据：`.gitignore:1-5`、`.gitignore:30-32`。

```text
shelf-point-monitor-public/
├─ .gitattributes
├─ .gitignore
├─ CHANGELOG.md
├─ README.md
├─ VERSION
├─ build.ps1
├─ .obsidian/
│  ├─ app.json
│  ├─ appearance.json
│  ├─ core-plugins.json
│  └─ workspace.json
├─ data/
│  └─ group-config.properties
├─ docs/
│  ├─ VERSIONING.md
│  ├─ ai/
│  │  └─ 01-project-baseline.md
│  ├─ manuals/
│  │  └─ point-shortage-alert-user-manual.md
│  ├─ releases/
│  │  ├─ v0.1.0.md
│  │  ├─ v0.2.0.md
│  │  ├─ v0.3.0.md
│  │  └─ v0.4.0.md
│  └─ superpowers/
│     ├─ plans/
│     │  ├─ 2026-06-29-readonly-db-tool-phase1.md
│     │  ├─ 2026-06-30-point-group-rule-alert-implementation.md
│     │  ├─ 2026-07-01-site-connection-v030-implementation.md
│     │  └─ 2026-07-03-point-status-dashboard-v040-implementation.md
│     └─ specs/
│        ├─ 2026-06-29-readonly-db-tool-phase1-design.md
│        ├─ 2026-06-30-point-group-rule-alert-design.md
│        ├─ 2026-07-01-site-connection-v030-design.md
│        └─ 2026-07-03-point-status-dashboard-v040-design.md
├─ logs/
│  ├─ check-log.csv
│  ├─ event-log.csv
│  └─ monitor.log
├─ src/
│  └─ com/local/monitor/
│     ├─ AlertState.java
│     ├─ ColumnInfo.java
│     ├─ ConfigStore.java
│     ├─ ConnectionProfile.java
│     ├─ ConnectionProfileStore.java
│     ├─ DbConfig.java
│     ├─ DbMetadataRepository.java
│     ├─ GroupAlertRule.java
│     ├─ GroupAlertStatus.java
│     ├─ GroupCheckPlanner.java
│     ├─ GroupConfigStore.java
│     ├─ GroupEvaluation.java
│     ├─ GroupLogWriter.java
│     ├─ GroupMonitorLogic.java
│     ├─ GroupMonitorPoint.java
│     ├─ GroupRuntimeState.java
│     ├─ GroupStatusText.java
│     ├─ LocalTestDatabase.java
│     ├─ LocalTestDbTool.java
│     ├─ MonitorEvaluation.java
│     ├─ MonitorLogic.java
│     ├─ PointAlert.java
│     ├─ PointDefinition.java
│     ├─ PointGroupDefinition.java
│     ├─ PointMaterialStatus.java
│     ├─ PointQuery.java
│     ├─ PointRecord.java
│     ├─ PointRepository.java
│     ├─ PointRole.java
│     ├─ PointSchedule.java
│     ├─ PointStatusMapper.java
│     ├─ PointStatusView.java
│     ├─ ReadOnlyConnectionFactory.java
│     ├─ SchemaInfo.java
│     ├─ ShelfPointMonitorApp.java
│     ├─ TableInfo.java
│     └─ TablePreview.java
└─ test/
   └─ com/local/monitor/
      ├─ ConfigStoreTest.java
      ├─ ConnectionProfileStoreTest.java
      ├─ DbMetadataRepositoryTest.java
      ├─ GroupCheckPlannerTest.java
      ├─ GroupConfigStoreTest.java
      ├─ GroupLogWriterTest.java
      ├─ GroupMonitorLogicTest.java
      ├─ LocalTestDatabaseTest.java
      ├─ MonitorLogicTest.java
      ├─ PointScheduleTest.java
      ├─ PointStatusMapperTest.java
      ├─ ShelfPointMonitorAppUiTest.java
      └─ ShelfPointMonitorSelfTestTest.java
```

# 2. 可运行入口与构建链路

* 应用主类：`com.local.monitor.ShelfPointMonitorApp`。证据：`src/com/local/monitor/ShelfPointMonitorApp.java:62` 类声明；`build.ps1:96` jar main-class。
* 应用 main 方法：`ShelfPointMonitorApp.main(String[] args)`。证据：`src/com/local/monitor/ShelfPointMonitorApp.java:164`。
* 本地测试库工具入口：`com.local.monitor.LocalTestDbTool.main(String[] args)`。证据：`src/com/local/monitor/LocalTestDbTool.java:7`；`build.ps1:127`、`build.ps1:138`、`build.ps1:149`、`build.ps1:160`。
* 启动脚本：`ShelfPointMonitor.bat`、`LocalTest_Reset.bat`、`LocalTest_Normal.bat`、`LocalTest_MissingUse.bat`、`LocalTest_MissingBackup.bat`。证据：`build.ps1:111-163`。
* 打包产物：`dist/ShelfPointMonitor/ShelfPointMonitor.jar` 与 `dist/ReadonlyBusinessDbTool-v0.4.0.zip`。证据：`build.ps1:96`、`build.ps1:257-258`；本次构建输出 `Built: ...\dist\ShelfPointMonitor`、`Zip: ...\dist\ReadonlyBusinessDbTool-v0.4.0.zip`。

`build.ps1` 完整流程：

1. 读取并校验版本号。证据：`build.ps1:4-10`。
2. 定义源码、测试、构建、发布、依赖目录。证据：`build.ps1:13-18`。
3. 指定 PostgreSQL JDBC 与 H2 jar 文件名和 Maven 下载地址。证据：`build.ps1:19-22`。
4. 解析 Java 工具，要求 JDK 17+。证据：`build.ps1:24-45`。
5. 创建 `build/classes`、`build/test-classes`、`lib` 并按需下载 jar。证据：`build.ps1:47-52`。
6. 编译生产源码。证据：`build.ps1:55-57`。
7. 编译测试源码。证据：`build.ps1:59-61`。
8. 逐个执行 13 个测试类。证据：`build.ps1:63-88`。
9. 清理并重建 `dist/ShelfPointMonitor`。证据：`build.ps1:90-94`。
10. 创建可执行 jar 并复制依赖 jar。证据：`build.ps1:96-99`。
11. 如果可用，使用 jlink 生成运行时。证据：`build.ps1:101-108`。
12. 写入 `.bat` 启动脚本。证据：`build.ps1:111-163`。
13. 写入发布包内样例配置。证据：`build.ps1:165-247`。
14. 初始化发布包内 H2 本地测试库。证据：`build.ps1:249-254`。
15. 删除旧 zip 并生成版本化 zip。证据：`build.ps1:256-258`。

版本信息：

* 本机 JDK：OpenJDK Temurin `21.0.11+10-LTS`，`javac 21.0.11`，`jlink 21.0.11`。证据：本次只读命令 `java -version`、`javac -version`、`jlink --version`。
* PostgreSQL JDBC：`42.2.25`。证据：`build.ps1:19-21`。
* H2：`2.2.224`。证据：`build.ps1:20-22`。

本次实际构建结果：成功。证据：本次执行 `.\build.ps1`，退出码 `0`，最终输出包括：

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

# 3. 模块与职责地图

| 文件路径 | 类名 | 核心职责 | 对外依赖 | 是否有测试覆盖 |
|---|---|---|---|---|
| `src/com/local/monitor/AlertState.java` | `AlertState` | 旧点位报警确认状态集合，按 alertKey 压制已确认报警。证据：`AlertState.acknowledge` `src/com/local/monitor/AlertState.java:10`。 | `java.util.Set` | 有；`MonitorLogicTest` 覆盖旧报警确认。证据：`test/com/local/monitor/MonitorLogicTest.java:47-85`。 |
| `src/com/local/monitor/ColumnInfo.java` | `ColumnInfo` | 数据库字段元数据值对象。证据：`ColumnInfo` `src/com/local/monitor/ColumnInfo.java:3`。 | 无业务外部依赖 | 有；`DbMetadataRepositoryTest` 间接覆盖字段读取。证据：`test/com/local/monitor/DbMetadataRepositoryTest.java:27-30`。 |
| `src/com/local/monitor/ConfigStore.java` | `ConfigStore` | 旧点位配置读写 `config.properties`。证据：`ConfigStore.load/save` `src/com/local/monitor/ConfigStore.java:22`、`43`。 | `java.nio.file.Files`、`Properties`、`DbConfig`、`PointDefinition` | 有；`ConfigStoreTest`。证据：`test/com/local/monitor/ConfigStoreTest.java:7-33`。 |
| `src/com/local/monitor/ConnectionProfile.java` | `ConnectionProfile` | 连接配置值对象，并转换为 `DbConfig`。证据：`ConnectionProfile.toDbConfig` `src/com/local/monitor/ConnectionProfile.java:32`。 | `DbConfig` | 有；`ConnectionProfileStoreTest` 间接覆盖保存/加载。证据：`test/com/local/monitor/ConnectionProfileStoreTest.java:7-36`。 |
| `src/com/local/monitor/ConnectionProfileStore.java` | `ConnectionProfileStore` | 多连接配置读写，不保存密码。证据：`load/save` `src/com/local/monitor/ConnectionProfileStore.java:23`、`64`。 | `Files`、`Properties`、`ConnectionProfile` | 有；`ConnectionProfileStoreTest` 断言不含 password。证据：`test/com/local/monitor/ConnectionProfileStoreTest.java:31`。 |
| `src/com/local/monitor/DbConfig.java` | `DbConfig` | 数据库连接参数与 JDBC URL 生成。证据：`DbConfig.jdbcUrl` `src/com/local/monitor/DbConfig.java:80`。 | `PointQuery.validateIdentifier` | 有；连接、元数据和本地测试类间接覆盖。证据：`test/com/local/monitor/LocalTestDatabaseTest.java:17-19`、`test/com/local/monitor/DbMetadataRepositoryTest.java:15-17`。 |
| `src/com/local/monitor/DbMetadataRepository.java` | `DbMetadataRepository` | 只读 schema/table/column 列举和表预览。证据：`listSchemas/listTables/listColumns/previewTable` `src/com/local/monitor/DbMetadataRepository.java:16`、`35`、`60`、`87`。 | `ReadOnlyConnectionFactory`、JDBC `DatabaseMetaData` | 有；`DbMetadataRepositoryTest`。证据：`test/com/local/monitor/DbMetadataRepositoryTest.java:7-37`。 |
| `src/com/local/monitor/GroupAlertRule.java` | `GroupAlertRule` | 点位组报警规则：使用位无料、备用位下限、持续时间、备用位是否参与。证据：`durationSeconds` `src/com/local/monitor/GroupAlertRule.java:56`。 | 无业务外部依赖 | 有；`GroupMonitorLogicTest` 多场景覆盖。证据：`test/com/local/monitor/GroupMonitorLogicTest.java:6-15`。 |
| `src/com/local/monitor/GroupAlertStatus.java` | `GroupAlertStatus` | 点位组报警状态枚举。证据：`src/com/local/monitor/GroupAlertStatus.java:3`。 | 无 | 有；`GroupMonitorLogicTest`、`ShelfPointMonitorAppUiTest`。证据：`test/com/local/monitor/GroupMonitorLogicTest.java:127`、`test/com/local/monitor/ShelfPointMonitorAppUiTest.java:493-497`。 |
| `src/com/local/monitor/GroupCheckPlanner.java` | `GroupCheckPlanner` | 根据每组检测周期选出到期点位组。证据：`dueGroups` `src/com/local/monitor/GroupCheckPlanner.java:13`。 | `PointGroupDefinition`、`GroupRuntimeState` | 有；`GroupCheckPlannerTest`。证据：`test/com/local/monitor/GroupCheckPlannerTest.java:10-13`。 |
| `src/com/local/monitor/GroupConfigStore.java` | `GroupConfigStore` | 点位组配置读写和默认组。证据：`load/save/defaultGroups` `src/com/local/monitor/GroupConfigStore.java:22`、`46`、`63`。 | `Files`、`Properties`、点位组模型 | 有；`GroupConfigStoreTest`，含旧格式迁移和不保存密码。证据：`test/com/local/monitor/GroupConfigStoreTest.java:7-100`。 |
| `src/com/local/monitor/GroupEvaluation.java` | `GroupEvaluation` | 一次点位组检测结果 DTO，包括状态、计时、点位状态和提示消息。证据：`GroupEvaluation` `src/com/local/monitor/GroupEvaluation.java:5`。 | `GroupAlertStatus`、`PointStatusView` | 有；`GroupMonitorLogicTest` 间接覆盖。证据：`test/com/local/monitor/GroupMonitorLogicTest.java:41-154`。 |
| `src/com/local/monitor/GroupLogWriter.java` | `GroupLogWriter` | 写入检测快照日志和事件日志 CSV。证据：`appendCheck/appendEvent` `src/com/local/monitor/GroupLogWriter.java:43`、`60`。 | `Files.newBufferedWriter`、`GroupEvaluation` | 有；`GroupLogWriterTest`。证据：`test/com/local/monitor/GroupLogWriterTest.java:8-41`。 |
| `src/com/local/monitor/GroupMonitorLogic.java` | `GroupMonitorLogic` | 点位组规则计算、连续计时、报警状态判定。证据：`evaluate` `src/com/local/monitor/GroupMonitorLogic.java:10`、`17`。 | `GroupRuntimeState`、`PointStatusMapper` | 有；`GroupMonitorLogicTest`。证据：`test/com/local/monitor/GroupMonitorLogicTest.java:6-294`。 |
| `src/com/local/monitor/GroupMonitorPoint.java` | `GroupMonitorPoint` | 点位组内单点配置，含角色、别名、启用、排序。证据：`GroupMonitorPoint` `src/com/local/monitor/GroupMonitorPoint.java:3`。 | `PointRole` | 有；`GroupConfigStoreTest` 和 UI 测试间接覆盖。证据：`test/com/local/monitor/GroupConfigStoreTest.java:95-100`。 |
| `src/com/local/monitor/GroupRuntimeState.java` | `GroupRuntimeState` | 每个点位组运行时状态：上次检测、条件首次成立、确认、弹窗已显示。证据：`acknowledge/markMatched/reset` `src/com/local/monitor/GroupRuntimeState.java:37`、`42`、`80`。 | `LocalDateTime` | 有；`GroupMonitorLogicTest`。证据：`test/com/local/monitor/GroupMonitorLogicTest.java:114-154`。 |
| `src/com/local/monitor/GroupStatusText.java` | `GroupStatusText` | 报警状态中文文案和摘要。证据：`statusText/summary` `src/com/local/monitor/GroupStatusText.java:9`、`32`。 | `GroupAlertStatus` | 有；`ShelfPointMonitorAppUiTest` 断言不泄露枚举名。证据：`test/com/local/monitor/ShelfPointMonitorAppUiTest.java:493-497`。 |
| `src/com/local/monitor/LocalTestDatabase.java` | `LocalTestDatabase` | 初始化和切换 H2 本地测试库场景。证据：`reset/setScenario/createIfMissing` `src/com/local/monitor/LocalTestDatabase.java:12`、`23`、`51`。 | JDBC、H2、`DbConfig` | 有；`LocalTestDatabaseTest`。证据：`test/com/local/monitor/LocalTestDatabaseTest.java:9-77`。 |
| `src/com/local/monitor/LocalTestDbTool.java` | `LocalTestDbTool` | 命令行入口，调用本地测试库 reset/normal/missing 场景。证据：`main` `src/com/local/monitor/LocalTestDbTool.java:7`。 | `LocalTestDatabase` | 有；`ShelfPointMonitorSelfTestTest` 和构建脚本间接覆盖。证据：`build.ps1:127-160`。 |
| `src/com/local/monitor/MonitorEvaluation.java` | `MonitorEvaluation` | 旧单点监控结果 DTO。证据：`MonitorEvaluation` `src/com/local/monitor/MonitorEvaluation.java:6`。 | `PointAlert` | 有；`MonitorLogicTest`。证据：`test/com/local/monitor/MonitorLogicTest.java:6-117`。 |
| `src/com/local/monitor/MonitorLogic.java` | `MonitorLogic` | 旧单点缺料报警逻辑。证据：`evaluate` `src/com/local/monitor/MonitorLogic.java:16`。 | `AlertState`、`PointRecord`、`PointDefinition` | 有；`MonitorLogicTest`。证据：`test/com/local/monitor/MonitorLogicTest.java:47-85`。 |
| `src/com/local/monitor/PointAlert.java` | `PointAlert` | 旧单点报警 DTO。证据：`PointAlert` `src/com/local/monitor/PointAlert.java:3`。 | 无 | 有；`MonitorLogicTest` 间接覆盖。证据：`test/com/local/monitor/MonitorLogicTest.java:47-85`。 |
| `src/com/local/monitor/PointDefinition.java` | `PointDefinition` | 旧点位定义：编码、别名、间隔分钟。证据：`PointDefinition` `src/com/local/monitor/PointDefinition.java:3`。 | 无 | 有；`ConfigStoreTest`、`PointScheduleTest`。证据：`test/com/local/monitor/PointScheduleTest.java:6-49`。 |
| `src/com/local/monitor/PointGroupDefinition.java` | `PointGroupDefinition` | 点位组定义：区域、组名、物料、周期、点位、规则。证据：`PointGroupDefinition` `src/com/local/monitor/PointGroupDefinition.java:10`。 | `GroupMonitorPoint`、`GroupAlertRule` | 有；`GroupConfigStoreTest`、`GroupCheckPlannerTest`、`GroupMonitorLogicTest`。证据：`test/com/local/monitor/GroupConfigStoreTest.java:95-100`。 |
| `src/com/local/monitor/PointMaterialStatus.java` | `PointMaterialStatus` | 点位物料状态枚举：有料、无料、未查到、停用。证据：`src/com/local/monitor/PointMaterialStatus.java:3-15`。 | 无 | 有；`PointStatusMapperTest`。证据：`test/com/local/monitor/PointStatusMapperTest.java:57-58`。 |
| `src/com/local/monitor/PointQuery.java` | `PointQuery` | 点位状态 SELECT SQL 生成、schema 标识符校验。证据：`buildSelectSql/validateIdentifier` `src/com/local/monitor/PointQuery.java:7`、`26`。 | `Pattern` | 有；`MonitorLogicTest` 校验 SQL 和注入拒绝。证据：`test/com/local/monitor/MonitorLogicTest.java:91-101`。 |
| `src/com/local/monitor/PointRecord.java` | `PointRecord` | 数据库点位状态记录 DTO。证据：`PointRecord` `src/com/local/monitor/PointRecord.java:5`。 | `LocalDateTime` | 有；`LocalTestDatabaseTest` 验证字段映射。证据：`test/com/local/monitor/LocalTestDatabaseTest.java:26-29`。 |
| `src/com/local/monitor/PointRepository.java` | `PointRepository` | 测试连接和按点位查询状态。证据：`testConnection/fetch` `src/com/local/monitor/PointRepository.java:12`、`23`。 | `ReadOnlyConnectionFactory`、`PointQuery` | 有；`DbMetadataRepositoryTest` 和 `LocalTestDatabaseTest` 间接覆盖查询路径；SQL 安全由 `MonitorLogicTest` 覆盖。 |
| `src/com/local/monitor/PointRole.java` | `PointRole` | 使用位/备用位角色枚举。证据：`src/com/local/monitor/PointRole.java:3`。 | 无 | 有；组配置、组逻辑、UI 测试间接覆盖。 |
| `src/com/local/monitor/PointSchedule.java` | `PointSchedule` | 旧单点按分钟调度。证据：`duePoints/markChecked` `src/com/local/monitor/PointSchedule.java:12`、`27`。 | `PointDefinition` | 有；`PointScheduleTest`。证据：`test/com/local/monitor/PointScheduleTest.java:6-49`。 |
| `src/com/local/monitor/PointStatusMapper.java` | `PointStatusMapper` | 将点位配置和数据库记录映射为有料/无料/未查到/停用视图。证据：`map` `src/com/local/monitor/PointStatusMapper.java:12`。 | `PointMaterialStatus`、`PointStatusView` | 有；`PointStatusMapperTest`。证据：`test/com/local/monitor/PointStatusMapperTest.java:6-86`。 |
| `src/com/local/monitor/PointStatusView.java` | `PointStatusView` | UI 用点位状态视图 DTO。证据：`PointStatusView` `src/com/local/monitor/PointStatusView.java:5`。 | `PointRole`、`PointMaterialStatus` | 有；`PointStatusMapperTest` 间接覆盖。 |
| `src/com/local/monitor/ReadOnlyConnectionFactory.java` | `ReadOnlyConnectionFactory` | 统一创建只读 JDBC 连接，设置只读事务和超时。证据：`open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:12`。 | JDBC、`DbConfig` | 有；所有数据库测试间接覆盖；无专门断言事务只读的集成测试。 |
| `src/com/local/monitor/SchemaInfo.java` | `SchemaInfo` | schema 元数据 DTO。证据：`SchemaInfo` `src/com/local/monitor/SchemaInfo.java:3`。 | 无 | 有；`DbMetadataRepositoryTest` 间接覆盖。 |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `ShelfPointMonitorApp` | Swing 主界面、连接管理、数据库浏览、点位组监控、弹窗、运行日志、自检。证据：类 `src/com/local/monitor/ShelfPointMonitorApp.java:62`；`buildUi` `:358`；`startMonitoring` `:1382`；`showGroupAlertDialog` `:1421`。 | Swing、调度器、配置存储、仓储、日志、业务逻辑 | 有；`ShelfPointMonitorAppUiTest`、`ShelfPointMonitorSelfTestTest`。证据：`test/com/local/monitor/ShelfPointMonitorAppUiTest.java:37-1175`、`test/com/local/monitor/ShelfPointMonitorSelfTestTest.java:7-107`。 |
| `src/com/local/monitor/TableInfo.java` | `TableInfo` | 表/视图元数据 DTO。证据：`TableInfo` `src/com/local/monitor/TableInfo.java:3`。 | 无 | 有；`DbMetadataRepositoryTest` 间接覆盖。 |
| `src/com/local/monitor/TablePreview.java` | `TablePreview` | 表预览结果 DTO。证据：`TablePreview` `src/com/local/monitor/TablePreview.java:7`。 | `List` | 有；`DbMetadataRepositoryTest` 间接覆盖。 |

# 4. 数据库与只读边界

## 4.1 数据库连接创建路径

* 所有生产查询连接统一通过 `ReadOnlyConnectionFactory.open(DbConfig, char[])` 创建。证据：`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:12`；`DbMetadataRepository.listSchemas/listTables/listColumns/previewTable` 调用该工厂，证据：`src/com/local/monitor/DbMetadataRepository.java:17`、`37`、`63`、`92`；`PointRepository.testConnection/fetch` 调用该工厂，证据：`src/com/local/monitor/PointRepository.java:13`、`24`。
* PostgreSQL JDBC URL 规则：`jdbc:postgresql://host:port/database?sslmode=...&connectTimeout=5`。证据：`DbConfig.jdbcUrl` `src/com/local/monitor/DbConfig.java:80-85`。
* 本地测试库 JDBC URL 规则：`jdbc:h2:file:absolutePath;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`。证据：`DbConfig.jdbcUrl` `src/com/local/monitor/DbConfig.java:80-83`。
* 本地测试库直接使用 `DriverManager.getConnection(config.jdbcUrl(), "sa", "")`。证据：`LocalTestDatabase.reset/setScenario/createIfMissing` `src/com/local/monitor/LocalTestDatabase.java:14`、`26`、`53`。

## 4.2 事务、超时、密码处理

* 连接级只读：`conn.setReadOnly(true)`。证据：`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:21`。
* 事务配置：`conn.setAutoCommit(false)` 后执行 `SET TRANSACTION READ ONLY`。证据：`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:22-25`。
* 查询超时：执行 `SET statement_timeout='8s'`。证据：`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:26`。
* 连接超时：PostgreSQL URL 中固定 `connectTimeout=5`。证据：`DbConfig.jdbcUrl` `src/com/local/monitor/DbConfig.java:85`。
* 密码处理：UI 用 `JPasswordField`，当前运行内存中保存 `char[] currentPassword`，测试连接成功后 `clone` 到内存。证据：`ShelfPointMonitorApp` 字段 `src/com/local/monitor/ShelfPointMonitorApp.java:101`、`:160`；`testSelectedProfile` `src/com/local/monitor/ShelfPointMonitorApp.java:800-805`。
* 密码转换风险：真正创建 JDBC `Properties` 时把 `char[]` 转为不可清零的 `String`。证据：`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:19`。
* 密码不落配置文件：连接配置存储类没有保存 password 字段；测试明确断言配置文本不包含 password。证据：`ConnectionProfileStore.save` `src/com/local/monitor/ConnectionProfileStore.java:64-90`；`ConnectionProfileStoreTest` `test/com/local/monitor/ConnectionProfileStoreTest.java:31`；自检读取并校验无密码属性，证据：`ShelfPointMonitorApp.assertNoPasswordProperty` `src/com/local/monitor/ShelfPointMonitorApp.java:310-313`。

## 4.3 数据库浏览器能执行的 SQL 或 SQL 模板

* 测试连接 SQL：`select current_database(), current_user`。证据：`PointRepository.testConnection` `src/com/local/monitor/PointRepository.java:15`。
* 表预览 SQL：`select * from schema.table limit N`。证据：`DbMetadataRepository.previewTable` `src/com/local/monitor/DbMetadataRepository.java:94`。
* schema/table/column 列表主要走 JDBC `DatabaseMetaData.getSchemas/getTables/getColumns`，未拼写显式业务 SQL。证据：`DbMetadataRepository.listSchemas/listTables/listColumns` `src/com/local/monitor/DbMetadataRepository.java:16-68`。
* 预览行数被限制在 1 到 500。证据：`DbMetadataRepository.previewTable` 中 `safeLimit = Math.max(1, Math.min(limit, 500))`，`src/com/local/monitor/DbMetadataRepository.java:91`。

## 4.4 点位报警能执行的 SQL 或 SQL 模板

点位报警查询模板：

```sql
select map_data_code as point_code,
       pod_code as shelf_code,
       pod_status as shelf_status,
       status,
       ind_lock as lock_state,
       area_code,
       relate_area_code as next_area_code,
       date_chg as updated_at,
       date_chg as marked_at
from <schema>.tcs_map_data
where map_data_code in (?, ?, ...)
```

证据：`PointQuery.buildSelectSql` `src/com/local/monitor/PointQuery.java:19-23`；占位符数量按点位数生成，证据：`src/com/local/monitor/PointQuery.java:12-17`；`PointRepository.fetch` 逐个绑定 `map_data_code`，证据：`src/com/local/monitor/PointRepository.java:23-34`。

## 4.5 所有可能写入数据库的路径

* 生产 PostgreSQL 查询路径未发现 INSERT/UPDATE/DELETE/DDL。证据：本次 `rg` 搜索 SQL 字符串，生产远程仓储只命中 `PointRepository` 和 `DbMetadataRepository` 的 SELECT/metadata；`MonitorLogicTest` 断言 `PointQuery` 不含 `update`、`delete`。证据：`test/com/local/monitor/MonitorLogicTest.java:91-101`。
* 写数据库的代码集中在本地 H2 测试库 `LocalTestDatabase`：`delete from public.tcs_map_data`、`create table if not exists`、`insert into public.tcs_map_data`、`update public.tcs_map_data set pod_code=?...`。证据：`src/com/local/monitor/LocalTestDatabase.java:17`、`:68`、`:90`、`:106`。
* 本地 H2 写入由 `LocalTestDbTool.main` 和构建脚本触发。证据：`src/com/local/monitor/LocalTestDbTool.java:7-20`；`build.ps1:127-160`、`build.ps1:253-254`。
* 未确认项：无法仅凭代码证明现场数据库账号一定只读；数据库权限必须由 PostgreSQL 角色权限强制保障。证据：代码只执行 `setReadOnly`、`SET TRANSACTION READ ONLY`，证据 `src/com/local/monitor/ReadOnlyConnectionFactory.java:21-26`；README 也要求生产环境建议只读账号，证据 `README.md:40`。

## 4.6 输入校验与参数化

* schema/table 标识符校验：只允许 `[A-Za-z_][A-Za-z0-9_]*`。证据：`PointQuery.validateIdentifier` `src/com/local/monitor/PointQuery.java:26-34`。
* `DbConfig` 构造时校验 schema。证据：`DbConfig` 构造 `src/com/local/monitor/DbConfig.java:26`。
* `DbMetadataRepository` 对 schema/table 分别校验。证据：`src/com/local/monitor/DbMetadataRepository.java:36`、`:61-62`、`:89-90`。
* 点位编码使用 `PreparedStatement` 参数绑定，不拼接进 SQL。证据：`PointRepository.fetch` `src/com/local/monitor/PointRepository.java:29-34`。
* 代码层只读与账户权限区别：当前代码属于“尽量只读”加防误操作；真正“强制只读”只能由数据库账户撤销写权限实现。证据：代码层只读设置在 `ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:21-26`；README 说明“生产环境仍建议使用数据库只读账号运行” `README.md:40`。

# 5. 点位缺料报警业务模型

## 5.1 对象关系

* 点位组：`PointGroupDefinition`，包含 `id`、区域、组名、物料名、启用状态、检测周期秒、点位列表、报警规则。证据：`src/com/local/monitor/PointGroupDefinition.java:10`、`:52-84`。
* 使用位/备用位：`GroupMonitorPoint` + `PointRole`，每个点位有编码、别名、角色、启用状态、排序。证据：`src/com/local/monitor/GroupMonitorPoint.java:3`、`:23-43`；`src/com/local/monitor/PointRole.java:3`。
* 报警规则：`GroupAlertRule`，包含 `enabled`、`requireUsePointEmpty`、`minBackupAvailable`、`durationMinutes`、`backupThresholdParticipates`。证据：`src/com/local/monitor/GroupAlertRule.java:3`、`:36-56`。
* 运行时状态：`GroupRuntimeState`，保存上次检测时间、条件首次/最后成立时间、是否确认、弹窗是否显示。证据：`src/com/local/monitor/GroupRuntimeState.java:6-10`、`:13-85`。
* 检测结果：`GroupEvaluation`，保存状态、使用位是否无料、备用位总数/有料数/未查到数、连续成立秒数、点位状态列表、是否应弹窗。证据：`src/com/local/monitor/GroupEvaluation.java:5`、`:87-150`。
* 确认：用户在弹窗点击“已关注”后调用 `acknowledgeGroupAlert`，把对应组的 `GroupRuntimeState` 标记为确认。证据：`ShelfPointMonitorApp.acknowledgeGroupAlert` `src/com/local/monitor/ShelfPointMonitorApp.java:1535-1549`。
* 恢复：条件不成立时 `GroupMonitorLogic.evaluate` 调用 `state.reset()`。证据：`src/com/local/monitor/GroupMonitorLogic.java:63`；`GroupRuntimeState.reset` `src/com/local/monitor/GroupRuntimeState.java:80-85`。

## 5.2 定时检测到弹出报警/写日志调用链

1. 用户点击开始监控，进入 `ShelfPointMonitorApp.startMonitoring()`。证据：`src/com/local/monitor/ShelfPointMonitorApp.java:1382`。
2. 调度器用 `ScheduledExecutorService.scheduleWithFixedDelay` 每 10 秒触发一次扫描。证据：`src/com/local/monitor/ShelfPointMonitorApp.java:1390`。
3. `checkDueGroups()` 根据 `GroupCheckPlanner.dueGroups` 筛选到期组。证据：`ShelfPointMonitorApp.checkDueGroups` `src/com/local/monitor/ShelfPointMonitorApp.java:1140`；`GroupCheckPlanner.dueGroups` `src/com/local/monitor/GroupCheckPlanner.java:13`。
4. `checkGroups()` 收集每组启用点位，调用 `PointRepository.fetch` 查询数据库。证据：`ShelfPointMonitorApp.checkGroups` `src/com/local/monitor/ShelfPointMonitorApp.java:1154-1172`；`PointRepository.fetch` `src/com/local/monitor/PointRepository.java:23`。
5. 查询结果进入 `GroupMonitorLogic.evaluate`，计算状态机和点位状态。证据：`src/com/local/monitor/GroupMonitorLogic.java:10-17`。
6. UI 刷新状态看板。证据：`ShelfPointMonitorApp.updateSelectedGroupBoard` `src/com/local/monitor/ShelfPointMonitorApp.java:1221`；`renderPointStatusBoard` `src/com/local/monitor/ShelfPointMonitorApp.java:1280`。
7. 写检测快照日志。证据：`ShelfPointMonitorApp.appendCheckLog` `src/com/local/monitor/ShelfPointMonitorApp.java:1253`；`GroupLogWriter.appendCheck` `src/com/local/monitor/GroupLogWriter.java:43`。
8. 写报警/恢复事件。证据：`ShelfPointMonitorApp.appendGroupEvents` `src/com/local/monitor/ShelfPointMonitorApp.java:1261-1277`；`GroupLogWriter.appendEvent` `src/com/local/monitor/GroupLogWriter.java:60`。
9. 当 `GroupEvaluation.shouldShowDialog()` 为真时弹出报警。证据：`GroupEvaluation.shouldShowDialog` `src/com/local/monitor/GroupEvaluation.java:146`；`ShelfPointMonitorApp.showGroupAlertDialog` `src/com/local/monitor/ShelfPointMonitorApp.java:1421`。
10. 用户点击“已关注”后调用确认并写事件日志。证据：`ShelfPointMonitorApp.acknowledgeGroupAlert` `src/com/local/monitor/ShelfPointMonitorApp.java:1535-1549`。

## 5.3 报警状态机

* `NORMAL`：规则未启用或条件不成立，重置运行状态。证据：`GroupAlertStatus` `src/com/local/monitor/GroupAlertStatus.java:3-7`；`GroupMonitorLogic.evaluate` `src/com/local/monitor/GroupMonitorLogic.java:57-64`。
* `PENDING_ALERT`：规则条件成立，但持续时间未达到规则要求。证据：`GroupMonitorLogic.evaluate` 调用 `state.markMatched(now)` 后计算持续秒数，`src/com/local/monitor/GroupMonitorLogic.java:67-74`。
* `ACTIVE_ALERT`：规则条件成立且持续时间达到要求，且尚未确认。证据：`GroupMonitorLogic.evaluate` `src/com/local/monitor/GroupMonitorLogic.java:71-79`。
* `ACKED_ALERT`：报警已被人工确认，条件仍成立时继续压制重复弹窗。证据：`GroupRuntimeState.acknowledge` `src/com/local/monitor/GroupRuntimeState.java:37-39`；`GroupMonitorLogicTest` `test/com/local/monitor/GroupMonitorLogicTest.java:114-154`。
* 恢复转换：条件不成立时 `reset()` 清除首次成立、确认和弹窗标志。证据：`GroupRuntimeState.reset` `src/com/local/monitor/GroupRuntimeState.java:80-85`。

## 5.4 点位是否有货架的判定依据

* 点位记录来自 `public.tcs_map_data` 的 `map_data_code`、`pod_code`、`pod_status`、`status`、`ind_lock`、`date_chg` 等字段。证据：`PointQuery.buildSelectSql` `src/com/local/monitor/PointQuery.java:19-23`；公开映射说明 `README.md:74-82`。
* 组规则内部“有料”判定：记录存在、`podCode` 非空、`status == 1`、`indLock == 0`。证据：`GroupMonitorLogic.isAvailable` `src/com/local/monitor/GroupMonitorLogic.java:110-114`。
* UI 状态映射：停用点位返回 `DISABLED`；记录不存在返回 `MISSING`；`pod_code` 空返回 `EMPTY`；`status != 1` 返回 `EMPTY`；`ind_lock != 0` 返回 `EMPTY`；否则 `AVAILABLE`。证据：`PointStatusMapper.map` `src/com/local/monitor/PointStatusMapper.java:22-60`。
* 显示文本：`AVAILABLE=有料`、`EMPTY=无料`、`MISSING=未查到`、`DISABLED=停用`。证据：`PointMaterialStatus` `src/com/local/monitor/PointMaterialStatus.java:3-15`。

## 5.5 边界条件

* 误报风险：`status != 1` 或 `ind_lock != 0` 即使 `pod_code` 存在也显示无料；如果现场对 `status/ind_lock` 的语义不同，会误报。证据：`PointStatusMapper.map` `src/com/local/monitor/PointStatusMapper.java:38-48`；`GroupMonitorLogic.isAvailable` `src/com/local/monitor/GroupMonitorLogic.java:110-114`。
* 漏报风险：如果查询失败，当前批次没有 `GroupEvaluation`，无法推进持续计时，也不会弹窗；需要依赖运行日志发现异常。证据：`ShelfPointMonitorApp.checkGroups` 捕获异常并记录 failedGroups，`src/com/local/monitor/ShelfPointMonitorApp.java:1180-1191`；UI 测试确认失败组不更新 last status，`test/com/local/monitor/ShelfPointMonitorAppUiTest.java:394`。
* 重复报警风险：状态达到 `ACTIVE_ALERT` 后依赖 `dialogShown` 和 `acknowledge` 压制；如果应用重启，运行时确认状态会丢失。证据：`GroupRuntimeState` 仅为内存类 `src/com/local/monitor/GroupRuntimeState.java:6-10`；`GroupMonitorLogic.evaluate` 使用 `state.isAcknowledged()`，`src/com/local/monitor/GroupMonitorLogic.java:71-79`。
* 报警无法恢复风险：如果数据库长期返回旧记录且 `status/pod_code/ind_lock` 不变化，恢复条件不会成立；代码没有用 `date_chg` 超时判断数据新鲜度。证据：`PointQuery` 读取 `date_chg`，`src/com/local/monitor/PointQuery.java:21-22`；`GroupMonitorLogic.isAvailable` 未使用 `dateChg`，证据 `src/com/local/monitor/GroupMonitorLogic.java:110-114`。
* 点位组 ID 重复风险：未在模型层看到唯一性强校验；运行时状态 Map 以 groupId 为 key，重复 ID 可能相互覆盖。证据：`ShelfPointMonitorApp.checkGroups` 使用 `groupRuntimeStates.computeIfAbsent(group.id(), ...)`，`src/com/local/monitor/ShelfPointMonitorApp.java:1174`。

# 6. 本地数据与敏感信息

## 6.1 本地文件读写位置和用途

* `data/config.properties`：旧点位监控配置。证据：`ConfigStore.load/save` `src/com/local/monitor/ConfigStore.java:22-57`；`ShelfPointMonitorApp.loadPointConfig/savePointConfig` `src/com/local/monitor/ShelfPointMonitorApp.java:1561`、`:1612`。
* `data/connections.properties`：连接配置，不保存密码。证据：`ConnectionProfileStore.load/save` `src/com/local/monitor/ConnectionProfileStore.java:23-90`；README `README.md:70`。
* `data/group-config.properties`：点位组配置。证据：`GroupConfigStore.load/save` `src/com/local/monitor/GroupConfigStore.java:22-59`。
* `logs/check-log.csv`：每次检测快照。证据：`GroupLogWriter.appendCheck` `src/com/local/monitor/GroupLogWriter.java:43-58`。
* `logs/event-log.csv`：报警、确认、恢复事件。证据：`GroupLogWriter.appendEvent` `src/com/local/monitor/GroupLogWriter.java:60-70`。
* `logs/monitor.log`：运行状态文本日志。证据：`ShelfPointMonitorApp.writeLog` `src/com/local/monitor/ShelfPointMonitorApp.java:1813-1818`。
* `data/local-test-db.mv.db`：H2 本地测试数据库文件。证据：`DbConfig.localTest` `src/com/local/monitor/DbConfig.java:36`；`LocalTestDatabase.reset` `src/com/local/monitor/LocalTestDatabase.java:12-18`。
* 发布包样例配置由 `build.ps1` 写入 `dist/ShelfPointMonitor/data`。证据：`build.ps1:165-247`。

## 6.2 密码、IP、用户名、点位编码、日志保存规则

* 密码：不保存到配置文件；只在 UI 本次运行内存 `char[]` 和 JDBC `String` 转换过程中存在。证据：`ShelfPointMonitorApp.currentPassword` `src/com/local/monitor/ShelfPointMonitorApp.java:160`；`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:19`；`ConnectionProfileStoreTest` `test/com/local/monitor/ConnectionProfileStoreTest.java:31`。
* IP/用户名/数据库名/schema：连接配置会保存到 `data/connections.properties`。证据：`ConnectionProfileStore.save` `src/com/local/monitor/ConnectionProfileStore.java:64-90`；README `README.md:57-70`。
* 点位编码：点位组配置会保存到 `data/group-config.properties`；检测日志也会写入点位组状态摘要。证据：`GroupConfigStore.save` `src/com/local/monitor/GroupConfigStore.java:46-59`；`GroupLogWriter.appendCheck/appendEvent` `src/com/local/monitor/GroupLogWriter.java:43-70`。
* 日志：本地 `logs/` 目录，包含检测和报警事件。证据：`.gitignore:8-10`；`GroupLogWriter.writeCsv` `src/com/local/monitor/GroupLogWriter.java:73-83`。
* 自检包含敏感属性检查：禁止配置文件中出现 password、私网 IP、真实点位编码格式。证据：`ShelfPointMonitorApp.assertNoPasswordProperty/assertNoSensitiveProperties` `src/com/local/monitor/ShelfPointMonitorApp.java:310-318`；正则字段 `PRIVATE_10_NET_PATTERN`、`REAL_POINT_CODE_PATTERN` `src/com/local/monitor/ShelfPointMonitorApp.java:65-66`。

## 6.3 `.gitignore` 评估

* 已忽略构建产物、发布产物、依赖 jar、本地工作树。证据：`.gitignore:1-5`、`.gitignore:30-32`。
* 已忽略运行数据和日志。证据：`.gitignore:7-12`。
* 已忽略常见配置、secret、env。证据：`.gitignore:14-21`。
* 未忽略 `.obsidian/`，当前工作区已有未跟踪 `.obsidian/` 文件。证据：`.gitignore:23-28` 未包含 `.obsidian/`；本次 `git status --short --branch` 输出 `?? .obsidian/`。

## 6.4 泄露风险排序

1. P1：`.obsidian/` 未忽略，可能把本地工作区路径、最近文件、插件状态提交到仓库。证据：当前未跟踪 `.obsidian/`；`.gitignore:23-28` 未覆盖。
2. P1：`data/connections.properties` 保存 IP、用户名、数据库名、schema；虽然根目录 `/data/` 已忽略，但如果复制到其他路径或文档中仍可能泄露。证据：`ConnectionProfileStore.save` `src/com/local/monitor/ConnectionProfileStore.java:64-90`；`.gitignore:9`。
3. P1：`logs/check-log.csv` 与 `logs/event-log.csv` 可能包含真实点位组、物料、点位编码和缺料事件。证据：`GroupLogWriter.appendCheck/appendEvent` `src/com/local/monitor/GroupLogWriter.java:43-70`；`.gitignore:8`。
4. P2：报告、说明书或 release 文档如果手工粘贴现场 IP/点位编码，当前 `.gitignore` 无法阻止 Markdown 泄露。证据：README 只写“禁止提交真实 IP...”，`README.md:88`，这属于流程约束而非技术拦截。

# 7. 测试覆盖与缺口

| 测试类 | 覆盖的生产类 | 已覆盖场景证据 |
|---|---|---|
| `ConfigStoreTest` | `ConfigStore`、`DbConfig`、`PointDefinition` | 新旧点位配置加载保存。证据：`test/com/local/monitor/ConfigStoreTest.java:7-33`。 |
| `ConnectionProfileStoreTest` | `ConnectionProfileStore`、`ConnectionProfile` | 连接配置保存加载、默认配置、不保存密码。证据：`test/com/local/monitor/ConnectionProfileStoreTest.java:7-36`、`:31`。 |
| `DbMetadataRepositoryTest` | `DbMetadataRepository`、`LocalTestDatabase` | schema/table/column/preview 和非法标识符。证据：`test/com/local/monitor/DbMetadataRepositoryTest.java:7-37`。 |
| `GroupCheckPlannerTest` | `GroupCheckPlanner`、`GroupRuntimeState` | 按组检测周期筛选。证据：`test/com/local/monitor/GroupCheckPlannerTest.java:10-113`。 |
| `GroupConfigStoreTest` | `GroupConfigStore`、`PointGroupDefinition`、`GroupMonitorPoint` | 默认配置、保存加载、旧格式迁移、不保存密码。证据：`test/com/local/monitor/GroupConfigStoreTest.java:7-126`。 |
| `GroupLogWriterTest` | `GroupLogWriter` | 检测日志和事件日志写入 CSV。证据：`test/com/local/monitor/GroupLogWriterTest.java:8-70`。 |
| `GroupMonitorLogicTest` | `GroupMonitorLogic`、`GroupRuntimeState`、`GroupAlertRule` | 正常、观察中、报警、确认、恢复、文案不泄露枚举。证据：`test/com/local/monitor/GroupMonitorLogicTest.java:6-294`。 |
| `LocalTestDatabaseTest` | `LocalTestDatabase`、`PointRepository`、`PointRecord` | H2 测试库初始化和字段映射。证据：`test/com/local/monitor/LocalTestDatabaseTest.java:9-77`。 |
| `MonitorLogicTest` | `MonitorLogic`、`AlertState`、`PointQuery` | 旧单点报警确认、SQL 只读和注入拒绝。证据：`test/com/local/monitor/MonitorLogicTest.java:47-101`。 |
| `PointScheduleTest` | `PointSchedule` | 旧单点定时到期和强制全量。证据：`test/com/local/monitor/PointScheduleTest.java:6-49`。 |
| `PointStatusMapperTest` | `PointStatusMapper`、`PointMaterialStatus` | 有料、无料、未查到、停用状态映射。证据：`test/com/local/monitor/PointStatusMapperTest.java:6-86`。 |
| `ShelfPointMonitorAppUiTest` | `ShelfPointMonitorApp`、UI 文案、调度交互、弹窗确认 | 中文 UI、调度器替身、报警弹窗、确认按钮、日志目录。证据：`test/com/local/monitor/ShelfPointMonitorAppUiTest.java:37-1175`。 |
| `ShelfPointMonitorSelfTestTest` | `ShelfPointMonitorApp` 自检、发布包配置 | 发布包文件完整性、无敏感属性、本地测试库存在。证据：`test/com/local/monitor/ShelfPointMonitorSelfTestTest.java:7-107`。 |

已覆盖的关键规则：

* 点位状态映射。证据：`PointStatusMapperTest` `test/com/local/monitor/PointStatusMapperTest.java:57-58`。
* 点位 SQL 只读和 schema 注入拦截。证据：`MonitorLogicTest` `test/com/local/monitor/MonitorLogicTest.java:91-101`。
* 连接/组配置不保存密码。证据：`ConnectionProfileStoreTest.java:31`；`GroupConfigStoreTest.java:38`。
* 组报警确认后不重复弹窗，恢复后清确认。证据：`GroupMonitorLogicTest` `test/com/local/monitor/GroupMonitorLogicTest.java:114-154`。

未覆盖或覆盖不足的高风险场景：

* 未确认真实 PostgreSQL 权限只读；没有用 PostgreSQL 实例断言 `INSERT/UPDATE/DELETE` 会被拒绝。证据：只看到 H2/逻辑测试，构建测试列表 `build.ps1:63-88`。
* 数据库断连、连接超时、statement timeout 的端到端 UI 行为未见专门测试。证据：`ReadOnlyConnectionFactory.open` 设置超时 `src/com/local/monitor/ReadOnlyConnectionFactory.java:25-26`；测试类没有网络失败集成项。
* 配置文件损坏时 `ConfigStore.load`、`GroupConfigStore.load` 多处吞异常回默认，缺少用户可见错误测试。证据：`ConfigStore.load` 捕获异常 `src/com/local/monitor/ConfigStore.java:25-30`；`GroupConfigStore.load` 捕获异常 `src/com/local/monitor/GroupConfigStore.java:27-31`。
* 并发和重复调度仅由 UI 替身调度器有限覆盖，缺少长时间运行或真实线程竞争测试。证据：实际调度在 `ShelfPointMonitorApp.startMonitoring` `src/com/local/monitor/ShelfPointMonitorApp.java:1382-1394`；UI 测试使用 `CapturingScheduledExecutor` `test/com/local/monitor/ShelfPointMonitorAppUiTest.java:1083-1169`。
* 日志写入失败、磁盘满、CSV 被占用未见测试。证据：`GroupLogWriter.writeCsv` `src/com/local/monitor/GroupLogWriter.java:73-83`；测试只覆盖正常写入 `test/com/local/monitor/GroupLogWriterTest.java:8-41`。

# 8. 架构与代码质量问题

## P0

未确认 P0。基于本次只读扫描和构建，未发现会直接写现场数据库或导致应用无法构建运行的确定性问题。证据：构建成功；远程业务 SQL 仅发现 SELECT/metadata，证据 `src/com/local/monitor/PointQuery.java:19-23`、`src/com/local/monitor/DbMetadataRepository.java:94`、`test/com/local/monitor/MonitorLogicTest.java:91-101`。

## P1

### P1-1 数据库只读边界依赖代码约束，未被数据库权限强制证明

* 问题描述：代码设置只读连接和只读事务，但没有证据证明现场账号权限已撤销写入。
* 影响：如果未来误加写 SQL，或数据库/驱动对只读设置不严格，现场数据仍可能有风险。
* 证据：`ReadOnlyConnectionFactory.open` `src/com/local/monitor/ReadOnlyConnectionFactory.java:21-26`；README 建议使用只读账号 `README.md:40`。
* 最小修复方向：增加启动时可选权限自检或运维文档强制只读账号验收；禁止在生产连接执行破坏性测试。
* 是否需要改动数据结构或用户界面：不需要数据结构；可能需要连接页增加“已验证只读账号”状态。

### P1-2 密码在 JDBC 连接前被转换为 String

* 问题描述：UI 用 `char[]`，但 `ReadOnlyConnectionFactory` 中 `new String(password)` 会生成无法主动清零的字符串。
* 影响：降低“密码只在内存短暂存在”的强度。
* 证据：`src/com/local/monitor/ReadOnlyConnectionFactory.java:19`；`ShelfPointMonitorApp.currentPassword` `src/com/local/monitor/ShelfPointMonitorApp.java:160`。
* 最小修复方向：评估 JDBC driver 是否可接受 char[]；若不能，缩短 password 字符串生命周期并在文档中如实说明。
* 是否需要改动数据结构或用户界面：不需要。

### P1-3 查询失败不会推进报警计时，可能漏掉“数据库异常导致无法判断”的现场风险

* 问题描述：某组查询失败时没有生成 `GroupEvaluation`，持续缺料计时不会变化。
* 影响：真实缺料同时发生数据库短断时，报警可能延迟；运维只能看运行日志。
* 证据：`ShelfPointMonitorApp.checkGroups` 捕获异常 `src/com/local/monitor/ShelfPointMonitorApp.java:1180-1191`；UI 测试断言 failed group 不更新 last status `test/com/local/monitor/ShelfPointMonitorAppUiTest.java:394`。
* 最小修复方向：把“查询失败”作为独立状态和日志事件，但不能等同于无料。
* 是否需要改动数据结构或用户界面：需要 UI 增加查询异常状态；不一定改数据库结构。

### P1-4 点位组 ID 缺少唯一性强约束

* 问题描述：运行时状态以 `group.id()` 为 key，重复 ID 会共享或覆盖状态。
* 影响：不同组可能互相影响报警计时、确认和恢复。
* 证据：`ShelfPointMonitorApp.checkGroups` `groupRuntimeStates.computeIfAbsent(group.id(), ...)`，`src/com/local/monitor/ShelfPointMonitorApp.java:1174`；`GroupConfigStore.load` 未见全局去重校验，证据 `src/com/local/monitor/GroupConfigStore.java:22-59`。
* 最小修复方向：保存组配置前校验 ID 唯一。
* 是否需要改动数据结构或用户界面：不需要数据结构；需要 UI 保存校验提示。

### P1-5 `.obsidian/` 未被忽略

* 问题描述：本地 `.obsidian/` 当前未跟踪，但 `.gitignore` 未忽略。
* 影响：容易提交本地编辑器状态或路径信息。
* 证据：`git status --short --branch` 输出 `?? .obsidian/`；`.gitignore:23-28` 未包含 `.obsidian/`。
* 最小修复方向：把 `.obsidian/` 加入忽略或转移到个人全局 gitignore。
* 是否需要改动数据结构或用户界面：不需要。

## P2

### P2-1 `ShelfPointMonitorApp` 承担过多职责

* 问题描述：主类同时处理 UI、连接、配置、调度、日志、弹窗、自检。
* 影响：后续修改容易互相影响，测试需要大量反射或 UI 替身。
* 证据：`ShelfPointMonitorApp` 类 `src/com/local/monitor/ShelfPointMonitorApp.java:62`；方法跨度包括 `buildUi` `:358`、`testSelectedProfile` `:797`、`checkGroups` `:1154`、`showGroupAlertDialog` `:1421`、`writeLog` `:1813`。
* 最小修复方向：后续按连接管理、浏览器、组监控、日志服务逐步提取。
* 是否需要改动数据结构或用户界面：不必先改 UI；属于内部结构调整。

### P2-2 旧单点监控链路仍保留

* 问题描述：v0.4.0 主需求是点位组状态看板，但代码仍保留旧单点配置和旧报警方法。
* 影响：维护成本增加，未来改动可能漏改旧路径或误触发。
* 证据：`ShelfPointMonitorApp.checkNowLegacy/startMonitoringLegacy/showAlertDialog` `src/com/local/monitor/ShelfPointMonitorApp.java:1621`、`:1694`、`:1732`；旧模型 `MonitorLogic` `src/com/local/monitor/MonitorLogic.java:12`。
* 最小修复方向：确认兼容需求后再决定删除、隐藏或隔离旧路径。
* 是否需要改动数据结构或用户界面：可能需要 UI 兼容决策。

### P2-3 配置损坏时回退默认值，用户可能不知道配置已损坏

* 问题描述：配置读取异常被捕获后直接返回默认配置。
* 影响：用户可能误以为配置仍存在，导致监控对象变化。
* 证据：`ConfigStore.load` `src/com/local/monitor/ConfigStore.java:25-30`；`GroupConfigStore.load` `src/com/local/monitor/GroupConfigStore.java:27-31`。
* 最小修复方向：读取失败时保留默认回退，但 UI 显示“配置读取失败，已使用默认配置”。
* 是否需要改动数据结构或用户界面：需要 UI 提示；不需要数据结构。

# 9. 建议的后续改造顺序

1. 目标：补强只读账号验收。涉及文件：`README.md`、`docs/manuals/point-shortage-alert-user-manual.md`、可选 `ShelfPointMonitorApp.java`。验证方法：使用现场只读账号执行浏览和监控；用单独受控账号证明写权限被数据库拒绝。回滚方式：移除新增验收提示或文档段落。
2. 目标：忽略 `.obsidian/` 防泄露。涉及文件：`.gitignore`。验证方法：`git status --short --branch` 不再显示 `.obsidian/`。回滚方式：删除该忽略规则。
3. 目标：连接配置保存前做敏感字段自检。涉及文件：`ConnectionProfileStore.java`、`ShelfPointMonitorApp.java`。验证方法：保存含 10.x IP 的测试配置后确认只落本地且不会进入发布包自检。回滚方式：恢复保存逻辑。
4. 目标：点位组 ID 唯一校验。涉及文件：`GroupConfigStore.java`、`ShelfPointMonitorApp.java`、`GroupConfigStoreTest.java`。验证方法：重复 ID 保存失败并显示中文提示。回滚方式：移除保存前校验。
5. 目标：查询失败状态独立展示。涉及文件：`ShelfPointMonitorApp.java`、`GroupEvaluation.java` 或新增错误 DTO、UI 测试。验证方法：模拟 `PointRepository.fetch` 抛异常，界面显示“查询失败”且不误报无料。回滚方式：恢复异常只写运行日志。
6. 目标：配置损坏时显式告警。涉及文件：`ConfigStore.java`、`GroupConfigStore.java`、`ShelfPointMonitorApp.java`。验证方法：写入非法 properties 后启动，UI 提示并使用默认配置。回滚方式：恢复吞异常回默认。
7. 目标：为 `ReadOnlyConnectionFactory` 增加专门测试。涉及文件：`ReadOnlyConnectionFactory.java`、新增或扩展测试。验证方法：H2 或测试数据库验证 readonly/autocommit/timeout 设置。回滚方式：删除测试，不影响生产代码。
8. 目标：把日志服务从主 UI 中提取。涉及文件：`ShelfPointMonitorApp.java`、`GroupLogWriter.java`。验证方法：现有日志测试和 UI 测试全通过。回滚方式：恢复内联调用。
9. 目标：清理或隔离旧单点监控路径。涉及文件：`MonitorLogic.java`、`AlertState.java`、`PointSchedule.java`、`ShelfPointMonitorApp.java`。验证方法：确认 v0.4.0 组监控测试不受影响；旧功能若保留则补充入口说明。回滚方式：保留旧路径不删除。
10. 目标：增加长时间监控和断线恢复测试。涉及文件：`ShelfPointMonitorAppUiTest.java`、可能新增测试替身。验证方法：模拟多轮调度、断线、恢复、确认后再缺料。回滚方式：删除新增测试。

# 10. 给架构审查者的原始证据

## 10.1 所有 Java 文件清单

```text
src/com/local/monitor/AlertState.java
src/com/local/monitor/ColumnInfo.java
src/com/local/monitor/ConfigStore.java
src/com/local/monitor/ConnectionProfile.java
src/com/local/monitor/ConnectionProfileStore.java
src/com/local/monitor/DbConfig.java
src/com/local/monitor/DbMetadataRepository.java
src/com/local/monitor/GroupAlertRule.java
src/com/local/monitor/GroupAlertStatus.java
src/com/local/monitor/GroupCheckPlanner.java
src/com/local/monitor/GroupConfigStore.java
src/com/local/monitor/GroupEvaluation.java
src/com/local/monitor/GroupLogWriter.java
src/com/local/monitor/GroupMonitorLogic.java
src/com/local/monitor/GroupMonitorPoint.java
src/com/local/monitor/GroupRuntimeState.java
src/com/local/monitor/GroupStatusText.java
src/com/local/monitor/LocalTestDatabase.java
src/com/local/monitor/LocalTestDbTool.java
src/com/local/monitor/MonitorEvaluation.java
src/com/local/monitor/MonitorLogic.java
src/com/local/monitor/PointAlert.java
src/com/local/monitor/PointDefinition.java
src/com/local/monitor/PointGroupDefinition.java
src/com/local/monitor/PointMaterialStatus.java
src/com/local/monitor/PointQuery.java
src/com/local/monitor/PointRecord.java
src/com/local/monitor/PointRepository.java
src/com/local/monitor/PointRole.java
src/com/local/monitor/PointSchedule.java
src/com/local/monitor/PointStatusMapper.java
src/com/local/monitor/PointStatusView.java
src/com/local/monitor/ReadOnlyConnectionFactory.java
src/com/local/monitor/SchemaInfo.java
src/com/local/monitor/ShelfPointMonitorApp.java
src/com/local/monitor/TableInfo.java
src/com/local/monitor/TablePreview.java
```

## 10.2 所有测试文件清单

```text
test/com/local/monitor/ConfigStoreTest.java
test/com/local/monitor/ConnectionProfileStoreTest.java
test/com/local/monitor/DbMetadataRepositoryTest.java
test/com/local/monitor/GroupCheckPlannerTest.java
test/com/local/monitor/GroupConfigStoreTest.java
test/com/local/monitor/GroupLogWriterTest.java
test/com/local/monitor/GroupMonitorLogicTest.java
test/com/local/monitor/LocalTestDatabaseTest.java
test/com/local/monitor/MonitorLogicTest.java
test/com/local/monitor/PointScheduleTest.java
test/com/local/monitor/PointStatusMapperTest.java
test/com/local/monitor/ShelfPointMonitorAppUiTest.java
test/com/local/monitor/ShelfPointMonitorSelfTestTest.java
```

## 10.3 所有 SQL 字符串所在文件、方法

| 文件 | 类/方法 | SQL 字符串 | 行号 |
|---|---|---|---|
| `src/com/local/monitor/DbMetadataRepository.java` | `DbMetadataRepository.previewTable` | `select * from schema.table limit N` | `94` |
| `src/com/local/monitor/LocalTestDatabase.java` | `LocalTestDatabase.reset` | `delete from public.tcs_map_data` | `17` |
| `src/com/local/monitor/LocalTestDatabase.java` | `LocalTestDatabase.createIfMissing` | `select count(*) from public.tcs_map_data` | `56` |
| `src/com/local/monitor/LocalTestDatabase.java` | `LocalTestDatabase.createIfMissing` | `create table if not exists public.tcs_map_data (...)` | `68` |
| `src/com/local/monitor/LocalTestDatabase.java` | `LocalTestDatabase.insert` | `insert into public.tcs_map_data (...) values (...)` | `90` |
| `src/com/local/monitor/LocalTestDatabase.java` | `LocalTestDatabase.setPod` | `update public.tcs_map_data set pod_code=?, date_chg=current_timestamp where map_data_code=?` | `106` |
| `src/com/local/monitor/PointQuery.java` | `PointQuery.buildSelectSql` | `select map_data_code ... from <schema>.tcs_map_data where map_data_code in (...)` | `19-23` |
| `src/com/local/monitor/PointRepository.java` | `PointRepository.testConnection` | `select current_database(), current_user` | `15` |
| `src/com/local/monitor/ReadOnlyConnectionFactory.java` | `ReadOnlyConnectionFactory.open` | `SET TRANSACTION READ ONLY` | `25` |
| `src/com/local/monitor/ReadOnlyConnectionFactory.java` | `ReadOnlyConnectionFactory.open` | `SET statement_timeout='8s'` | `26` |

## 10.4 `ScheduledExecutorService`、文件写入、密码转换、数据库连接创建所在文件和方法

### ScheduledExecutorService

| 文件 | 类/方法 | 证据 |
|---|---|---|
| `src/com/local/monitor/ShelfPointMonitorApp.java` | 字段 `executor` | `ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()`，行 `80` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `startMonitoring` | `scheduleWithFixedDelay(..., 0, 10, TimeUnit.SECONDS)`，行 `1390` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `stopMonitoring` | `scheduledTask.cancel(false)`，行 `1405` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `startMonitoringLegacy` | 旧路径 `scheduleWithFixedDelay`，行 `1701` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `stopMonitoringLegacy` | 旧路径取消，行 `1717` |
| `test/com/local/monitor/ShelfPointMonitorAppUiTest.java` | `CapturingScheduledExecutor` | 测试替身实现，行 `1083-1169` |

### 文件写入

| 文件 | 类/方法 | 证据 |
|---|---|---|
| `src/com/local/monitor/ConfigStore.java` | `ConfigStore.save` | `Files.createDirectories`、`Files.newOutputStream`、`Properties.store`，行 `43-57` |
| `src/com/local/monitor/ConnectionProfileStore.java` | `ConnectionProfileStore.save` | `Files.createDirectories`、`Files.newOutputStream`、`Properties.store`，行 `64-90` |
| `src/com/local/monitor/GroupConfigStore.java` | `GroupConfigStore.save` | `Files.createDirectories`、`Files.newOutputStream`、`Properties.store`，行 `46-59` |
| `src/com/local/monitor/GroupLogWriter.java` | `GroupLogWriter.writeCsv` | `Files.createDirectories`、`Files.newBufferedWriter`，行 `73-83` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `writeLog` | `Files.createDirectories`、`Files.writeString`，行 `1813-1818` |
| `src/com/local/monitor/LocalTestDatabase.java` | `reset/createIfMissing/insert/setPod` | 写 H2 测试库，行 `12-18`、`68`、`90`、`106` |

### 密码转换

| 文件 | 类/方法 | 证据 |
|---|---|---|
| `src/com/local/monitor/ShelfPointMonitorApp.java` | 字段 `profilePasswordField` | `JPasswordField`，行 `101` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | 字段 `currentPassword` | `char[] currentPassword`，行 `160` |
| `src/com/local/monitor/ShelfPointMonitorApp.java` | `testSelectedProfile` | `profilePasswordField.getPassword()` 并 `clone` 到 `currentPassword`，行 `800-805` |
| `src/com/local/monitor/ReadOnlyConnectionFactory.java` | `open` | `new String(password)` 写入 JDBC `Properties`，行 `19` |

### 数据库连接创建

| 文件 | 类/方法 | 证据 |
|---|---|---|
| `src/com/local/monitor/ReadOnlyConnectionFactory.java` | `open` | `DriverManager.getConnection(config.jdbcUrl(), props)`，行 `20` |
| `src/com/local/monitor/LocalTestDatabase.java` | `reset` | `DriverManager.getConnection(config.jdbcUrl(), "sa", "")`，行 `14` |
| `src/com/local/monitor/LocalTestDatabase.java` | `setScenario` | `DriverManager.getConnection(config.jdbcUrl(), "sa", "")`，行 `26` |
| `src/com/local/monitor/LocalTestDatabase.java` | `createIfMissing` | `DriverManager.getConnection(config.jdbcUrl(), "sa", "")`，行 `53` |

## 10.5 本次构建命令与最终输出

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

构建结果：通过，退出码 `0`。
