# v0.5.0-rc.1 Field Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a self-contained Windows `0.5.0-rc.1` package with eight polished, real Swing pages, preserved read-only/security semantics, offline preflight, sanitized diagnostics, Chinese operations documents, and reproducible release verification.

**Architecture:** Preserve the existing domain, repositories, MonitoringSession, executors, and sanitization behavior. Incrementally extract Swing theme primitives, application shell, and page layout builders from `ShelfPointMonitorApp`, while callbacks continue to invoke its existing real operations. Add isolated services for CSV export, diagnostics, and offline deployment checks, then extend `build.ps1` to assemble and verify the RC package.

**Tech Stack:** Java 21-compatible source, Java Swing/Java2D, JDBC, local H2, PostgreSQL JDBC driver, PowerShell, Windows batch files, Markdown.

## Global Constraints

- Baseline is `f1443c7132b1c18ba5662d2ef94d58545e7acc29`; do not rewrite or replace its completed semantics.
- Target version is exactly `0.5.0-rc.1` and target ZIP is `dist/ReadonlyBusinessDbTool-v0.5.0-rc.1.zip`.
- Production database paths allow only parameterized `SELECT` and metadata reads.
- Do not add SQL editing, arbitrary identifiers, database writes, Maven, Gradle, third-party UI frameworks, online assets, fonts, or icons.
- Passwords remain in memory-only `char[]` snapshots and are cleared after use.
- UI, logs, diagnostics, and reports must redact credentials, usernames, IPv4, hostnames, JDBC URLs, absolute paths, Java stack frames, and source line locations.
- Tests use only local H2, fake, stub, or mock targets; never connect to a real production database.
- Do not push, create a Pull Request, or modify the frozen legacy directory.
- Every production behavior change follows RED → GREEN → REFACTOR and every task ends with `./build.ps1` plus `git diff --check`.

---

### Task 1: Final P0/P1 Safety Audit and Explicit Guards

**Files:**
- Create: `test/com/local/monitor/SensitiveDataSanitizationTest.java`
- Create: `test/com/local/monitor/ReadOnlyConnectionTest.java`
- Modify only if a failing test proves a gap: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify only if a failing test proves a gap: `src/com/local/monitor/ReadOnlyConnectionFactory.java`
- Modify: `build.ps1`
- Modify: `docs/ai/AUTONOMOUS_EXECUTION_STATE.md`

**Interfaces:**
- Consumes: `ShelfPointMonitorApp.userVisibleErrorMessage(Exception)`, `ShelfPointMonitorApp.sanitizeVisibleLog(String)`, `ReadOnlyConnectionFactory.open(DbConfig,char[])`.
- Produces: named safety regression entry points required by the delivery specification.

- [ ] **Step 1: Write the sanitizer guard test**

Add a `main` test that feeds one synthetic message containing a documentation IPv4 address, dummy username/password, JDBC URL, absolute path, Java frame, and `.java:line` location through both public-facing sanitizers. Assert none of those literals remain and the category `连接超时` remains.

```java
String raw = "连接超时 user=demo password=demo jdbc:postgresql://192.0.2.10:5432/demo "
        + "C:/example/site/config.properties at demo.Driver.run(Driver.java:42)";
String safe = ShelfPointMonitorApp.sanitizeVisibleLog(raw);
TestSupport.assertNotContains(safe, "demo", "username and password must be hidden");
TestSupport.assertContains(safe, "连接超时", "operator category must remain");
```

- [ ] **Step 2: Verify RED for the new test entry**

Run the source/test compile commands from `build.ps1`, then run `com.local.monitor.SensitiveDataSanitizationTest`. Expected: FAIL because the class is not yet compiled into the build script or because a proven sanitizer gap exists.

- [ ] **Step 3: Write the read-only connection guard test**

Use the local H2 configuration, open through `ReadOnlyConnectionFactory`, and assert `Connection.isReadOnly()` is true. Scan production repository SQL constants and assert only `SELECT`/metadata operations occur outside `LocalTestDatabase`.

```java
try (Connection connection = ReadOnlyConnectionFactory.open(config, new char[0])) {
    TestSupport.assertTrue(connection.isReadOnly(), "factory connection must be read-only");
}
```

