# 只读业务数据库工具第一阶段设计

## 目标

把现有“点位缺料报警 MVP”升级为一个轻量只读业务数据库工具。第一阶段只包含连接管理、数据库浏览器、点位缺料报警三个模块，不实现通用 SQL 编辑器，不提供任何数据库写入入口。

## 范围

包含：

- 多连接配置管理：新增、保存、删除、测试连接。
- 连接配置保存 host、port、database、schema、user、sslmode、连接名、连接类型。
- 密码不保存，只在本次运行中通过密码框传入 JDBC。
- 只读数据库浏览器：查看 schema、table/view、column、字段类型，可预览前 100 行。
- 点位缺料报警：复用当前连接，保留点位别名、点位编码、点位级监测周期、弹窗确认。
- 本地 H2 测试库继续保留，用于离线验证。

不包含：

- 通用 SQL 编辑器。
- 表数据编辑。
- INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE 等写入或 DDL 功能。
- ER 图、导入导出、权限管理、密码保存。

## 架构

继续使用 Java Swing + JDBC。主窗口改为左侧导航、右侧页面、底部日志的结构。

模块边界：

- `ConnectionProfile`：单个连接配置，不包含密码。
- `ConnectionProfileStore`：保存和读取多个连接配置。
- `ReadOnlyConnectionFactory`：统一创建只读 JDBC 连接，设置 `conn.setReadOnly(true)`、`setAutoCommit(false)`，PostgreSQL 下执行 `SET TRANSACTION READ ONLY` 和 `SET statement_timeout='8s'`。
- `DbMetadataRepository`：只通过 JDBC metadata 和固定 SELECT 实现数据库浏览。
- `PointRepository`：继续只负责点位业务查询，改为复用只读连接工厂。
- `ShelfPointMonitorApp`：组织三页 UI，连接管理页负责当前连接，数据库浏览器页和点位报警页复用当前连接。

## 只读安全边界

第一阶段的安全边界是功能级和代码级双重限制：

- UI 不提供 SQL 编辑器、不提供修改按钮。
- 数据库浏览器只调用 `DatabaseMetaData`、固定 `SELECT * FROM schema.table LIMIT 100`。
- schema/table 标识符必须通过 `[A-Za-z_][A-Za-z0-9_]*` 校验。
- JDBC 连接统一走只读连接工厂。
- 预览查询限制 1 到 500 行，UI 固定使用 100 行。
- 自动报警仍只查询 `shelf_point_status` 指定点位。

## 数据流

连接管理：

1. 用户选择或填写连接配置。
2. 输入密码。
3. 点击测试连接。
4. 程序创建只读连接并记录为当前连接。
5. 当前连接供数据库浏览器和点位报警使用。

数据库浏览器：

1. 用户点击刷新 schema。
2. 程序读取 metadata schema 列表。
3. 用户选择 schema 后加载 table/view。
4. 用户选择 table/view 后加载字段。
5. 用户点击预览前 100 行，程序执行固定 SELECT 并展示结果。

点位报警：

1. 使用当前连接和当前密码。
2. 读取关注点位表。
3. 手动检测查询全部点位。
4. 自动监控按点位周期查询到期点位。
5. 发现异常弹窗，用户点击已关注后同一异常不重复弹出。

## 测试策略

- `ConnectionProfileStoreTest`：验证多个连接可保存和读取，且不会保存密码。
- `DbMetadataRepositoryTest`：使用本地 H2 测试库验证 schema、table、column、预览数据可读，非法标识符被拒绝。
- `ReadOnlyConnectionFactoryTest`：验证 PostgreSQL/H2 外的本地路径和只读连接行为。
- 更新 `ShelfPointMonitorAppUiTest`：不再假设单页面结构，改为递归检查所有 `GridBagLayout` 容器无组件重叠。
- 保留现有点位报警测试。

## 验收标准

- 程序可启动，左侧有连接管理、数据库浏览器、点位缺料报警。
- 默认包含示例 PostgreSQL 配置和本地测试库配置。
- 密码不会进入配置文件。
- 数据库浏览器可在本地测试库看到 `public.shelf_point_status` 字段并预览数据。
- 点位报警功能仍可运行。
- 构建脚本全部测试通过，并生成新的压缩包。


