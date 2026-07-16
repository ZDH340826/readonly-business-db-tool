# 可读宽表与固定重要列实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 保护现有按钮可读性修复，并让所有标准数据表格按需横向滚动；在数据源预览中提供可见、可恢复的固定列功能，使地码和货码能够一键相邻固定在左侧。

**Architecture:** 通用表格列宽与滚动行为集中在 `UiFactory` 和两个小型 Swing 组件中；固定列使用共享 `TableModel`、共享行选择模型和 `JScrollPane` 行头视图实现，数据库返回模型保持不变。本机列布局由独立的无敏感信息属性文件保存，数据源页面只负责提供当前 schema/表名和显示组件。

**Tech Stack:** Java 17、Swing、JDBC 元数据只读预览、PowerShell `build.ps1`、无新增依赖。

## Global Constraints

- 仅使用本地 H2 示例数据库测试，不连接真实工作环境。
- `map_data_code` 的运维名称为“地码”，`pod_code` 的运维名称为“货码”。
- 固定列和列顺序只改变 Swing 视图，不改变 SQL、`TablePreview`、数据库字段顺序或数据。
- 所有运维入口和错误提示使用直观中文，不依赖右键菜单或拖动表头。
- 每张表最多固定 4 列；固定区预计超过预览框 45% 时拒绝应用并显示“固定列太多，请先取消一列”。
- 不修改旧版页面结构，不进行无关 UI 美化。
- 保留工作区中用户已有的 `docs/templates/点位组导入模板.csv`，本计划不把它混入表格提交。

---

### Task 0: 保护现有按钮可读性成果

**Files:**
- Existing modifications: `src/com/local/monitor/AppTheme.java`
- Existing modifications: `src/com/local/monitor/GroupManagementPage.java`
- Existing modifications: `src/com/local/monitor/UiFactory.java`
- Existing modifications: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

**Interfaces:**
- Consumes: 已完成的 Windows 按钮实际绘制回归测试。
- Produces: 一个只包含按钮颜色与测试的本地提交，作为后续表格开发的干净基线。

- [ ] **Step 1: 确认提交范围不包含模板或设计外文件**

Run:

```powershell
git status --short
git diff -- src/com/local/monitor/AppTheme.java src/com/local/monitor/GroupManagementPage.java src/com/local/monitor/UiFactory.java test/com/local/monitor/ShelfPointMonitorAppUiTest.java
```

Expected: 仅四个按钮相关文件有差异；`docs/templates/` 仍为未跟踪且不暂存。

- [ ] **Step 2: 执行完整基线验证**

Run:

```powershell
.\build.ps1
git diff --check
```

Expected: 所有测试、打包自检、现场部署检查和本地 H2 只读查询通过；`git diff --check` 无输出。

- [ ] **Step 3: 提交按钮成果**

Run:

```powershell
git add -- src/com/local/monitor/AppTheme.java src/com/local/monitor/GroupManagementPage.java src/com/local/monitor/UiFactory.java test/com/local/monitor/ShelfPointMonitorAppUiTest.java
git commit -m "fix: keep action buttons readable on Windows"
```

Expected: 新提交只包含上述四个文件。

---

### Task 1: 建立按内容计算列宽的通用行为

**Files:**
- Create: `src/com/local/monitor/ReadableTableColumns.java`
- Create: `test/com/local/monitor/ReadableTableTest.java`
- Modify: `src/com/local/monitor/UiFactory.java`
- Modify: `build.ps1`

**Interfaces:**
- Consumes: `UiFactory.configureTable(JTable)` 的现有统一表格入口。
- Produces: `ReadableTableColumns.install(JTable table)` 和 `ReadableTableColumns.resizeNow(JTable table)`；列宽限制为 80–320 像素，最多采样前 100 行。

- [ ] **Step 1: 写入失败测试**

在 `ReadableTableTest.main` 调用三个场景：