- [ ] **Step 4: Verify RED for the second named test entry**

Run `java ... com.local.monitor.ReadOnlyConnectionTest`. Expected: FAIL because the new class is not yet wired into `build.ps1` or because a real boundary gap is exposed.

- [ ] **Step 5: Add only the minimal implementation or test wiring**

If existing behavior already passes, add both test invocations to `build.ps1` without changing production code. If a test exposes a gap, make the smallest change in the named production file and retain the existing API.

- [ ] **Step 6: Run complete verification**

Run `./build.ps1` and `git diff --check`. Expected: all existing 21 tests plus the two named safety tests pass; no warnings other than documented line-ending notices.

- [ ] **Step 7: Record and commit the audit**

Update the execution state with each of the 20 audit assertions and evidence. Commit only when the audit is green:

```powershell
git add src test build.ps1 docs/ai/AUTONOMOUS_EXECUTION_STATE.md
git commit -m "fix: close final field stability gaps"
```

---

### Task 2: Theme Tokens and Reusable Swing Components

**Files:**
- Create: `src/com/local/monitor/AppTheme.java`
- Create: `src/com/local/monitor/UiFactory.java`
- Create: `src/com/local/monitor/SectionCard.java`
- Create: `src/com/local/monitor/MetricCard.java`
- Create: `src/com/local/monitor/StatusBadge.java`
- Create: `test/com/local/monitor/AppThemeTest.java`
- Modify: `build.ps1`

**Interfaces:**
- Produces: `AppTheme.install()`, `AppTheme.font(int,float)`, semantic color constants, `UiFactory.primaryButton(String)`, `UiFactory.secondaryButton(String)`, `UiFactory.dangerButton(String)`, `UiFactory.configureTable(JTable)`, `SectionCard(String,String,JComponent)`, `MetricCard(String,JLabel,Color)`, and `StatusBadge.setStatus(String,Color)`.

- [ ] **Step 1: Write failing visual-token tests**

Assert exact required colors, 1440×900 preferred size, 1180×760 minimum size, Microsoft YaHei/SansSerif fallback, 32px standard table row height, and that badges expose both text and a non-null semantic color.

```java
TestSupport.assertEquals(new Color(0x25, 0x63, 0xEB), AppTheme.PRIMARY, "primary blue");
JTable table = new JTable(1, 1);
UiFactory.configureTable(table);
TestSupport.assertEquals(32, table.getRowHeight(), "standard row height");
```

- [ ] **Step 2: Run RED**

Compile and run `AppThemeTest`. Expected: FAIL because theme classes do not exist.

- [ ] **Step 3: Implement immutable theme tokens and UIManager defaults**

`AppTheme.install()` sets fonts, control backgrounds, selection colors, borders, table header colors, disabled colors, and focus colors using only system fonts and Swing defaults.

- [ ] **Step 4: Implement reusable cards, badge, and factories**

Components use opaque white cards, a one-pixel border, 12px radius-like compound padding, and semantic accents. No component performs I/O or business evaluation.

- [ ] **Step 5: Run GREEN and full build**

Run `AppThemeTest`, then `./build.ps1` and `git diff --check`. Expected: all tests pass.

- [ ] **Step 6: Commit**

```powershell
git add src/com/local/monitor/AppTheme.java src/com/local/monitor/UiFactory.java src/com/local/monitor/SectionCard.java src/com/local/monitor/MetricCard.java src/com/local/monitor/StatusBadge.java test/com/local/monitor/AppThemeTest.java build.ps1
git commit -m "refactor: add shared swing theme components"
```

---

### Task 3: Maintainable Application Shell

