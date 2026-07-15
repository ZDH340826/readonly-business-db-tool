# 多使用位与本地全场景交付实施计划

> **执行要求：** 实施时使用 `superpowers:executing-plans`，每项功能使用 `superpowers:test-driven-development`，交付前使用 `superpowers:verification-before-completion`。

**目标：** 点位组允许配置一个或多个启用的使用位；只要任一启用使用位无料或未查到，就满足“使用位异常”条件。备用位阈值、持续时间、查询失败和恢复重新计时语义保持不变。同时让低学历运维能看懂配置和状态，并用纯本地 H2 示例库覆盖常见及异常场景。

**约束：** 业务数据库只读；开发和验收只连接本地 H2；不纳入或修改未跟踪的 `docs/templates/点位组导入模板.csv`；本阶段不实现 CSV 导入导出；界面统一把 `map_data_code` 称为“地码”、`pod_code` 称为“货码”。

**技术路线：** 保留现有配置格式和 `GroupEvaluation.usePointEmpty()` 兼容接口，只放宽领域校验并增加可计算的使用位汇总。监控逻辑继续从每个启用点位的只读查询结果计算，界面用中文比例和异常点位列表解释结果。本地示例组和数据库行由同一份代码目录生成，打包脚本不再维护重复的硬编码组配置。

---

## 任务 1：放宽领域校验并锁定报警真值

**文件：**

- 修改：`src/com/local/monitor/PointGroupDefinition.java`
- 测试：`test/com/local/monitor/GroupConfigStoreTest.java`
- 测试：`test/com/local/monitor/GroupMonitorLogicTest.java`

### 步骤 1：先写失败测试

增加以下用例：

- 两个启用使用位的点位组可以创建、保存并重新加载。
- 零个启用使用位仍被拒绝。
- 两个使用位全部有料时，不满足使用位异常条件。
- 两个使用位中任一个无料时，满足使用位异常条件。
- 使用位记录缺失时按异常处理。
- 查询失败保持 `QUERY_FAILED`，不进入持续计时。
- 从满足规则恢复后，再次异常从零重新计时。

运行针对性测试，确认新用例在“只能有一个使用位”的旧实现上失败。

### 步骤 2：做最小实现

将 `PointGroupDefinition.validatePoints` 的约束从“恰好一个启用使用位”改为“至少一个启用使用位”，保留以下约束：

- 点位 ID、地码不得重复；
- 至少一个启用备用位；
- 备用位最小可用数不得超过启用备用位数量。

监控判定明确采用：

```java
anyUsePointUnavailable = enabledUsePoints.stream().anyMatch(point -> !point.available());
```

可以重命名局部变量提高可读性，但保留 `usePointEmpty()` 公共接口，避免破坏日志和已有调用方。

### 步骤 3：验证并提交

运行针对性测试、`git diff --check` 和 `./build.ps1`。提交：

`feat: allow multiple use points`

---

## 任务 2：提供可理解的多使用位状态说明

**文件：**

