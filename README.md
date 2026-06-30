# 只读业务数据库工具 - 第一阶段

当前版本：`0.1.0`

这是一个 Windows 本地 Java Swing 工具，用于现场只读数据库查看和点位缺料报警。它不是 database client 替代品，第一阶段只做业务需要的安全子集。

## 功能

- 连接管理：保存多个连接配置，支持示例 PostgreSQL 和本地测试库。
- 密码策略：密码只在本次运行中输入和使用，不保存到任何配置文件。
- 数据库浏览器：只读查看 schema、table/view、column，可预览表或视图前 100 行。
- 点位缺料报警：监控指定点位是否有货架，支持每个点位独立监测周期。
- 报警确认：报警窗口必须手动点击“已关注”才会关闭；同一异常在恢复前不重复弹窗。
- 本地测试库：可不连接现场数据库，先验证界面、浏览器和报警逻辑。

## 严格只读边界

程序不提供通用 SQL 编辑器，不提供任何数据编辑按钮。

数据库访问限制：

```sql
SET TRANSACTION READ ONLY;
```

代码层限制：

- 所有 JDBC 连接统一走只读连接工厂。
- 数据库浏览器只使用 JDBC metadata 和固定 SELECT。
- 表预览只执行 `SELECT * FROM schema.table LIMIT 100`。
- schema/table 名必须通过标识符校验。
- 自动报警只查询配置的点位，不写入数据库。

仍建议使用数据库只读账号运行。

## 运行

```powershell
.\build.ps1
.\dist\ShelfPointMonitor\ShelfPointMonitor.bat
```

启动后先进入“连接管理”，选择连接，输入密码，点击“测试并使用连接”。之后可进入“数据库浏览器”或“点位缺料报警”。

构建脚本会读取根目录 `VERSION`，生成版本化压缩包：

```text
dist/ReadonlyBusinessDbTool-v0.1.0.zip
```

版本策略见 [docs/VERSIONING.md](docs/VERSIONING.md)。

## 默认点位

- 使用位：`USE_POINT_001`，监测周期 1 分钟。
- 备用位：`BACKUP_POINT_001`，监测周期 10 分钟。


