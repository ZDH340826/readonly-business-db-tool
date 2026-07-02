# v0.4.0 Point Status Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the v0.4.0 point status dashboard so onsite operators can see every monitored point as `有料`, `无料`, `未查到`, or `停用`, with configurable group check intervals and elapsed-time alerting.

**Architecture:** Add small domain/view-model classes for point status and group dashboard snapshots, then update the existing group evaluator to use elapsed time while preserving readonly queries. Keep `ShelfPointMonitorApp` as the Swing shell, but move status mapping, scheduling, and Chinese result text into focused classes so the large UI file only renders state.

**Tech Stack:** Java Swing, Java standard library, JDBC, PostgreSQL driver, H2 local test database, PowerShell build script, existing handwritten Java test harness.

---

## File Structure

Create focused domain/view files:

- `src/com/local/monitor/PointMaterialStatus.java`: enum for `有料`, `无料`, `未查到`, `停用`.
- `src/com/local/monitor/PointStatusView.java`: immutable per-point status view model for dashboard rendering and alert details.
- `src/com/local/monitor/PointStatusMapper.java`: maps configured `GroupMonitorPoint` plus returned `PointRecord` rows into `PointStatusView`.
- `src/com/local/monitor/GroupStatusText.java`: converts internal group status and evaluation fields into Chinese operator-facing text.
- `src/com/local/monitor/GroupCheckPlanner.java`: decides which groups are due based on per-group check interval and runtime state.

Modify existing domain files:

- `src/com/local/monitor/GroupAlertRule.java`: add `backupThresholdParticipates`.
- `src/com/local/monitor/GroupRuntimeState.java`: replace count-only timing with `lastCheckedAt`, `conditionFirstMatchedAt`, elapsed seconds, and acknowledgement state.
- `src/com/local/monitor/GroupEvaluation.java`: add elapsed seconds, alert duration seconds, status text, and point status views.
- `src/com/local/monitor/GroupMonitorLogic.java`: evaluate rules by elapsed time and point statuses.
- `src/com/local/monitor/GroupConfigStore.java`: load/save new rule fields with v0.3.0 compatibility.
- `src/com/local/monitor/PointGroupDefinition.java`: keep current check interval storage, but allow UI to edit it.
- `src/com/local/monitor/ShelfPointMonitorApp.java`: render dashboard, add interval/rule controls, use due-group planner, replace technical runtime text.
- `src/com/local/monitor/GroupLogWriter.java`: keep CSV shape compatible and add elapsed seconds where needed without writing secrets.
- `src/com/local/monitor/LocalTestDatabase.java`: add sample data/scenarios for locked points and missing returned records so local dashboard states cover `无料` and `未查到`.

Modify build/release files:

- `build.ps1`
- `README.md`
- `CHANGELOG.md`
- `VERSION`
- `docs/manuals/point-shortage-alert-user-manual.md`
- Create `docs/releases/v0.4.0.md`

---

### Task 1: Point Status View Model And Mapper

**Files:**
- Create: `src/com/local/monitor/PointMaterialStatus.java`
- Create: `src/com/local/monitor/PointStatusView.java`
- Create: `src/com/local/monitor/PointStatusMapper.java`
- Create: `test/com/local/monitor/PointStatusMapperTest.java`
- Modify: `build.ps1`

- [ ] **Step 1: Write the failing mapper test**

Create `test/com/local/monitor/PointStatusMapperTest.java`:

```java
package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class PointStatusMapperTest {
    public static void main(String[] args) {
        mapsAvailablePoint();
        mapsEmptyPointWhenShelfCodeIsBlank();
        mapsEmptyPointWhenLocked();
        mapsMissingPointWhenRecordIsAbsent();
        mapsDisabledPoint();
        System.out.println("PointStatusMapperTest PASS");
    }

    private static void mapsAvailablePoint() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("use", "USE_POINT_001", "使用位", PointRole.USE, true, 1)),
                List.of(record("USE_POINT_001", "SHELF_USE_001", 1, 0)));
        PointStatusView view = views.get(0);
        TestSupport.assertEquals(PointMaterialStatus.AVAILABLE, view.status(), "available point status");
        TestSupport.assertEquals("有料", view.statusText(), "available point text");
        TestSupport.assertEquals("SHELF_USE_001", view.shelfCode(), "available shelf code");
    }

    private static void mapsEmptyPointWhenShelfCodeIsBlank() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("use", "USE_POINT_001", "使用位", PointRole.USE, true, 1)),
                List.of(record("USE_POINT_001", "", 1, 0)));
        TestSupport.assertEquals(PointMaterialStatus.EMPTY, views.get(0).status(), "blank shelf should be empty");
        TestSupport.assertEquals("无货架", views.get(0).reason(), "blank shelf reason");
    }

    private static void mapsEmptyPointWhenLocked() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2)),
                List.of(record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 1)));
        TestSupport.assertEquals(PointMaterialStatus.EMPTY, views.get(0).status(), "locked point should be empty");
        TestSupport.assertEquals("锁定", views.get(0).reason(), "locked reason");
    }

    private static void mapsMissingPointWhenRecordIsAbsent() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2)),
                List.of());
        TestSupport.assertEquals(PointMaterialStatus.MISSING, views.get(0).status(), "missing record status");
        TestSupport.assertEquals("未查到", views.get(0).statusText(), "missing record text");
    }

    private static void mapsDisabledPoint() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, false, 2)),
                List.of(record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0)));
        TestSupport.assertEquals(PointMaterialStatus.DISABLED, views.get(0).status(), "disabled point status");
        TestSupport.assertEquals("已停用", views.get(0).reason(), "disabled reason");
    }

    private static GroupMonitorPoint point(String id, String code, String alias, PointRole role, boolean enabled, int sortOrder) {
        return new GroupMonitorPoint(id, code, alias, role, enabled, sortOrder);
    }

    private static PointRecord record(String code, String podCode, int status, int lock) {
        return new PointRecord(
                code,
                podCode,
                "1",
                status,
                lock,
                "AREA",
                "NEXT",
                LocalDateTime.of(2026, 7, 3, 6, 0),
                LocalDateTime.of(2026, 7, 3, 6, 0));
    }
}
```

- [ ] **Step 2: Wire the test into `build.ps1`**

Add after `GroupMonitorLogicTest`:

```powershell
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PointStatusMapperTest
if ($LASTEXITCODE -ne 0) { throw "point status mapper tests failed with exit code $LASTEXITCODE" }
```

- [ ] **Step 3: Run the test and verify it fails**

Run:

```powershell
.\build.ps1
```

Expected: compilation fails because `PointStatusMapper`, `PointStatusView`, and `PointMaterialStatus` do not exist.