```java
private static void standardTablesDoNotCompressAllColumns() {
    JTable table = tableWithWideContent();
    UiFactory.configureTable(table);
    TestSupport.assertEquals(JTable.AUTO_RESIZE_OFF, table.getAutoResizeMode(),
            "wide tables must preserve readable column widths");
}

private static void headersAndRowsDetermineBoundedPreferredWidths() {
    JTable table = tableWithWideContent();
    UiFactory.configureTable(table);
    ReadableTableColumns.resizeNow(table);
    for (int index = 0; index < table.getColumnCount(); index++) {
        int width = table.getColumnModel().getColumn(index).getPreferredWidth();
        TestSupport.assertTrue(width >= 80, "column must retain a readable minimum width");
        TestSupport.assertTrue(width <= 320, "one value must not create an unbounded column");
    }
}

private static void modelStructureChangesRecalculateNewColumns() {
    DefaultTableModel model = new DefaultTableModel(new Object[] {"短列"}, 0);
    JTable table = new JTable(model);
    UiFactory.configureTable(table);
    model.setColumnIdentifiers(new Object[] {"地码（map_data_code）", "货码（pod_code）", "更新时间"});
    ReadableTableColumns.resizeNow(table);
    TestSupport.assertEquals(3, table.getColumnModel().getColumnCount(),
            "new preview columns must participate in readable sizing");
}
```

- [ ] **Step 2: 运行测试并确认正确失败**

Run:

```powershell
.\build.ps1
```

Expected: 编译或 `ReadableTableTest` 失败，因为 `ReadableTableColumns` 尚不存在或自动压缩仍开启；不得是语法错误。

- [ ] **Step 3: 实现最小列宽控制器**

`ReadableTableColumns` 必须：

```java
final class ReadableTableColumns {
    static final int MIN_WIDTH = 80;
    static final int MAX_WIDTH = 320;
    static final int SAMPLE_ROWS = 100;

    static void install(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // 用 client property 防止重复安装；监听当前 TableModel 变化并在 EDT 重新计算。
    }

    static void resizeNow(JTable table) {
        // 使用表头和最多 100 行的 renderer preferredSize，增加 24 像素留白，限制在 80–320。
    }
}
```

`UiFactory.configureTable` 在现有字体、颜色和行高设置完成后调用：

```java
ReadableTableColumns.install(table);
```

`build.ps1` 在 `AppThemeTest` 前增加：

```powershell
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ReadableTableTest
if ($LASTEXITCODE -ne 0) { throw "readable table tests failed with exit code $LASTEXITCODE" }
```

- [ ] **Step 4: 运行测试并确认通过**

Run:

```powershell
.\build.ps1
```

Expected: `ReadableTableTest PASS`，其余现有测试保持通过。

- [ ] **Step 5: 提交通用列宽行为**

Run:

```powershell
git add -- src/com/local/monitor/ReadableTableColumns.java src/com/local/monitor/UiFactory.java test/com/local/monitor/ReadableTableTest.java build.ps1
git commit -m "feat: keep table columns readable"
```

---

### Task 2: 为所有标准数据表提供按需横向滚动

**Files:**
- Create: `src/com/local/monitor/ReadableTableScrollPane.java`
- Modify: `src/com/local/monitor/UiFactory.java`
- Modify: `src/com/local/monitor/OverviewPage.java`
- Modify: `src/com/local/monitor/AlertCenterPage.java`
- Modify: `src/com/local/monitor/DataQueryPage.java`
- Modify: `src/com/local/monitor/DataSourceBrowserPage.java`
- Modify: `src/com/local/monitor/GroupManagementPage.java`
- Modify: `src/com/local/monitor/LogsSystemPage.java`
- Modify: `test/com/local/monitor/ReadableTableTest.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

**Interfaces:**
- Consumes: 已安装 `AUTO_RESIZE_OFF` 的 `JTable`。
- Produces: `UiFactory.tableScrollPane(JTable table)`；横向滚动条策略为 `HORIZONTAL_SCROLLBAR_AS_NEEDED`，并支持 `Shift + 鼠标滚轮`。

- [ ] **Step 1: 写入失败测试**

新增：

```java
private static void tableScrollPaneShowsHorizontalRangeOnlyWhenNeeded() {
    JTable table = tableWithEightReadableColumns();
    JScrollPane pane = UiFactory.tableScrollPane(table);
    pane.setSize(420, 240);
    pane.doLayout();
    TestSupport.assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED,
            pane.getHorizontalScrollBarPolicy(), "wide tables need an as-needed horizontal bar");
    TestSupport.assertTrue(table.getPreferredSize().width > pane.getViewport().getExtentSize().width,
            "wide table content must exceed the viewport instead of being compressed");
}