**Files:**
- Create: `src/com/local/monitor/AppShell.java`
- Create: `src/com/local/monitor/NavigationSidebar.java`
- Create: `src/com/local/monitor/TopStatusBar.java`
- Create: `src/com/local/monitor/BottomStatusBar.java`
- Create: `test/com/local/monitor/AppShellTest.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java:281-555`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`
- Modify: `build.ps1`

**Interfaces:**
- `AppShell(JList<String>, JPanel, JComponent, JComponent)` builds the fixed navigation/status geometry.
- `NavigationSidebar` styles and renders the existing navigation list; it does not own page switching.
- `TopStatusBar` receives the four existing labels.
- `BottomStatusBar` receives the status label and version text.

- [ ] **Step 1: Write failing shell structure tests**

Assert preferred window 1440×900, minimum 1180×760, navigation width 210, top height 56, bottom height 28, eight navigation entries, and no overlapping GridBag cells.

- [ ] **Step 2: Run RED**

Run `AppShellTest`. Expected: FAIL because shell classes do not exist and preferred size is not set.

- [ ] **Step 3: Implement shell components**

Move only layout/styling from `buildUi`, `buildTopStatusBar`, and `buildBottomStatusBar`. Keep selection listeners and page callbacks in `ShelfPointMonitorApp`.

- [ ] **Step 4: Install theme before frame creation**

Call `AppTheme.install()` from the existing look-and-feel path, set preferred and minimum size, and keep window lifecycle/executor shutdown unchanged.

- [ ] **Step 5: Run GREEN and full regression**

Run `AppShellTest`, `ShelfPointMonitorAppUiTest`, `MonitoringSessionRaceTest`, then `./build.ps1` and `git diff --check`.

- [ ] **Step 6: Commit**

```powershell
git add src test build.ps1
git commit -m "refactor: establish maintainable application UI architecture"
```

---

### Task 4: Overview and Alert Center Pages

**Files:**
- Create: `src/com/local/monitor/OverviewPage.java`
- Create: `src/com/local/monitor/AlertCenterPage.java`
- Create: `test/com/local/monitor/OverviewAlertPageTest.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java:558-619,898-956`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`
- Modify: `build.ps1`

**Interfaces:**
- Each page receives existing tables, labels, detail areas, and `Runnable` callbacks; it never receives a password or Repository.
- Overview actions remain `startMonitoring`, `stopMonitoring`, `checkNow`, navigate to alert center, and acknowledge selected alert.
- Alert center operations continue to resolve targets from `AlertCenterEntry.groupId()`.

- [ ] **Step 1: Write failing page structure and semantics tests**

Assert four MetricCards, status table columns, detail card, five real actions, five alert filters, and exact `QUERY_FAILED` text “查询失败，数据不可用”. Assert `QUERY_RECOVERED` and ordinary `NORMAL` do not enter recovered rows.

- [ ] **Step 2: Run RED**

Expected: FAIL because the page classes and exact visual hierarchy do not exist.

- [ ] **Step 3: Extract layouts and apply semantic renderers**

Use `MetricCard`, `SectionCard`, `StatusBadge`, and `UiFactory.configureTable`. Preserve all existing listeners and groupId mapping.

- [ ] **Step 4: Run GREEN and full build**

Run the focused test, `ShelfPointMonitorAppUiTest`, `GroupMonitorLogicTest`, then full build and diff check.

- [ ] **Step 5: Commit**

```powershell
git add src test build.ps1
git commit -m "refactor: extract overview and alert center pages"
```

---

### Task 5: Group Management and Connection Pages