- [ ] **Step 4: Add `PointMaterialStatus`**

Create `src/com/local/monitor/PointMaterialStatus.java`:

```java
package com.local.monitor;

public enum PointMaterialStatus {
    AVAILABLE("有料"),
    EMPTY("无料"),
    MISSING("未查到"),
    DISABLED("停用");

    private final String displayText;

    PointMaterialStatus(String displayText) {
        this.displayText = displayText;
    }

    public String displayText() {
        return displayText;
    }
}
```

- [ ] **Step 5: Add `PointStatusView`**

Create `src/com/local/monitor/PointStatusView.java`:

```java
package com.local.monitor;

import java.time.LocalDateTime;

public final class PointStatusView {
    private final String pointId;
    private final String pointCode;
    private final String alias;
    private final PointRole role;
    private final boolean enabled;
    private final PointMaterialStatus status;
    private final String shelfCode;
    private final LocalDateTime updatedAt;
    private final String reason;

    public PointStatusView(
            String pointId,
            String pointCode,
            String alias,
            PointRole role,
            boolean enabled,
            PointMaterialStatus status,
            String shelfCode,
            LocalDateTime updatedAt,
            String reason) {
        this.pointId = pointId;
        this.pointCode = pointCode;
        this.alias = alias;
        this.role = role;
        this.enabled = enabled;
        this.status = status;
        this.shelfCode = shelfCode == null ? "" : shelfCode;
        this.updatedAt = updatedAt;
        this.reason = reason == null ? "" : reason;
    }

    public String pointId() {
        return pointId;
    }

    public String pointCode() {
        return pointCode;
    }

    public String alias() {
        return alias;
    }

    public PointRole role() {
        return role;
    }

    public boolean enabled() {
        return enabled;
    }

    public PointMaterialStatus status() {
        return status;
    }

    public String statusText() {
        return status.displayText();
    }

    public String shelfCode() {
        return shelfCode;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public String reason() {
        return reason;
    }

    public boolean available() {
        return status == PointMaterialStatus.AVAILABLE;
    }
}
```

- [ ] **Step 6: Add `PointStatusMapper`**

Create `src/com/local/monitor/PointStatusMapper.java`:

```java
package com.local.monitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PointStatusMapper {
    private PointStatusMapper() {
    }

    public static List<PointStatusView> map(List<GroupMonitorPoint> points, List<PointRecord> records) {
        Map<String, PointRecord> byCode = new LinkedHashMap<>();
        if (records != null) {
            for (PointRecord record : records) {
                byCode.put(record.mapDataCode(), record);
            }
        }

        List<PointStatusView> views = new ArrayList<>();
        for (GroupMonitorPoint point : points) {
            PointRecord record = byCode.get(point.code());
            views.add(mapOne(point, record));
        }
        return views;
    }

    private static PointStatusView mapOne(GroupMonitorPoint point, PointRecord record) {
        if (!point.enabled()) {
            return view(point, null, PointMaterialStatus.DISABLED, "", "已停用");
        }
        if (record == null) {
            return view(point, null, PointMaterialStatus.MISSING, "", "点位未返回");
        }
        if (MonitorLogic.isBlank(record.podCode())) {
            return view(point, record, PointMaterialStatus.EMPTY, "", "无货架");
        }
        if (record.status() != 1) {
            return view(point, record, PointMaterialStatus.EMPTY, record.podCode(), "点位状态异常");
        }
        if (record.indLock() != 0) {
            return view(point, record, PointMaterialStatus.EMPTY, record.podCode(), "锁定");
        }
        return view(point, record, PointMaterialStatus.AVAILABLE, record.podCode(), "正常");
    }

    private static PointStatusView view(
            GroupMonitorPoint point,
            PointRecord record,
            PointMaterialStatus status,
            String shelfCode,
            String reason) {
        return new PointStatusView(
                point.id(),
                point.code(),
                point.alias(),
                point.role(),
                point.enabled(),
                status,
                shelfCode,
                record == null ? null : record.dateChg(),
                reason);
    }
}
```

- [ ] **Step 7: Run build and verify mapper tests pass**

Run:

```powershell
.\build.ps1
```

Expected: `PointStatusMapperTest PASS` and existing tests pass.

- [ ] **Step 8: Commit**

```powershell
git add build.ps1 src/com/local/monitor/PointMaterialStatus.java src/com/local/monitor/PointStatusView.java src/com/local/monitor/PointStatusMapper.java test/com/local/monitor/PointStatusMapperTest.java
git commit -m "Add point material status mapper"
```

---

### Task 2: Group Rule And Config Compatibility

**Files:**
- Modify: `src/com/local/monitor/GroupAlertRule.java`
- Modify: `src/com/local/monitor/GroupConfigStore.java`
- Modify: `test/com/local/monitor/GroupConfigStoreTest.java`

- [ ] **Step 1: Add failing config tests**

In `test/com/local/monitor/GroupConfigStoreTest.java`, add calls from `main`:

```java
loadsOldConfigWithBackupThresholdParticipationEnabled();
savesAndLoadsBackupThresholdParticipation();
```

Add test methods:

```java
private static void loadsOldConfigWithBackupThresholdParticipationEnabled() throws Exception {
    Path configPath = Files.createTempFile("old-group-config", ".properties");
    Files.writeString(configPath, String.join(System.lineSeparator(),
            "group.count=1",
            "group.0.id=old-group",
            "group.0.areaName=区域A",
            "group.0.groupName=后围板组",
            "group.0.materialName=后围板总成",
            "group.0.enabled=true",
            "group.0.checkIntervalSeconds=60",
            "group.0.rule.enabled=true",
            "group.0.rule.requireUsePointEmpty=true",
            "group.0.rule.minBackupAvailable=1",
            "group.0.rule.durationMinutes=5",
            "group.0.point.count=2",
            "group.0.point.0.id=use",
            "group.0.point.0.code=USE_POINT_001",
            "group.0.point.0.alias=使用位",
            "group.0.point.0.role=USE",
            "group.0.point.0.enabled=true",
            "group.0.point.0.sortOrder=1",
            "group.0.point.1.id=backup",
            "group.0.point.1.code=BACKUP_POINT_001",
            "group.0.point.1.alias=备用位1",
            "group.0.point.1.role=BACKUP",
            "group.0.point.1.enabled=true",
            "group.0.point.1.sortOrder=2",
            ""));

    List<PointGroupDefinition> loaded = new GroupConfigStore(configPath).load();
    TestSupport.assertTrue(loaded.get(0).rule().backupThresholdParticipates(),
            "old config should default backup threshold participation to true");
}

private static void savesAndLoadsBackupThresholdParticipation() throws Exception {
    Path configPath = Files.createTempFile("new-group-config", ".properties");
    PointGroupDefinition group = group("group-v040", 1, 5, false);
    GroupConfigStore store = new GroupConfigStore(configPath);
    store.save(List.of(group));

    List<PointGroupDefinition> loaded = store.load();
    TestSupport.assertTrue(!loaded.get(0).rule().backupThresholdParticipates(),
            "new config should persist disabled backup threshold participation");
}
```