private static void shiftWheelMovesTheHorizontalBar() {
    ReadableTableScrollPane pane = new ReadableTableScrollPane(tableWithEightReadableColumns());
    // 布局后向 pane 派发带 SHIFT_DOWN_MASK 的 MouseWheelEvent。
    // 断言 horizontalScrollBar.value 增加且 verticalScrollBar.value 不变。
}
```

在 `ShelfPointMonitorAppUiTest` 遍历 8 页中的可见 `JTable`，断言其祖先滚动容器由 `UiFactory.tableScrollPane` 创建且为按需横向滚动。

- [ ] **Step 2: 运行并确认测试失败**

Run:

```powershell
.\build.ps1
```

Expected: 因 `UiFactory.tableScrollPane` 或 `ReadableTableScrollPane` 不存在而失败。

- [ ] **Step 3: 实现滚动容器并替换页面表格容器**

实现：

```java
final class ReadableTableScrollPane extends JScrollPane {
    ReadableTableScrollPane(JTable table) {
        super(table);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent event) {
        if (event.isShiftDown() && getHorizontalScrollBar().isVisible()) {
            JScrollBar bar = getHorizontalScrollBar();
            bar.setValue(bar.getValue() + event.getWheelRotation() * bar.getUnitIncrement());
            event.consume();
            return;
        }
        super.processMouseWheelEvent(event);
    }
}
```

`UiFactory` 增加：

```java
public static JScrollPane tableScrollPane(JTable table) {
    return new ReadableTableScrollPane(table);
}
```

只替换页面内包裹 `JTable` 的 `new JScrollPane(table)`；树、列表和文本区域继续使用普通 `JScrollPane`。

- [ ] **Step 4: 验证所有页面表格**

Run:

```powershell
.\build.ps1
```

Expected: `ReadableTableTest PASS`、`ShelfPointMonitorAppUiTest PASS`，8 页现有行为无回归。

- [ ] **Step 5: 提交横向滚动**

Run:

```powershell
git add -- src/com/local/monitor/ReadableTableScrollPane.java src/com/local/monitor/UiFactory.java src/com/local/monitor/OverviewPage.java src/com/local/monitor/AlertCenterPage.java src/com/local/monitor/DataQueryPage.java src/com/local/monitor/DataSourceBrowserPage.java src/com/local/monitor/GroupManagementPage.java src/com/local/monitor/LogsSystemPage.java test/com/local/monitor/ReadableTableTest.java test/com/local/monitor/ShelfPointMonitorAppUiTest.java
git commit -m "feat: add horizontal table browsing"
```

---

### Task 3: 建立固定列顺序模型与本机存储

**Files:**
- Create: `src/com/local/monitor/PinnedColumnLayout.java`
- Create: `src/com/local/monitor/TableColumnLayoutStore.java`
- Create: `test/com/local/monitor/PinnedColumnLayoutTest.java`
- Create: `test/com/local/monitor/TableColumnLayoutStoreTest.java`
- Modify: `build.ps1`

**Interfaces:**
- Produces: `PinnedColumnLayout.normalize(List<String> available, List<String> requested)`、`presetFor(List<String> available)`、`moveUp(List<String>, int)`、`moveDown(List<String>, int)`。
- Produces: `TableColumnLayoutStore.load(String schema, String table)`、`save(String schema, String table, List<String>)`、`clear(String schema, String table)`。

- [ ] **Step 1: 写入布局模型失败测试**

```java
private static void presetPinsLandCodeBeforePodCode() {
    List<String> available = List.of("status", "map_data_code", "date_chg", "pod_code");
    TestSupport.assertEquals(List.of("map_data_code", "pod_code"),
            PinnedColumnLayout.presetFor(available),
            "preset must pin land code immediately before pod code");
}

