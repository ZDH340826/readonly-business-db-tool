# 只读业务数据库工具

当前版本：`0.5.0-rc.1`

这是一个 Windows 本地 Java Swing 只读数据库工具，用于只读查看业务数据库，并按区域/物料组监控点位缺料风险。它不是通用数据库客户端替代品，不提供 SQL 编辑器，不提供任何数据修改功能。

## 功能

- 连接管理：保存多个连接配置，支持 PostgreSQL 和本地 H2 测试库。
- 密码策略：密码只在本次运行中输入和使用，不保存到配置文件。
- 数据库浏览器：只读查看 schema、table/view、column，并预览前 100 行。
- 点位状态看板：直接展示每个点位的 `有料`、`无料`、`未查到`、`停用` 状态。
- 点位组缺料报警：按区域、物料组配置 1 个使用位和多个备用位。
- 分组规则：每个点位组独立配置使用位、备用位、备用位下限和是否参与报警判断。
- 每组检测周期：不同点位组可以设置不同查询周期，自动监控只检测到期组。
- 持续缺料报警：报警持续时间和检测周期分开配置，按真实经过时间判断是否弹窗。
- 报警确认：报警窗口必须手动点击“已关注”才会关闭。
- 日志系统：记录每次检测快照和报警/确认/恢复事件。
- 本地测试库：无需连接现场数据库即可验证界面、浏览器和报警逻辑。
- 八页工作台：监控总览、点位组管理、报警中心、连接管理、数据查询、数据源浏览器、日志与系统、系统设置。
- 现场交付：内嵌 Java 运行时、中文启动/预检/诊断脚本、脱敏诊断包和中文验收/回滚文档。

## 只读边界

程序不提供通用 SQL 编辑器，不提供表格编辑、插入、删除、更新按钮。

数据库访问边界：

```sql
SET TRANSACTION READ ONLY;
```

代码层限制：

- 所有 JDBC 连接统一走只读连接工厂。
- 数据库浏览器只使用 JDBC metadata 和固定 SELECT。
- 表预览只执行 `SELECT * FROM schema.table LIMIT 100`。
- schema/table 名必须通过标识符校验。
- 点位组监控只查询配置的点位，不写入数据库。
- 密码只保存在本次运行内存中，不写入 `data/connections.properties` 或发布包。

生产环境仍建议使用数据库只读账号运行。

## 构建与运行

```powershell
.\build.ps1
.\dist\ShelfPointMonitor\启动工具.bat
.\dist\ShelfPointMonitor\现场部署检查.bat
```

构建脚本会读取根目录 `VERSION`，生成版本化压缩包：

```text
dist/ReadonlyBusinessDbTool-v0.5.0-rc.1.zip
```

## 现场连接配置

新建连接时由现场运维在本机填写：

| 字段 | 建议值/说明 |
|---|---|
| 连接名称 | 自定义，例如“现场数据库” |
| 服务器地址/IP | 现场数据库 IP，本地填写，不提交到 Git |
| 端口 | `2345` |
| 数据库名 | `cms_web` |
| 数据库空间/Schema | `public` |
| 用户名 | 本地填写，不提交到 Git |
| 密码 | 本次运行输入，不保存到配置文件 |
| SSL模式 | `disable` |

连接配置保存到本机 `data/connections.properties`。该文件位于运行目录内，已被 `.gitignore` 排除。

## 现场点位表映射

公开仓库允许保留只读查询所需的表名和字段名：

| 数据库字段 | 软件内部含义 |
|---|---|
| `cms_web.public.tcs_map_data` | 点位状态来源表 |
| `map_data_code` | 点位编码 |
| `pod_code` | 货架编号 |
| `pod_status` | 货架状态 |
| `status` | 点位状态 |
| `ind_lock` | 锁定状态 |
| `area_code` | 区域编码 |
| `relate_area_code` | 关联区域编码 |
| `date_chg` | 更新时间 |

禁止提交真实 IP、用户名、密码、生产点位组、真实点位清单和现场导出数据。

## 默认样例

默认本地测试库和点位组使用公开样例数据：

- 使用位：`USE_POINT_001`
- 备用位：`BACKUP_POINT_001`
- 备用位：`BACKUP_POINT_002`
- 备用位：`BACKUP_POINT_003`
- 备用位：`BACKUP_POINT_004`

默认规则：

- 每 1 分钟检测一次该点位组。
- 使用位无货架。
- 4 个备用位中至少 3 个需要有货架。
- 备用位有料下限参与报警判断。
- 条件持续 5 分钟后报警。

## 文档

- 使用说明书：[docs/manuals/point-shortage-alert-user-manual.md](docs/manuals/point-shortage-alert-user-manual.md)
- 现场运维交付手册：[docs/manuals/现场运维交付手册.md](docs/manuals/现场运维交付手册.md)
- 现场验收清单：[docs/ops/现场验收清单.md](docs/ops/现场验收清单.md)
- 回滚说明：[docs/ops/回滚说明.md](docs/ops/回滚说明.md)
- 版本策略：[docs/VERSIONING.md](docs/VERSIONING.md)
- 发布说明：[docs/releases/v0.5.0-rc.1.md](docs/releases/v0.5.0-rc.1.md)

## 版本

见 [CHANGELOG.md](CHANGELOG.md)。