Update the existing `group` helper signature:

```java
private static PointGroupDefinition group(
        String id,
        int minBackupAvailable,
        int durationMinutes,
        boolean backupThresholdParticipates) {
    return new PointGroupDefinition(
            id,
            "区域A",
            "后围板组",
            "后围板总成",
            true,
            PointGroupDefinition.DEFAULT_CHECK_INTERVAL_SECONDS,
            List.of(
                    new GroupMonitorPoint(id + "-use", "USE_POINT_001", "使用位", PointRole.USE, true, 1),
                    new GroupMonitorPoint(id + "-backup-1", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2),
                    new GroupMonitorPoint(id + "-backup-2", "BACKUP_POINT_002", "备用位2", PointRole.BACKUP, true, 3),
                    new GroupMonitorPoint(id + "-backup-3", "BACKUP_POINT_003", "备用位3", PointRole.BACKUP, true, 4),
                    new GroupMonitorPoint(id + "-backup-4", "BACKUP_POINT_004", "备用位4", PointRole.BACKUP, true, 5)),
            new GroupAlertRule(true, true, minBackupAvailable, durationMinutes, backupThresholdParticipates));
}
```

For existing test calls that use the old helper, pass `true`.

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
.\build.ps1
```

Expected: compilation fails because `backupThresholdParticipates()` and the five-argument constructor do not exist.

- [ ] **Step 3: Extend `GroupAlertRule`**

Modify `src/com/local/monitor/GroupAlertRule.java`:

```java
public final class GroupAlertRule {
    public static final int MIN_DURATION_MINUTES = 1;
    public static final int MAX_DURATION_MINUTES = 1440;

    private final boolean enabled;
    private final boolean requireUsePointEmpty;
    private final int minBackupAvailable;
    private final int durationMinutes;
    private final boolean backupThresholdParticipates;

    public GroupAlertRule(boolean enabled, boolean requireUsePointEmpty, int minBackupAvailable, int durationMinutes) {
        this(enabled, requireUsePointEmpty, minBackupAvailable, durationMinutes, true);
    }

    public GroupAlertRule(
            boolean enabled,
            boolean requireUsePointEmpty,
            int minBackupAvailable,
            int durationMinutes,
            boolean backupThresholdParticipates) {
        if (minBackupAvailable < 0) {
            throw new IllegalArgumentException("minBackupAvailable must be >= 0");
        }
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("durationMinutes must be between 1 and 1440");
        }
        this.enabled = enabled;
        this.requireUsePointEmpty = requireUsePointEmpty;
        this.minBackupAvailable = minBackupAvailable;
        this.durationMinutes = durationMinutes;
        this.backupThresholdParticipates = backupThresholdParticipates;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean requireUsePointEmpty() {
        return requireUsePointEmpty;
    }

    public int minBackupAvailable() {
        return minBackupAvailable;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public boolean backupThresholdParticipates() {
        return backupThresholdParticipates;
    }

    public int durationSeconds() {
        return durationMinutes * 60;
    }
}
```

- [ ] **Step 4: Load and save the new property**

In `src/com/local/monitor/GroupConfigStore.java`, update `loadGroup` rule creation:

```java
GroupAlertRule rule = new GroupAlertRule(
        parseBoolean(p.getProperty(prefix + "rule.enabled"), true),
        parseBoolean(p.getProperty(prefix + "rule.requireUsePointEmpty"), true),
        parseInt(p.getProperty(prefix + "rule.minBackupAvailable"), 1),
        parseInt(p.getProperty(prefix + "rule.durationMinutes"), 5),
        parseBoolean(p.getProperty(prefix + "rule.backupThresholdParticipates"), true));
```

Update `storeGroup`:

```java
p.setProperty(prefix + "rule.backupThresholdParticipates",
        String.valueOf(group.rule().backupThresholdParticipates()));
```

- [ ] **Step 5: Run build and verify config tests pass**

Run:

```powershell
.\build.ps1
```

Expected: `GroupConfigStoreTest PASS` and all previous tests pass.

- [ ] **Step 6: Commit**

```powershell
git add src/com/local/monitor/GroupAlertRule.java src/com/local/monitor/GroupConfigStore.java test/com/local/monitor/GroupConfigStoreTest.java
git commit -m "Add backup threshold rule compatibility"
```

---

### Task 3: Elapsed-Time Group Evaluation

**Files:**
- Modify: `src/com/local/monitor/GroupRuntimeState.java`
- Modify: `src/com/local/monitor/GroupEvaluation.java`
- Modify: `src/com/local/monitor/GroupMonitorLogic.java`
- Modify: `test/com/local/monitor/GroupMonitorLogicTest.java`
- Modify: `test/com/local/monitor/GroupLogWriterTest.java`

- [ ] **Step 1: Add failing elapsed-time tests**

In `test/com/local/monitor/GroupMonitorLogicTest.java`, add:

```java
private static void elapsedTimeControlsPendingAndActiveAlert() {
    PointGroupDefinition group = group(1, 5, false);
    GroupRuntimeState state = new GroupRuntimeState();
    LocalDateTime start = LocalDateTime.of(2026, 7, 3, 6, 0);

    GroupEvaluation first = GroupMonitorLogic.evaluate(group, shortageRecords(), state, start);
    TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, first.status(), "first abnormal check is pending");
    TestSupport.assertEquals(0, first.continuousMatchedSeconds(), "first continuous seconds");

    GroupEvaluation afterFour = GroupMonitorLogic.evaluate(group, shortageRecords(), state, start.plusMinutes(4));
    TestSupport.assertEquals(GroupAlertStatus.PENDING_ALERT, afterFour.status(), "four minutes is still pending");
    TestSupport.assertEquals(240, afterFour.continuousMatchedSeconds(), "four minutes in seconds");

    GroupEvaluation afterFive = GroupMonitorLogic.evaluate(group, shortageRecords(), state, start.plusMinutes(5));
    TestSupport.assertEquals(GroupAlertStatus.ACTIVE_ALERT, afterFive.status(), "five minutes should alert");
    TestSupport.assertTrue(afterFive.shouldShowDialog(), "first active evaluation should request dialog");
}