- 修改：`src/com/local/monitor/GroupEvaluation.java`
- 修改：`src/com/local/monitor/GroupStatusText.java`
- 修改：`src/com/local/monitor/ShelfPointMonitorApp.java`
- 测试：`test/com/local/monitor/GroupMonitorLogicTest.java`
- 测试：`test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

### 步骤 1：先写失败测试

断言评估结果能提供：

- 使用位总数；
- 有料使用位数量；
- 异常使用位列表；
- 摘要包含“使用位有料 1/2”和“任一使用位无料：是”；
- 查询失败摘要仍只表达本次未获得状态，不误报有料/无料。

### 步骤 2：增加兼容的计算接口

在 `GroupEvaluation` 基于 `pointStatuses()` 增加只读计算方法，例如：

```java
int usePointTotal()
int usePointAvailableCount()
List<PointStatusView> unavailableUsePoints()
```

不改变构造器参数和序列化格式。

### 步骤 3：改为直接、中文、可核对的文案

- 规则复选框改为“任一启用使用位无料”；
- 状态摘要展示“使用位有料 X/Y”；
- 明确展示“任一使用位无料：是/否”；
- 点位明细使用“地码”和“货码”，不再向运维暴露数据库列名或英文角色值；
- 报警对话框列出具体异常使用位，便于现场定位。

### 步骤 4：验证并提交

运行针对性测试、`git diff --check` 和 `./build.ps1`。提交：

`feat: explain multiple use point alarms`

---

## 任务 3：改善点位组配置和状态看板

**文件：**

- 修改：`src/com/local/monitor/GroupManagementPage.java`
- 修改：`src/com/local/monitor/ShelfPointMonitorApp.java`
- 可能新增：`src/com/local/monitor/PointRoleDisplay.java`
- 测试：`test/com/local/monitor/ShelfPointMonitorAppUiTest.java`
- 测试：`test/com/local/monitor/AppThemeTest.java`

### 步骤 1：先写界面结构测试

覆盖：

- 角色表格始终显示“使用位/备用位”，保存时仍能正确转换为 `PointRole`；
- 同组多个使用位可添加、保存、重新选择并显示；
- 窄窗口和多点位时卡片不重叠，状态区可滚动；
- 关键按钮和判断结论完整可见。

### 步骤 2：按第一性原理整理界面

运维需要回答的只有三个问题：现在是否正常、哪个地码异常、下一步看哪里。因此：

- 规则区用一句浅色说明解释“任一使用位异常 + 备用位不足 + 持续达到时间才报警”；
- 点位角色用中文下拉框，新增按钮拆成“添加使用位”和“添加备用位”；
- 使用位卡片集中在上方、备用位卡片在下方；
- 卡片突出“有料/无料/未查到”，其次显示别名、地码、货码和原因；
- 颜色只是辅助，文字必须独立表达状态；
- 沿用现有主题、字号和无横向遮挡规则，不做与功能无关的重绘。

### 步骤 3：验证并提交

运行界面结构测试、`git diff --check` 和 `./build.ps1`。提交：

`feat: improve point group status board`

---

## 任务 4：建立本地 H2 全场景目录

**文件：**

- 新增：`src/com/local/monitor/LocalDemoCatalog.java`
- 修改：`src/com/local/monitor/LocalTestDatabase.java`
- 修改：`src/com/local/monitor/GroupConfigStore.java`
- 修改：`src/com/local/monitor/LocalTestDbTool.java`
- 测试：`test/com/local/monitor/LocalTestDatabaseTest.java`
- 测试：`test/com/local/monitor/GroupConfigStoreTest.java`
- 测试：`test/com/local/monitor/FieldDeliveryScenarioTest.java`

### 步骤 1：先写目录完整性测试

至少覆盖以下互相独立的示例组：

1. 双使用位，一个无料，备用位不足——满足规则；
2. 双使用位全部有料，备用位不足——不满足规则；
3. 使用位无料，备用位充足——不满足完整报警规则；
4. 使用位状态异常、备用位锁定——能显示具体原因；
5. 配置中含停用点位——停用点位不参与计数；
6. 配置地码在数据库缺失——显示“未查到”；
7. 正常组——全部条件正常；
8. 临界组——备用位数量恰好达到下限。

同时断言所有地码、货码均为公开虚构值，不匹配现场编码特征。

### 步骤 2：建立单一事实来源

`LocalDemoCatalog` 同时返回：

- 本地示例数据库行；
- 对应的 `PointGroupDefinition` 列表。

`LocalTestDatabase` 和 `GroupConfigStore.defaultGroups()` 都从该目录读取，避免数据行与组配置漂移。缺失场景只存在于组配置中，不向数据库插入对应行。

### 步骤 3：扩展本地工具

给 `LocalTestDbTool reset` 增加可选的组配置输出路径；传入时同时写入示例组配置。工具继续调用 `DbConfig.localTest` 并保留 `ensureLocal`，从机制上阻止对真实数据库写入。

### 步骤 4：验证并提交

运行针对性测试、源文件敏感信息检查、`git diff --check` 和 `./build.ps1`。提交：

`feat: expand local demo scenarios`

---

## 任务 5：让发布包自动生成同一套本地示例

**文件：**

- 修改：`build.ps1`
- 修改：`test/com/local/monitor/ShelfPointMonitorSelfTestTest.java`
- 修改：`test/com/local/monitor/FieldDeploymentPreflightTest.java`
- 测试：`test/com/local/monitor/WindowsPathPackagingTest.java`

### 步骤 1：先写失败测试

断言发布包中的本地 H2 行数、示例组数量、多使用位组和缺失地码场景均存在；现场连接配置仍只有占位符，不包含凭据。

### 步骤 2：去掉打包脚本中的重复硬编码

删除 `build.ps1` 手写单组 `group-config.properties` 的块，改为调用：

```powershell
LocalTestDbTool reset <本地数据库路径> <组配置路径>
```

打包自检和部署预检继续在生成发布包之后执行。

### 步骤 3：验证并提交

运行打包相关测试、`git diff --check` 和 `./build.ps1`。提交：

`build: package full local demo catalog`

---

## 任务 6：文档和最终本地验收

**文件：**

- 修改：`README.md`
- 修改：`CHANGELOG.md`
- 修改：`docs/USER_MANUAL.md`（如存在）
- 修改：与本次交付直接相关的 `docs/ai/` 记录

### 步骤 1：更新用户文档

说明：

- 使用位可为一个或多个；
- 任一启用使用位无料或未查到时，使用位条件成立；
- 示例组分别代表什么；
- 所有演示和验收均使用本地 H2；
- “地码/货码”的对应含义；
- CSV 导入导出仍是下一阶段，不假装已经交付。

### 步骤 2：完成全量自动验证

依次执行：

```powershell
git diff --check
.\build.ps1
git status --short
```

检查构建日志中的测试、自检、预检、诊断包和压缩包哈希均成功。

### 步骤 3：完成真实界面验收

只启动发布包里的本地 H2 配置，逐项确认：

- 多使用位组可以打开、编辑和保存；
- “双使用位一个无料”明确指出异常地码；
- 正常、备用不足、状态异常、锁定、停用、缺失、临界场景均可浏览；
- 地码和货码用中文展示；
- 状态卡无重叠、无截断，按钮可用；
- 不发起任何现场数据库连接。

保存必要的本地验收截图到临时目录，不把构建产物或现场配置提交到 Git。

### 步骤 4：文档提交与交付审计

提交：

`docs: document multiple use point monitoring`

随后审计新增提交、工作区、未跟踪文件、敏感信息、构建产物和测试结果。只有所有证据一致时才宣布完成。