private static void normalizeIgnoresMissingDuplicatesAndRejectsMoreThanFour() {
    TestSupport.assertEquals(List.of("pod_code", "status"),
            PinnedColumnLayout.normalize(
                    List.of("map_data_code", "pod_code", "status"),
                    List.of("missing", "pod_code", "pod_code", "status")),
            "saved layouts must tolerate schema changes");
    TestSupport.assertThrows(IllegalArgumentException.class,
            () -> PinnedColumnLayout.normalize(
                    List.of("a", "b", "c", "d", "e"),
                    List.of("a", "b", "c", "d", "e")),
            "more than four fixed columns must be rejected");
}
```

- [ ] **Step 2: 写入存储失败测试**

```java
private static void roundTripsUnicodeAndPunctuationWithoutSecrets() throws Exception {
    Path path = Files.createTempDirectory("column-layout").resolve("table-column-layout.properties");
    TableColumnLayoutStore store = new TableColumnLayoutStore(path);
    store.save("public", "tcs_map_data", List.of("map_data_code", "pod_code"));
    TestSupport.assertEquals(List.of("map_data_code", "pod_code"),
            store.load("public", "tcs_map_data"), "layout must round-trip");
    TestSupport.assertFalse(Files.readString(path).toLowerCase(java.util.Locale.ROOT).contains("password"),
            "layout file must not contain database secrets");
    store.clear("public", "tcs_map_data");
    TestSupport.assertTrue(store.load("public", "tcs_map_data").isEmpty(),
            "restore original order must clear only this table layout");
}
```

- [ ] **Step 3: 运行并确认失败**

Run: `./build.ps1`

Expected: 新类型不存在导致编译失败。

- [ ] **Step 4: 实现纯布局模型与安全属性存储**

`PinnedColumnLayout` 使用大小写不敏感匹配、保持请求顺序、去重、忽略不存在列，超过 4 列时抛出：

```java
throw new IllegalArgumentException("固定列太多，请先取消一列");
```

`TableColumnLayoutStore` 使用 UTF-8 `Properties`，以 URL-safe Base64 编码 schema、表名和列名；值只包含列标识符，不保存数据库值、连接、账号或密码。文件损坏时该表返回空布局，不阻止预览。

在 `build.ps1` 中加入两个测试入口，并确保每个入口检查 `$LASTEXITCODE`。

- [ ] **Step 5: 验证并提交**

Run:

```powershell
.\build.ps1
git add -- src/com/local/monitor/PinnedColumnLayout.java src/com/local/monitor/TableColumnLayoutStore.java test/com/local/monitor/PinnedColumnLayoutTest.java test/com/local/monitor/TableColumnLayoutStoreTest.java build.ps1
git commit -m "feat: persist fixed table columns"
```

---

### Task 4: 实现冻结列组件和直观中文选择面板

**Files:**
- Create: `src/com/local/monitor/DatabaseFieldLabel.java`
- Create: `src/com/local/monitor/PinnedColumnChooserPanel.java`
- Create: `src/com/local/monitor/PinnedTablePane.java`
- Create: `test/com/local/monitor/PinnedTablePaneTest.java`
- Modify: `build.ps1`

**Interfaces:**
- Consumes: `PinnedColumnLayout`、`TableColumnLayoutStore`、`UiFactory.tableScrollPane`。
- Produces: `PinnedTablePane(JTable, TableColumnLayoutStore)`、`showTable(String schema, String table)`、`applyPinnedColumns(List<String>)`、`restoreOriginalOrder()`、`pinnedColumns()`、`scrollingColumns()`。
- Produces: `DatabaseFieldLabel.display(String identifier)`，其中 `map_data_code` → `地码（map_data_code）`，`pod_code` → `货码（pod_code）`。

- [ ] **Step 1: 写入冻结行为失败测试**

```java
private static void presetCreatesTwoFrozenAdjacentColumnsWithoutChangingTheModel() throws Exception {
    DefaultTableModel model = previewModel(
            "status", "map_data_code", "date_chg", "pod_code", "area_code");
    JTable scrolling = new JTable(model);
    PinnedTablePane pane = new PinnedTablePane(scrolling, temporaryStore());
    pane.setSize(900, 500);
    pane.showTable("public", "tcs_map_data");
    pane.applyPinnedColumns(PinnedColumnLayout.presetFor(modelColumnNames(model)));

    TestSupport.assertEquals(List.of("map_data_code", "pod_code"), pane.pinnedColumns(),
            "land code and pod code must be adjacent in the fixed area");
    TestSupport.assertEquals("status", model.getColumnName(0),
            "fixed columns must not rewrite database/model column order");
    TestSupport.assertTrue(pane.pinnedTable().getModel() == scrolling.getModel(),
            "fixed and scrolling areas must share one model");
    TestSupport.assertTrue(pane.pinnedTable().getSelectionModel() == scrolling.getSelectionModel(),
            "fixed and scrolling areas must share row selection");
}
```

同时加入以下具体测试：

```java
private static void restoreClearsOnlyTheViewLayout() throws Exception {
    DefaultTableModel model = previewModel("status", "map_data_code", "pod_code");
    PinnedTablePane pane = new PinnedTablePane(new JTable(model), temporaryStore());
    pane.setSize(900, 500);
    pane.showTable("public", "tcs_map_data");
    pane.applyPinnedColumns(List.of("map_data_code", "pod_code"));
    pane.restoreOriginalOrder();
    TestSupport.assertTrue(pane.pinnedColumns().isEmpty(), "restore must clear the fixed area");
    TestSupport.assertEquals(List.of("status", "map_data_code", "pod_code"), modelColumnNames(model),
            "restore must not rewrite model order");
}