private static void recoveryClearsElapsedTimeAndAcknowledgement() {
    PointGroupDefinition group = group(1, 5, false);
    GroupRuntimeState state = new GroupRuntimeState();
    LocalDateTime start = LocalDateTime.of(2026, 7, 3, 6, 0);

    GroupEvaluation active = GroupMonitorLogic.evaluate(group, shortageRecords(), state, start.plusMinutes(5));
    state.acknowledge();
    GroupEvaluation recovered = GroupMonitorLogic.evaluate(group, healthyRecords(), state, start.plusMinutes(6));

    TestSupport.assertEquals(GroupAlertStatus.NORMAL, recovered.status(), "recovered group should be normal");
    TestSupport.assertEquals(0, recovered.continuousMatchedSeconds(), "recovery clears elapsed seconds");
    TestSupport.assertTrue(!state.isAcknowledged(), "recovery clears acknowledgement");
}
```

Add helper:

```java
private static List<PointRecord> healthyRecords() {
    return List.of(
            record("USE_POINT_001", "SHELF_USE_001", 1, 0),
            record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0),
            record("BACKUP_POINT_002", "SHELF_BACKUP_002", 1, 0),
            record("BACKUP_POINT_003", "SHELF_BACKUP_003", 1, 0),
            record("BACKUP_POINT_004", "SHELF_BACKUP_004", 1, 0));
}
```

Call both tests from `main`.

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
.\build.ps1
```

Expected: compilation fails because `evaluate(..., LocalDateTime)` and `continuousMatchedSeconds()` do not exist.

- [ ] **Step 3: Replace count-only runtime state**

Modify `src/com/local/monitor/GroupRuntimeState.java`:

```java
package com.local.monitor;

import java.time.Duration;
import java.time.LocalDateTime;

public final class GroupRuntimeState {
    private LocalDateTime lastCheckedAt;
    private LocalDateTime conditionFirstMatchedAt;
    private LocalDateTime conditionLastMatchedAt;
    private boolean acknowledged;
    private boolean activeDialogShown;

    public LocalDateTime lastCheckedAt() {
        return lastCheckedAt;
    }

    public void markChecked(LocalDateTime checkedAt) {
        lastCheckedAt = checkedAt;
    }

    public LocalDateTime conditionFirstMatchedAt() {
        return conditionFirstMatchedAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    boolean activeDialogShown() {
        return activeDialogShown;
    }

    public void acknowledge() {
        acknowledged = true;
        activeDialogShown = true;
    }

    void markMatched(LocalDateTime now) {
        if (conditionFirstMatchedAt == null) {
            conditionFirstMatchedAt = now;
        }
        conditionLastMatchedAt = now;
        lastCheckedAt = now;
    }

    int continuousMatchedSeconds(LocalDateTime now) {
        if (conditionFirstMatchedAt == null) {
            return 0;
        }
        long seconds = Duration.between(conditionFirstMatchedAt, now).getSeconds();
        return (int) Math.max(0, seconds);
    }

    int continuousMatchedMinutes(LocalDateTime now) {
        int seconds = continuousMatchedSeconds(now);
        return seconds == 0 ? 0 : (int) Math.ceil(seconds / 60.0);
    }

    void markActiveDialogShown() {
        activeDialogShown = true;
    }

    void reset() {
        conditionFirstMatchedAt = null;
        conditionLastMatchedAt = null;
        acknowledged = false;
        activeDialogShown = false;
    }
}
```

- [ ] **Step 4: Extend `GroupEvaluation`**

Add fields and constructor parameters:

```java
private final int continuousMatchedSeconds;
private final int alertDurationSeconds;
private final List<PointStatusView> pointStatuses;
```

Add accessors:

```java
public int continuousMatchedSeconds() {
    return continuousMatchedSeconds;
}

public int alertDurationSeconds() {
    return alertDurationSeconds;
}

public List<PointStatusView> pointStatuses() {
    return pointStatuses;
}
```

In the constructor, wrap point statuses:

```java
this.pointStatuses = Collections.unmodifiableList(new ArrayList<>(pointStatuses));
```

Keep `continuousMatchedMinutes()` for compatibility, but have callers pass the elapsed-minute value computed from seconds.

Update `GroupLogWriterTest.evaluation(...)` to pass:

```java
0,
300,
List.of(),
"message"
```

for the new parameters.

- [ ] **Step 5: Update `GroupMonitorLogic` to accept `now`**

Add overload:

```java
public static GroupEvaluation evaluate(
        PointGroupDefinition group,
        List<PointRecord> records,
        GroupRuntimeState state) {
    return evaluate(group, records, state, java.time.LocalDateTime.now());
}
```

Update main evaluation method:

```java
public static GroupEvaluation evaluate(
        PointGroupDefinition group,
        List<PointRecord> records,
        GroupRuntimeState state,
        LocalDateTime now) {
    if (group == null) {
        throw new IllegalArgumentException("group is required");
    }
    if (state == null) {
        throw new IllegalArgumentException("runtime state is required");
    }

    List<PointStatusView> pointStatuses = PointStatusMapper.map(group.points(), records);
    boolean usePointEmpty = true;
    int backupTotal = 0;
    int backupAvailable = 0;

    for (PointStatusView point : pointStatuses) {
        if (!point.enabled()) {
            continue;
        }
        boolean available = point.status() == PointMaterialStatus.AVAILABLE;
        if (point.role() == PointRole.USE) {
            usePointEmpty = !available;
        } else if (point.role() == PointRole.BACKUP) {
            backupTotal++;
            if (available) {
                backupAvailable++;
            }
        }
    }

    GroupAlertRule rule = group.rule();
    boolean useCondition = !rule.requireUsePointEmpty() || usePointEmpty;
    boolean backupCondition = !rule.backupThresholdParticipates()
            || backupAvailable < rule.minBackupAvailable();
    boolean ruleMatched = group.enabled() && rule.enabled() && useCondition && backupCondition;

    GroupAlertStatus status;
    boolean shouldShowDialog = false;
    int continuousSeconds = 0;
    int continuousMinutes = 0;

    if (!ruleMatched) {
        state.reset();
        state.markChecked(now);
        status = GroupAlertStatus.NORMAL;
    } else {
        state.markMatched(now);
        continuousSeconds = state.continuousMatchedSeconds(now);
        continuousMinutes = state.continuousMatchedMinutes(now);
        if (continuousSeconds < rule.durationSeconds()) {
            status = GroupAlertStatus.PENDING_ALERT;
        } else if (state.isAcknowledged()) {
            status = GroupAlertStatus.ACKED_ALERT;
        } else {
            status = GroupAlertStatus.ACTIVE_ALERT;
            shouldShowDialog = !state.activeDialogShown();
            if (shouldShowDialog) {
                state.markActiveDialogShown();
            }
        }
    }

    return new GroupEvaluation(
            group.id(),
            group.areaName(),
            group.groupName(),
            group.materialName(),
            status,
            usePointEmpty,
            backupTotal,
            backupAvailable,
            backupTotal - backupAvailable,
            ruleMatched,
            continuousMinutes,
            continuousSeconds,
            rule.durationSeconds(),
            shouldShowDialog,
            pointStatuses,
            message(group, usePointEmpty, backupTotal, backupAvailable, continuousSeconds, rule));
}
```