**Files:**
- Create: `src/com/local/monitor/GroupManagementPage.java`
- Create: `src/com/local/monitor/ConnectionManagementPage.java`
- Create: `test/com/local/monitor/GroupConnectionPageTest.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java:620-678,804-897`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`
- Modify: `build.ps1`

**Interfaces:**
- `GroupManagementPage` receives the group selector/tree model, basic fields, rule fields, point table, status board, and existing CRUD/monitor callbacks.
- `ConnectionManagementPage` receives profile components and existing new/save/delete/test/use callbacks.

- [ ] **Step 1: Write failing final-layout tests**

Assert group page positions: left area/group tree, center “基本信息”, right “报警规则”, bottom “点位配置表”. Assert connection page positions: left list, center form, right “连接测试与安全说明”.

- [ ] **Step 2: Write failing behavior-preservation tests**

Invoke each visible button and assert it reaches an injected callback. Keep existing validation tests for unique groupId, enabled use point, threshold, duplicate point code, intervals, and Chinese errors.

- [ ] **Step 3: Run RED**

Expected: FAIL on the final layout classes and callback bindings.

- [ ] **Step 4: Implement page builders**

Use nested horizontal/vertical split panes with explicit resize weights; keep the actual fields and callbacks owned by the app. Replace the left flat group list with a visible area/group tree while preserving groupId as the selection identity.

- [ ] **Step 5: Run GREEN and full regression**

Run focused tests, `GroupConfigStoreTest`, `MonitoringSessionRaceTest`, full build, and diff check.

- [ ] **Step 6: Commit**

```powershell
git add src test build.ps1
git commit -m "refactor: complete group and connection layouts"
```

---

### Task 6: Data Query and Data Source Browser Pages

**Files:**
- Create: `src/com/local/monitor/DataQueryPage.java`
- Create: `src/com/local/monitor/DataSourceBrowserPage.java`
- Create: `src/com/local/monitor/CsvExportService.java`
- Create: `test/com/local/monitor/DataPagesTest.java`
- Create: `test/com/local/monitor/CsvExportServiceTest.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java:679-803`
- Modify: `test/com/local/monitor/PointDataQueryRepositoryTest.java`
- Modify: `test/com/local/monitor/DbMetadataRepositoryTest.java`
- Modify: `build.ps1`

**Interfaces:**
- `CsvExportService.writeUtf8(Path,List<String>,List<List<String>>)` writes a UTF-8 BOM CSV with RFC 4180 escaping and no database access.
- Query page callbacks are execute first page, previous, next, and export current result.
- Browser page callbacks are refresh schemas, load objects, and preview selected object.

- [ ] **Step 1: Write failing CSV tests**

Assert UTF-8 Chinese round-trip, commas/quotes/newlines escaped, formula-like values prefixed safely, and no raw sensitive literal survives when sanitized rows are passed.

- [ ] **Step 2: Run RED**

Expected: FAIL because export service and button do not exist.

- [ ] **Step 3: Write failing page/layout tests**

Assert data query has structured conditions, pagination metadata, export button, no SQL text area, and query-condition edits reset page 1. Assert browser has left object tree, center metadata, right preview, and preview limit text 100.

- [ ] **Step 4: Implement minimal export and extracted layouts**

Export only the current in-memory result model. Do not query on the EDT and do not accept user-supplied identifiers.

- [ ] **Step 5: Verify read-only SQL and preview limits**

Run query and metadata repository tests; assert fixed SELECT/COUNT and maximum preview of 100.

- [ ] **Step 6: Run full build and commit**

```powershell
git add src test build.ps1
git commit -m "feat: finish read-only data pages"
```

---

### Task 7: Logs, Settings, and Sanitized Diagnostics

**Files:**
- Create: `src/com/local/monitor/LogsSystemPage.java`
- Create: `src/com/local/monitor/SystemSettingsPage.java`
- Create: `src/com/local/monitor/SensitiveTextSanitizer.java`
- Create: `src/com/local/monitor/DiagnosticBundleService.java`
- Create: `test/com/local/monitor/DiagnosticBundleServiceTest.java`
- Create: `test/com/local/monitor/LogsSettingsPageTest.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java:957-1060,2939-3267`
- Modify: `test/com/local/monitor/ErrorSanitizationTest.java`
- Modify: `build.ps1`

**Interfaces:**
- `SensitiveTextSanitizer.sanitize(String)` becomes the single package-level sanitizer; existing app methods delegate to it.
- `DiagnosticBundleService.create(Path appRoot, Path outputDir, String version)` returns the created ZIP path and never includes configuration or database files.

- [ ] **Step 1: Write failing diagnostic allowlist tests**

Create synthetic logs containing every forbidden sensitive class. Assert ZIP entries are exactly the allowlist and extracted contents contain categories but no raw sensitive values or absolute paths.

- [ ] **Step 2: Run RED**

Expected: FAIL because the service does not exist and current export is a plain text file.

- [ ] **Step 3: Write failing page tests**

Assert one dynamic health-card set, log limits 1000/1000/200, real filter controls, self-test, refresh, open logs, diagnostic export, and only effective settings plus four fixed safety statements.

- [ ] **Step 4: Extract sanitizer and diagnostic service**

Keep current visible behavior by delegating existing static helpers. Use an explicit entry allowlist, UTF-8, sanitized environment values, and relative entry names.

- [ ] **Step 5: Extract and style both pages**

Callbacks continue to use `ioExecutor`; no file operation runs on `monitorExecutor` or EDT.

- [ ] **Step 6: Run focused concurrency/security tests and full build**

Run diagnostic tests, ErrorSanitizationTest, ExecutorSeparationTest, SystemHealthStatusTest, full build, and diff check.

- [ ] **Step 7: Commit**

```powershell
git add src test build.ps1
git commit -m "feat: add sanitized diagnostics and finish system pages"
```

---

### Task 8: Field Scenario and Path Hardening

**Files:**
- Create: `test/com/local/monitor/FieldDeliveryScenarioTest.java`
- Create: `test/com/local/monitor/WindowsPathPackagingTest.java`
- Modify: existing tests only where a new scenario requires shared fixtures
- Modify: `build.ps1`

**Interfaces:**
- Produces one deterministic test entry covering the 27 named field scenarios and one packaging-path verifier using only local H2 and temporary directories.

- [ ] **Step 1: Add missing scenario tests one behavior at a time**

Map each of the 27 required scenarios to an existing test method; add focused methods only for uncovered configuration damage, CSV, diagnostics, and path behavior.

- [ ] **Step 2: Verify each new test RED**

Run the focused class after each method. Expected failure must be the missing behavior, not fixture setup.

- [ ] **Step 3: Add minimal fixes only where RED proves a gap**

Do not weaken assertions or delete tests.

- [ ] **Step 4: Run all tests and commit**

```powershell
git add src test build.ps1
git commit -m "test: harden field delivery scenarios"
```

---

### Task 9: Offline Deployment Preflight and Windows Scripts

**Files:**
- Create: `src/com/local/monitor/FieldDeploymentPreflight.java`
- Create: `src/com/local/monitor/DiagnosticBundleTool.java`
- Create: `test/com/local/monitor/FieldDeploymentPreflightTest.java`
- Create: `test/com/local/monitor/WindowsLauncherScriptTest.java`
- Modify: `build.ps1`

**Interfaces:**
- `FieldDeploymentPreflight.run(Path appRoot, PrintStream out)` returns `0` for no failures and nonzero otherwise.
- `DiagnosticBundleTool.main(String[])` calls `DiagnosticBundleService` for the packaged root.
- Packaged scripts are `启动工具.bat`, `现场部署检查.bat`, and `生成诊断包.bat`.

- [ ] **Step 1: Write failing preflight tests**

Cover missing runtime/JAR, unwritable directories, password keys, duplicate groupId, missing enabled use point, invalid threshold, and successful local H2 read-only query. Assert preflight never opens a PostgreSQL connection.

- [ ] **Step 2: Run RED**

Expected: FAIL because preflight does not exist.

- [ ] **Step 3: Implement preflight with Chinese result lines**

Every check writes `[通过]`, `[警告]`, or `[失败]`; any failure yields nonzero.

- [ ] **Step 4: Write failing launcher tests**

Inspect scripts for `%~dp0`, embedded runtime preference, system Java fallback, quoted paths, preserved `%ERRORLEVEL%`, Chinese errors, and absence of credentials.

- [ ] **Step 5: Extend packaging**

Create `diagnostics/`, copy three docs, generate three Chinese scripts, require jlink runtime for RC packaging, and add package self-tests.

- [ ] **Step 6: Run full build, preflight, and commit**

```powershell
git add src test build.ps1 .gitignore
git commit -m "build: add offline field deployment tooling"
```

---

### Task 10: Version, Release Metadata, and Operations Documents

**Files:**
- Modify: `VERSION`
- Modify: `CHANGELOG.md`
- Modify: `README.md`
- Create: `docs/releases/v0.5.0-rc.1.md`
- Create: `docs/manuals/现场运维交付手册.md`
- Create: `docs/ops/现场验收清单.md`
- Create: `docs/ops/回滚说明.md`
- Modify: `docs/ai/AUTONOMOUS_EXECUTION_STATE.md`
- Modify: `build.ps1`

**Interfaces:**
- `VERSION` and packaged VERSION are exactly `0.5.0-rc.1`.
- Build copies the three Chinese operation documents to the package root.

- [ ] **Step 1: Write package metadata assertions first**

Extend self-test expectations for version, scripts, runtime, docs, diagnostics directory, and no password keys. Run and verify failure against 0.4.0 package.

- [ ] **Step 2: Update version and release notes**

Document RC scope, read-only boundary, H2 validation, and every “现场待确认” PostgreSQL item without claiming production verification.

- [ ] **Step 3: Write the operations manual**

Include all 22 required topics with exact button/page names used by the application.

- [ ] **Step 4: Write acceptance and rollback documents**

Acceptance is a checkable field worksheet. Rollback is side-by-side extraction and configuration backup/restore; it never alters the database.

- [ ] **Step 5: Run full build and commit**

```powershell
git add VERSION CHANGELOG.md README.md docs build.ps1 test
git commit -m "docs: add field operation and acceptance manuals"
```

---

### Task 11: RC Package Verification and Final Evidence

**Files:**
- Create: `docs/ai/AUTONOMOUS_FINAL_REPORT.md`
- Create: `docs/ai/AUTONOMOUS_FINAL_DIFF.patch`
- Modify: `docs/ai/AUTONOMOUS_EXECUTION_STATE.md`
- Generate ignored artifacts: `dist/ReadonlyBusinessDbTool-v0.5.0-rc.1.zip`, `dist/SHA256SUMS.txt`

**Interfaces:**
- Final report contains all 25 required evidence items.
- Final patch is a binary-capable diff from `f1443c7`, excluding itself and `dist/`.

- [ ] **Step 1: Run final build and complete test suite**

Run `./build.ps1`, `git diff --check`, and all packaged self-tests. Expected: exit 0 and every test PASS.

- [ ] **Step 2: Verify extracted package in Chinese and spaced path**

Extract to a temporary path containing both Chinese characters and spaces. With system Java removed from the subprocess PATH, invoke embedded runtime self-test, offline preflight, and diagnostic tool. Expected: exit 0.

- [ ] **Step 3: Verify all three scripts**

Invoke script self-test/preflight modes without opening an interactive UI. Expected: preserved exit 0 and no administrator or network requirement.

- [ ] **Step 4: Inspect diagnostic ZIP and release ZIP**

Assert exact required entries, no forbidden entries, and zero sensitive-pattern matches after extraction.

- [ ] **Step 5: Generate SHA256SUMS.txt**

Write one line containing the uppercase SHA-256 and `ReadonlyBusinessDbTool-v0.5.0-rc.1.zip`; recompute and compare.

- [ ] **Step 6: Generate report and final diff**

Use `git diff --binary f1443c7..HEAD` plus current uncommitted final evidence, excluding `AUTONOMOUS_FINAL_DIFF.patch` and `dist/`. Normalize the patch to LF and run `git diff --check`.

- [ ] **Step 7: Commit final evidence**

```powershell
git add docs/ai/AUTONOMOUS_EXECUTION_STATE.md docs/ai/AUTONOMOUS_FINAL_REPORT.md docs/ai/AUTONOMOUS_FINAL_DIFF.patch
git commit -m "docs: record autonomous rc delivery evidence"
```

- [ ] **Step 8: Final completion audit**

Re-run build, tests, preflight, path verification, diagnostic scan, ZIP hash, `git status --short`, `git log --oneline f1443c7..HEAD`, and the 25-item final checklist. Do not mark complete if any evidence is missing or indirect.

---

## Plan Self-Review

- Spec coverage: all security invariants, eight pages, CSV export, diagnostics, preflight, path tests, runtime, documents, ZIP, hash, reports, patch, and Git restrictions map to tasks above.
- Placeholder scan: the plan contains no TBD/TODO or deferred implementation.
- Type consistency: page classes receive Swing components and callbacks; repositories and passwords are never passed to layout builders. Diagnostics and preflight have explicit path-based APIs reused by UI, CLI, tests, and packaging.
- Execution choice: inline execution in the current session, because the user explicitly requires autonomous continuation and current instructions do not authorize subagents.