private static void missingPresetColumnsProduceNoPreset() {
    TestSupport.assertTrue(PinnedColumnLayout.presetFor(List.of("status", "date_chg")).isEmpty(),
            "preset must be unavailable when either business column is missing");
}

private static void fixedAreaOverFortyFivePercentIsRejectedInChinese() throws Exception {
    DefaultTableModel model = previewModel("a", "b", "c", "d", "e");
    PinnedTablePane pane = new PinnedTablePane(new JTable(model), temporaryStore());
    pane.setSize(600, 400);
    IllegalArgumentException error = TestSupport.assertThrowsReturning(
            IllegalArgumentException.class,
            () -> pane.applyPinnedColumns(List.of("a", "b", "c", "d")),
            "oversized fixed area must be rejected");
    TestSupport.assertEquals("固定列太多，请先取消一列", error.getMessage(),
            "operator must receive a direct Chinese recovery message");
}

private static void structureChangeKeepsValidPinsAndLeavesNewColumnsScrollable() throws Exception {
    TableColumnLayoutStore store = temporaryStore();
    store.save("public", "tcs_map_data", List.of("map_data_code", "missing", "pod_code"));
    DefaultTableModel model = previewModel("status", "map_data_code", "pod_code", "new_column");
    PinnedTablePane pane = new PinnedTablePane(new JTable(model), store);
    pane.setSize(900, 500);
    pane.showTable("public", "tcs_map_data");
    TestSupport.assertEquals(List.of("map_data_code", "pod_code"), pane.pinnedColumns(),
            "valid saved pins must survive structure changes");
    TestSupport.assertTrue(pane.scrollingColumns().contains("new_column"),
            "new database columns must remain visible in the scrolling area");
}