Update `message(...)` to produce Chinese:

```java
private static String message(
        PointGroupDefinition group,
        boolean usePointEmpty,
        int backupTotal,
        int backupAvailable,
        int continuousSeconds,
        GroupAlertRule rule) {
    return GroupStatusText.summary(
            group.areaName(),
            group.groupName(),
            usePointEmpty,
            backupAvailable,
            backupTotal,
            continuousSeconds,
            rule.durationSeconds());
}
```

- [ ] **Step 6: Add `GroupStatusText`**

Create `src/com/local/monitor/GroupStatusText.java`:

```java
package com.local.monitor;

public final class GroupStatusText {
    private GroupStatusText() {
    }

    public static String statusText(GroupAlertStatus status) {
        if (status == GroupAlertStatus.NORMAL) {
            return "正常";
        }
        if (status == GroupAlertStatus.PENDING_ALERT) {
            return "观察中";
        }
        if (status == GroupAlertStatus.ACTIVE_ALERT) {
            return "需关注";
        }
        if (status == GroupAlertStatus.ACKED_ALERT) {
            return "已关注";
        }
        return status.name();
    }

    public static String summary(
            String areaName,
            String groupName,
            boolean usePointEmpty,
            int backupAvailable,
            int backupTotal,
            int continuousSeconds,
            int alertDurationSeconds) {
        if (!usePointEmpty) {
            return "正常：使用位有料，备用位 " + backupAvailable + "/" + backupTotal + " 有料。";
        }
        return "观察中：使用位无料，已持续 "
                + minutesText(continuousSeconds)
                + "/"
                + minutesText(alertDurationSeconds)
                + " 分钟，备用位 "
                + backupAvailable
                + "/"
                + backupTotal
                + " 有料。";
    }

    public static String minutesText(int seconds) {
        int minutes = seconds / 60;
        if (seconds > 0 && seconds % 60 != 0) {
            minutes++;
        }
        return String.valueOf(minutes);
    }
}
```

- [ ] **Step 7: Run build and update broken tests deliberately**

Run:

```powershell
.\build.ps1
```

Expected: existing count-based tests in `GroupMonitorLogicTest` fail. Update their assertions to use explicit `LocalDateTime` calls and elapsed-time expectations from Step 1. Preserve tests for acknowledgement and recovery.

- [ ] **Step 8: Run build and verify all tests pass**

Run:

```powershell
.\build.ps1
```

Expected: all tests pass.

- [ ] **Step 9: Commit**

```powershell
git add src/com/local/monitor/GroupRuntimeState.java src/com/local/monitor/GroupEvaluation.java src/com/local/monitor/GroupMonitorLogic.java src/com/local/monitor/GroupStatusText.java test/com/local/monitor/GroupMonitorLogicTest.java test/com/local/monitor/GroupLogWriterTest.java
git commit -m "Use elapsed time for group alerts"
```

---

### Task 4: Per-Group Due Scheduling

**Files:**
- Create: `src/com/local/monitor/GroupCheckPlanner.java`
- Create: `test/com/local/monitor/GroupCheckPlannerTest.java`
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `build.ps1`

- [ ] **Step 1: Add failing scheduler tests**

Create `test/com/local/monitor/GroupCheckPlannerTest.java`:

```java
package com.local.monitor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GroupCheckPlannerTest {
    public static void main(String[] args) {
        uncheckedGroupsAreDue();
        groupsAreNotDueBeforeTheirInterval();
        groupsAreDueAfterTheirInterval();
        System.out.println("GroupCheckPlannerTest PASS");
    }

    private static void uncheckedGroupsAreDue() {
        PointGroupDefinition group = group("g1", 60);
        List<PointGroupDefinition> due = GroupCheckPlanner.dueGroups(
                List.of(group),
                new LinkedHashMap<>(),
                LocalDateTime.of(2026, 7, 3, 6, 0));
        TestSupport.assertEquals(1, due.size(), "unchecked group should be due");
    }

    private static void groupsAreNotDueBeforeTheirInterval() {
        PointGroupDefinition group = group("g1", 300);
        GroupRuntimeState state = new GroupRuntimeState();
        state.markChecked(LocalDateTime.of(2026, 7, 3, 6, 0));
        Map<String, GroupRuntimeState> states = new LinkedHashMap<>();
        states.put("g1", state);

        List<PointGroupDefinition> due = GroupCheckPlanner.dueGroups(
                List.of(group),
                states,
                LocalDateTime.of(2026, 7, 3, 6, 4));
        TestSupport.assertEquals(0, due.size(), "group should not be due before interval");
    }

    private static void groupsAreDueAfterTheirInterval() {
        PointGroupDefinition group = group("g1", 300);
        GroupRuntimeState state = new GroupRuntimeState();
        state.markChecked(LocalDateTime.of(2026, 7, 3, 6, 0));
        Map<String, GroupRuntimeState> states = new LinkedHashMap<>();
        states.put("g1", state);

        List<PointGroupDefinition> due = GroupCheckPlanner.dueGroups(
                List.of(group),
                states,
                LocalDateTime.of(2026, 7, 3, 6, 5));
        TestSupport.assertEquals(1, due.size(), "group should be due at interval boundary");
    }

    private static PointGroupDefinition group(String id, int intervalSeconds) {
        return new PointGroupDefinition(
                id,
                "区域A",
                "后围板组",
                "后围板总成",
                true,
                intervalSeconds,
                List.of(
                        new GroupMonitorPoint(id + "-use", "USE_POINT_001", "使用位", PointRole.USE, true, 1),
                        new GroupMonitorPoint(id + "-backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2)),
                new GroupAlertRule(true, true, 1, 5));
    }
}
```

- [ ] **Step 2: Add the test to `build.ps1`**

Add after `PointScheduleTest`:

```powershell
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.GroupCheckPlannerTest
if ($LASTEXITCODE -ne 0) { throw "group check planner tests failed with exit code $LASTEXITCODE" }
```

- [ ] **Step 3: Run test and verify it fails**

Run:

```powershell
.\build.ps1
```

Expected: compilation fails because `GroupCheckPlanner` does not exist.

- [ ] **Step 4: Create `GroupCheckPlanner`**

Create `src/com/local/monitor/GroupCheckPlanner.java`:

```java
package com.local.monitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GroupCheckPlanner {
    private GroupCheckPlanner() {
    }

    public static List<PointGroupDefinition> dueGroups(
            List<PointGroupDefinition> groups,
            Map<String, GroupRuntimeState> states,
            LocalDateTime now) {
        List<PointGroupDefinition> due = new ArrayList<>();
        for (PointGroupDefinition group : groups) {
            if (!group.enabled()) {
                continue;
            }
            GroupRuntimeState state = states.get(group.id());
            if (state == null || state.lastCheckedAt() == null) {
                due.add(group);
                continue;
            }
            long elapsed = Duration.between(state.lastCheckedAt(), now).getSeconds();
            if (elapsed >= group.checkIntervalSeconds()) {
                due.add(group);
            }
        }
        return due;
    }
}
```

- [ ] **Step 5: Use due planning in automatic monitoring**

In `ShelfPointMonitorApp.checkDueGroups()`, replace:

```java
List<PointGroupDefinition> groups = readGroups();
checkGroups(config, groups, LocalDateTime.now(), "自动检测");
```

with:

```java
List<PointGroupDefinition> allGroups = readGroups();
LocalDateTime now = LocalDateTime.now();
List<PointGroupDefinition> dueGroups = GroupCheckPlanner.dueGroups(allGroups, groupStates, now);
if (dueGroups.isEmpty()) {
    return;
}
checkGroups(config, dueGroups, now, "自动检测");
```

In `startMonitoring()`, keep the background tick short and stable:

```java
scheduledTask = executor.scheduleWithFixedDelay(
        () -> runWithUiErrorHandling(this::checkDueGroups),
        0,
        10,
        TimeUnit.SECONDS);
appendStatus("已开始点位组监控。系统每 10 秒检查到期点位组，各组按自己的检测周期查询数据库。");
```

- [ ] **Step 6: Run build**

Run:

```powershell
.\build.ps1
```

Expected: `GroupCheckPlannerTest PASS` and existing tests pass.

- [ ] **Step 7: Commit**

```powershell
git add build.ps1 src/com/local/monitor/GroupCheckPlanner.java test/com/local/monitor/GroupCheckPlannerTest.java src/com/local/monitor/ShelfPointMonitorApp.java
git commit -m "Schedule groups by check interval"
```

---

### Task 5: Dashboard UI Rendering

**Files:**
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

- [ ] **Step 1: Add failing UI smoke assertions**

In `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`, add test call:

```java
alertPageShowsPointStatusDashboardLanguage();
```

Add method:

```java
private static void alertPageShowsPointStatusDashboardLanguage() throws Exception {
    runOnEdtAndWait(() -> {
        ShelfPointMonitorApp app = new ShelfPointMonitorApp();
        try {
            Set<String> texts = collectVisibleTexts(app.getContentPane());
            TestSupport.assertTrue(texts.contains("检测周期(分钟)："), "dashboard should show group check interval");
            TestSupport.assertTrue(texts.contains("报警持续(分钟)："), "dashboard should show alert duration");
            TestSupport.assertTrue(texts.contains("备用位下限参与报警"), "dashboard should allow backup threshold participation");
            TestSupport.assertTrue(texts.contains("点位状态看板"), "dashboard should have status board title");
            TestSupport.assertTrue(texts.contains("当前判断"), "dashboard should show business judgement area");
        } finally {
            app.dispose();
        }
    });
}
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```powershell
.\build.ps1
```

Expected: `ShelfPointMonitorAppUiTest` fails because new dashboard text is absent.

- [ ] **Step 3: Add UI fields**

In `ShelfPointMonitorApp`, add fields near existing group controls:

```java
private final JSpinner groupCheckIntervalMinutesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1440, 1));
private final JCheckBox backupThresholdParticipatesBox = new JCheckBox("备用位下限参与报警", true);
private final JPanel pointStatusPanel = new JPanel(new GridBagLayout());
private final JLabel groupSummaryLabel = new JLabel("当前判断：未检测");
```

- [ ] **Step 4: Update group detail form**

In `buildGroupDetailForm()`, replace row 376-378 with:

```java
addField(form, row, 0, "检测周期(分钟)", groupCheckIntervalMinutesSpinner);
addField(form, row, 2, "报警持续(分钟)", durationMinutesSpinner);
addCheckBox(form, row, 4, requireUseEmptyBox);
row++;
addField(form, row, 0, "最少备用位有料", minBackupAvailableSpinner);
addCheckBox(form, row, 2, backupThresholdParticipatesBox);
```

- [ ] **Step 5: Replace runtime text with status board container**

In `buildAlertPage()`, replace the `groupRuntimeArea` center/south-heavy rendering with:

```java
JPanel statusBoard = new JPanel(new BorderLayout(8, 8));
statusBoard.setBorder(javax.swing.BorderFactory.createTitledBorder("点位状态看板"));
pointStatusPanel.setBackground(Color.WHITE);
statusBoard.add(groupSummaryLabel, BorderLayout.NORTH);
statusBoard.add(new JScrollPane(pointStatusPanel), BorderLayout.CENTER);
groupRuntimeArea.setEditable(false);
groupRuntimeArea.setRows(5);
statusBoard.add(new JScrollPane(groupRuntimeArea), BorderLayout.SOUTH);
detail.add(statusBoard, BorderLayout.CENTER);
```

Keep the point configuration table in a titled panel below the rule form or as the upper part of a vertical split. Do not remove editability of existing group point configuration.

- [ ] **Step 6: Load and save new UI fields**

In `populateSelectedGroup()`:

```java
groupCheckIntervalMinutesSpinner.setValue(Math.max(1, group.checkIntervalSeconds() / 60));
backupThresholdParticipatesBox.setSelected(group.rule().backupThresholdParticipates());
```

In `updateSelectedGroupFromForm()`, replace check interval:

```java
((Integer) groupCheckIntervalMinutesSpinner.getValue()) * 60,
```

and replace rule constructor:

```java
new GroupAlertRule(
        ruleEnabledBox.isSelected(),
        requireUseEmptyBox.isSelected(),
        (Integer) minBackupAvailableSpinner.getValue(),
        (Integer) durationMinutesSpinner.getValue(),
        backupThresholdParticipatesBox.isSelected())