private static DefaultTableModel previewModel(String... columns) {
    return new DefaultTableModel(columns, 0);
}

private static TableColumnLayoutStore temporaryStore() throws Exception {
    return new TableColumnLayoutStore(
            Files.createTempDirectory("pinned-table-pane").resolve("table-column-layout.properties"));
}

private static List<String> modelColumnNames(DefaultTableModel model) {
    List<String> names = new ArrayList<>();
    for (int index = 0; index < model.getColumnCount(); index++) {
        names.add(model.getColumnName(index));
    }
    return names;
}
```

在该测试文件的 `TestSupport` 中加入实际异常返回辅助方法：

```java
static <T extends Throwable> T assertThrowsReturning(
        Class<T> type,
        ThrowingRunnable action,
        String message) {
    try {
        action.run();
    } catch (Throwable error) {
        if (type.isInstance(error)) {
            return type.cast(error);
        }
        throw new AssertionError(message + " wrong exception=" + error, error);
    }
    throw new AssertionError(message + " no exception thrown");
}
```

- [ ] **Step 2: 运行并确认失败**

Run: `./build.ps1`

Expected: 新组件不存在导致编译失败。

- [ ] **Step 3: 实现字段中文标签**

```java
final class DatabaseFieldLabel {
    static String display(String identifier) {
        if ("map_data_code".equalsIgnoreCase(identifier)) {
            return "<html>地码<br><small>map_data_code</small></html>";
        }
        if ("pod_code".equalsIgnoreCase(identifier)) {
            return "<html>货码<br><small>pod_code</small></html>";
        }
        return identifier;
    }
}
```

模型标识符保持原值，仅设置 `TableColumn.headerValue` 为显示标签。

- [ ] **Step 4: 实现冻结区域**

`PinnedTablePane` 使用一个滚动 `JTable` 和一个共享模型的固定 `JTable`。将固定列的 `TableColumn` 从滚动列模型移入固定列模型；固定表放入主 `JScrollPane.rowHeaderView`，固定表头放入 `UPPER_LEFT_CORNER`。两表共享行选择模型、行高、字体和渲染样式。

应用布局前计算固定列首选宽度；超过 4 列或组件当前宽度的 45% 时抛出统一中文提示。`showTable` 从本机存储恢复并过滤无效列；`restoreOriginalOrder` 清除该 schema/表的设置并重建全部滚动列。

- [ ] **Step 5: 实现运维可见的选择面板**

`PinnedColumnChooserPanel` 显示：

- 每个真实列对应一个中文复选框；
- “一键选择地码和货码”；
- 当前固定顺序列表；
- “上移”“下移”；
- 对话框按钮“应用”“取消”；
- 主页面按钮“固定重要列”和“恢复原顺序”。

不使用只有右键才能发现的入口。超过上限、缺少列或保存失败时显示简短中文，不显示栈信息。

- [ ] **Step 6: 验证并提交组件**

Run:

```powershell
.\build.ps1
git add -- src/com/local/monitor/DatabaseFieldLabel.java src/com/local/monitor/PinnedColumnChooserPanel.java src/com/local/monitor/PinnedTablePane.java test/com/local/monitor/PinnedTablePaneTest.java build.ps1
git commit -m "feat: add fixed important table columns"
```

---

### Task 5: 接入数据源浏览器和本地预览流程

**Files:**
- Modify: `src/com/local/monitor/DataSourceBrowserPage.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `test/com/local/monitor/DataPagesTest.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

**Interfaces:**
- Consumes: `PinnedTablePane` 和 `TableColumnLayoutStore(Paths.get("data", "table-column-layout.properties"))`。
- Produces: `DataSourceBrowserPage.previewLoaded(String schema, String table)`，由预览数据进入 Swing 模型后调用。

- [ ] **Step 1: 写入页面接入失败测试**

在 `DataPagesTest` 构造含 `map_data_code`、`pod_code` 的预览模型，断言：

```java
DataSourceBrowserPage page = browserPageWithTemporaryLayoutStore();
page.previewLoaded("public", "tcs_map_data");
TestSupport.assertTrue(findButton(page, "固定重要列") != null,
        "preview must expose a visible fixed-column entry");
TestSupport.assertTrue(findButton(page, "恢复原顺序") != null,
        "preview must expose an obvious reset action");
TestSupport.assertTrue(findAll(page, ReadableTableScrollPane.class).size() >= 3,
        "browser tables must support horizontal browsing");
```

在 `ShelfPointMonitorAppUiTest` 断言页面不显示“point code”“shelf code”，并能找到“地码”“货码”的表头显示值。

- [ ] **Step 2: 运行并确认失败**

Run: `./build.ps1`

Expected: 构造器、`previewLoaded` 或按钮断言失败。

- [ ] **Step 3: 接入页面与应用**

`DataSourceBrowserPage` 创建 `PinnedTablePane` 包裹 `components.previewTable()`，将其作为“前 100 行只读预览”的内容，并公开：

```java
public void previewLoaded(String schema, String table) {
    previewPane.showTable(schema, table);
}
```

`ShelfPointMonitorApp` 增加：

```java
private final TableColumnLayoutStore tableColumnLayoutStore =
        new TableColumnLayoutStore(Paths.get("data", "table-column-layout.properties"));
private DataSourceBrowserPage dataSourceBrowserPage;
```

构建页面时保存实例；`previewSelectedTable()` 在 `previewModel` 设置字段和行之后调用：

```java
this.dataSourceBrowserPage.previewLoaded(schema, tableName);
```

该调用只使用已返回的字段名，不重新查询数据库。

- [ ] **Step 4: 验证本地 H2 页面行为**

Run:

```powershell
.\build.ps1
git diff --check
```

Expected: 全部测试与本地 H2 部署预检通过；差异检查无输出。

- [ ] **Step 5: 提交页面接入**

Run:

```powershell
git add -- src/com/local/monitor/DataSourceBrowserPage.java src/com/local/monitor/ShelfPointMonitorApp.java test/com/local/monitor/DataPagesTest.java test/com/local/monitor/ShelfPointMonitorAppUiTest.java
git commit -m "feat: expose fixed columns in data preview"
```

---

### Task 6: 完整视觉验收与阶段收尾

**Files:**
- Create ignored evidence: `build/table-visual-audit/*.png`

**Interfaces:**
- Consumes: 完整打包应用和本地 H2 示例数据。
- Produces: 1440×900 的页面证据、干净的功能提交边界和下一子系统可用基线。

- [ ] **Step 1: 执行完成前验证**

Run:

```powershell
.\build.ps1
git diff --check
git status --short
```

Expected: 全部测试、`SELF_TEST_OK`、现场部署检查、本地 H2 只读查询和打包通过；仅保留有意的未跟踪 `docs/templates/`。

- [ ] **Step 2: 启动时确保只选择本地测试连接**

打开打包应用后，先在“连接管理”选择“本地测试库 [local]”，再设置为当前连接。不得选择“现场数据库 [prod]”，不得输入真实主机、账号或密码。

- [ ] **Step 3: 逐项人工验收**

在 1440×900 下确认：

1. 数据源浏览器中部字段表与右侧预览表的标题不再挤成无法理解的省略文本；
2. 宽表底部出现横向滚动条，表头和数据同步左右移动；
3. “固定重要列”入口明显可见；
4. 一键固定后左侧顺序为“地码、货码”，滚动其他列时两列不移动；
5. 恢复原顺序后所有数据库列仍按原模型顺序显示；
6. 其他 7 页的宽表可横向浏览，窄表不出现无意义滚动范围；
7. 按钮可读性修复仍然有效。

- [ ] **Step 4: 完成阶段状态核验**

Run:

```powershell
git log --oneline -10
git status --short
```

Expected: 本阶段形成按钮保护、列宽、横向滚动、布局存储、固定列组件和页面接入等可审查提交；未包含 CSV 模板或多使用位业务变更。