```

- [ ] **Step 7: Render point status cards after each check**

Add method:

```java
private void renderPointStatusBoard(GroupEvaluation evaluation) {
    pointStatusPanel.removeAll();
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(6, 6, 6, 6);
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 0.0;

    int row = 0;
    int col = 0;
    for (PointStatusView point : evaluation.pointStatuses()) {
        JPanel card = pointStatusCard(point);
        if (point.role() == PointRole.USE) {
            c.gridx = 0;
            c.gridy = row++;
            c.gridwidth = 4;
            pointStatusPanel.add(card, c);
            c.gridwidth = 1;
            col = 0;
        } else {
            c.gridx = col;
            c.gridy = row;
            pointStatusPanel.add(card, c);
            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }
    pointStatusPanel.revalidate();
    pointStatusPanel.repaint();
}
```

Add card method:

```java
private JPanel pointStatusCard(PointStatusView point) {
    JPanel card = new JPanel(new GridLayout(0, 1, 4, 4));
    card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(statusColor(point.status()), 2),
            javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    card.setBackground(new Color(255, 255, 255));
    JLabel title = new JLabel((point.role() == PointRole.USE ? "使用位：" : "备用位：") + point.alias());
    title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
    JLabel status = new JLabel(point.statusText());
    status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, point.role() == PointRole.USE ? 26 : 22));
    status.setForeground(statusColor(point.status()));
    card.add(title);
    card.add(status);
    card.add(new JLabel("点位：" + point.pointCode()));
    card.add(new JLabel("货架：" + (point.shelfCode().isEmpty() ? "--" : point.shelfCode())));
    card.add(new JLabel("原因：" + point.reason()));
    return card;
}
```

Add color method:

```java
private Color statusColor(PointMaterialStatus status) {
    if (status == PointMaterialStatus.AVAILABLE) {
        return new Color(24, 128, 72);
    }
    if (status == PointMaterialStatus.EMPTY) {
        return new Color(190, 48, 48);
    }
    if (status == PointMaterialStatus.MISSING) {
        return new Color(112, 112, 112);
    }
    return new Color(150, 150, 150);
}
```

In `checkGroups(...)`, after each evaluation, update selected group board:

```java
if (group.id().equals(selectedGroupId())) {
    SwingUtilities.invokeLater(() -> {
        groupSummaryLabel.setText("当前判断：" + evaluation.message());
        renderPointStatusBoard(evaluation);
    });
}
```

Add:

```java
private String selectedGroupId() {
    int index = groupList.getSelectedIndex();
    if (index < 0 || index >= pointGroups.size()) {
        return "";
    }
    return pointGroups.get(index).id();
}
```

- [ ] **Step 8: Replace technical runtime line**

In `formatGroupCheckResult(...)`, return:

```java
return TIME_FORMAT.format(LocalDateTime.now())
        + " "
        + GroupStatusText.statusText(evaluation.status())
        + "："
        + evaluation.message();
```

Do not show `status=`, `useEmpty=`, or `backup=` on the main screen.

- [ ] **Step 9: Run build and verify UI smoke tests pass**

Run:

```powershell
.\build.ps1
```

Expected: `ShelfPointMonitorAppUiTest PASS` and all tests pass.

- [ ] **Step 10: Commit**

```powershell
git add src/com/local/monitor/ShelfPointMonitorApp.java test/com/local/monitor/ShelfPointMonitorAppUiTest.java
git commit -m "Render point status dashboard"
```

---

### Task 6: Alert Dialog And Operator Text

**Files:**
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `src/com/local/monitor/GroupStatusText.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

- [ ] **Step 1: Add text tests for no raw alert codes**

In `ShelfPointMonitorAppUiTest`, add a helper test:

```java
private static void groupStatusTextUsesChineseBusinessLanguage() {
    String text = GroupStatusText.statusText(GroupAlertStatus.PENDING_ALERT);
    TestSupport.assertEquals("观察中", text, "pending should be operator-facing");
    TestSupport.assertTrue(!GroupStatusText.summary("区域", "后围板", true, 2, 4, 180, 300).contains("useEmpty"),
            "summary should not expose raw fields");
}
```

Call it from `main`.

- [ ] **Step 2: Run test**

Run:

```powershell
.\build.ps1
```

Expected: passes if Task 3 text exists. If it fails, update `GroupStatusText` before continuing.

- [ ] **Step 3: Improve alert dialog text**

Replace `groupAlertText(GroupEvaluation evaluation)` body:

```java
return "检测时间：" + TIME_FORMAT.format(LocalDateTime.now())
        + System.lineSeparator()
        + evaluation.areaName() + " / " + evaluation.groupName() + " 需要关注"
        + System.lineSeparator()
        + "物料：" + evaluation.materialName()
        + System.lineSeparator()
        + "使用位：" + (evaluation.usePointEmpty() ? "无料" : "有料")
        + System.lineSeparator()
        + "备用位：" + evaluation.backupAvailableCount() + "/" + evaluation.backupTotal() + " 有料"
        + System.lineSeparator()
        + "持续：" + GroupStatusText.minutesText(evaluation.continuousMatchedSeconds()) + " 分钟"
        + System.lineSeparator()
        + System.lineSeparator()
        + abnormalPointText(evaluation)
        + System.lineSeparator()
        + "使用位无料已达到报警时间，请现场确认补料或调度状态。";
```

Add:

```java
private String abnormalPointText(GroupEvaluation evaluation) {
    StringBuilder text = new StringBuilder("异常点位：");
    boolean found = false;
    for (PointStatusView point : evaluation.pointStatuses()) {
        if (point.status() == PointMaterialStatus.EMPTY || point.status() == PointMaterialStatus.MISSING) {
            text.append(System.lineSeparator())
                    .append(point.role() == PointRole.USE ? "使用位 " : "备用位 ")
                    .append(point.pointCode())
                    .append(" ")
                    .append(point.statusText())
                    .append("（")
                    .append(point.reason())
                    .append("）");
            found = true;
        }
    }
    if (!found) {
        text.append("无异常点位明细");
    }
    return text.toString();
}
```

- [ ] **Step 4: Add open log directory button to dialog**

In `showGroupAlertDialog(...)`, add secondary button:

```java
JButton openLogs = new JButton("打开日志目录");
openLogs.addActionListener(e -> openLogs());
buttons.add(openLogs);
buttons.add(ack);
```

- [ ] **Step 5: Run build**

Run:

```powershell
.\build.ps1
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```powershell
git add src/com/local/monitor/ShelfPointMonitorApp.java src/com/local/monitor/GroupStatusText.java test/com/local/monitor/ShelfPointMonitorAppUiTest.java
git commit -m "Use operator alert language"
```

---

### Task 7: Local Test Scenarios And Release Documentation

**Files:**
- Modify: `src/com/local/monitor/LocalTestDatabase.java`
- Modify: `build.ps1`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `VERSION`
- Modify: `docs/manuals/point-shortage-alert-user-manual.md`
- Create: `docs/releases/v0.4.0.md`

- [ ] **Step 1: Add local scenario notes to manual**

In `docs/manuals/point-shortage-alert-user-manual.md`, add a section explaining:

```markdown
## 检测周期与报警持续时间

检测周期表示系统多久查询一次数据库。
报警持续时间表示异常连续存在多久后弹窗。

示例：

- 检测周期：1 分钟
- 报警持续时间：5 分钟
- 含义：系统每 1 分钟查询一次；如果使用位连续 5 分钟无料，则报警。
```

- [ ] **Step 2: Update version**

Set `VERSION`:

```text
0.4.0
```

- [ ] **Step 3: Update `CHANGELOG.md`**

Add:

```markdown
## [0.4.0] - 2026-07-03

### Added

- Point status dashboard showing `有料`, `无料`, `未查到`, and `停用`.
- Per-group configurable check interval.
- Per-group configurable alert duration.
- Elapsed-time alert evaluation.
- Chinese operator-facing group status and alert text.

### Changed

- The point shortage page now prioritizes dashboard status over raw runtime logs.
- Automatic monitoring checks due groups based on each group's interval.

### Security

- Database access remains readonly.
- Passwords remain runtime-only and are not saved.
- Public package still contains sample point identifiers only.
```

- [ ] **Step 4: Create release note**

Create `docs/releases/v0.4.0.md`:

```markdown
# Release v0.4.0

Date: 2026-07-03

## Summary

Adds the onsite point status dashboard and configurable elapsed-time alerting.

## Included

- Point cards with `有料`, `无料`, `未查到`, and `停用`.
- Group statuses: `正常`, `观察中`, `需关注`, and `已关注`.
- Configurable check interval per point group.
- Configurable alert duration per point group.
- Elapsed-time alert logic independent of check interval.
- Chinese operator-facing alert dialog.

## Safety Boundary

- No SQL editor.
- No table editing UI.
- No database writes.
- Passwords are not saved.

## Validation

Release validation must include:

- `.\build.ps1`
- packaged self-test
- source privacy scan
- packaged zip privacy scan
```

- [ ] **Step 5: Update packaged defaults in `build.ps1`**

In the packaged `data/group-config.properties` block, add:

```text
group.0.rule.backupThresholdParticipates=true
```

Keep sample point codes only.

- [ ] **Step 6: Run full build**

Run:

```powershell
.\build.ps1
```

Expected:

```text
PointStatusMapperTest PASS
GroupCheckPlannerTest PASS
...
Version: 0.4.0
Zip:   ...ReadonlyBusinessDbTool-v0.4.0.zip
```

- [ ] **Step 7: Run packaged self-test**

Run:

```powershell
$java = if (Test-Path .\dist\ShelfPointMonitor\runtime\bin\java.exe) { ".\dist\ShelfPointMonitor\runtime\bin\java.exe" } else { "java" }
& $java -cp ".\dist\ShelfPointMonitor\ShelfPointMonitor.jar;.\dist\ShelfPointMonitor\lib\postgresql-42.2.25.jar;.\dist\ShelfPointMonitor\lib\h2-2.2.224.jar" com.local.monitor.ShelfPointMonitorApp --self-test
```

Expected:

```text
ShelfPointMonitor SELF_TEST_OK
```

- [ ] **Step 8: Run privacy scans**

Run:

```powershell
$pattern = '10\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}|[0-9]{6}BB[0-9]{6}|password\s*=\s*[^_[:space:]]'
rg -n $pattern src test docs build.ps1 README.md CHANGELOG.md VERSION .gitignore
$sourceExit = $LASTEXITCODE
$scanRoot = Join-Path (Resolve-Path .\build) ('release-scan-v040-' + [DateTime]::Now.ToString('yyyyMMddHHmmss'))
New-Item -ItemType Directory -Path $scanRoot | Out-Null
Expand-Archive -LiteralPath .\dist\ReadonlyBusinessDbTool-v0.4.0.zip -DestinationPath $scanRoot
rg -n $pattern $scanRoot
$zipExit = $LASTEXITCODE
if (($sourceExit -ne 1) -or ($zipExit -ne 1)) { throw "privacy scan found sensitive matches or failed" }
```

Expected: no matches and no thrown error.

- [ ] **Step 9: Commit**

```powershell
git add src/com/local/monitor/LocalTestDatabase.java build.ps1 README.md CHANGELOG.md VERSION docs/manuals/point-shortage-alert-user-manual.md docs/releases/v0.4.0.md
git commit -m "Prepare v0.4.0 release docs"
```

---

## Final Release Steps

- [ ] **Step 1: Run final build**

```powershell
.\build.ps1
```

- [ ] **Step 2: Verify status**

```powershell
git status -sb
git log --oneline -8
```

Expected: only ignored/local runtime files remain untracked, such as `.obsidian/`; no source changes are left unstaged.

- [ ] **Step 3: Push main**

```powershell
git push origin main
```

- [ ] **Step 4: Tag release**

```powershell
git tag -a v0.4.0 -m "v0.4.0"
git push origin v0.4.0
```

- [ ] **Step 5: Create GitHub release**

```powershell
gh release create v0.4.0 .\dist\ReadonlyBusinessDbTool-v0.4.0.zip --repo ZDH340826/readonly-business-db-tool --title "v0.4.0" --notes-file .\docs\releases\v0.4.0.md
```

- [ ] **Step 6: Confirm release**

```powershell
gh release view v0.4.0 --repo ZDH340826/readonly-business-db-tool --json tagName,name,url,assets --jq '{tag:.tagName,name:.name,url:.url,assets:[.assets[].name]}'
```

Expected: release has asset `ReadonlyBusinessDbTool-v0.4.0.zip`.

---

## Self-Review

- Spec coverage: point statuses, elapsed-time alerts, per-group interval, Chinese result text, alert dialog, config compatibility, docs, readonly boundary, and privacy scans are covered by tasks.
- Scope: one coherent subsystem, no database writes, no map rendering, no SQL editor.
- Type consistency: plan uses existing `PointGroupDefinition`, `GroupMonitorPoint`, `PointRecord`, `GroupAlertRule`, `GroupRuntimeState`, `GroupEvaluation`, and `ShelfPointMonitorApp` naming.
- Execution risk: `ShelfPointMonitorApp.java` is large, so UI work is delayed until domain tests pass and is limited to rendering the dashboard and controls.
