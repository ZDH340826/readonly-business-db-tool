# Point Group Rule Alert Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement group-level shortage monitoring with configurable area/group/point rules, duration-based alerts, CSV logs, UI management, and Obsidian-ready documentation.

**Architecture:** Add focused domain classes for groups, points, rules, runtime state, evaluations, and logs. Reuse the existing readonly JDBC repository to fetch point records, then evaluate groups in memory. Replace the point-level alert page with a group management and group monitoring workflow while preserving connection management and database browser behavior.

**Tech Stack:** Java Swing, JDBC, local H2 test database, PowerShell build script, Markdown documentation.

---

### Task 1: Core Group Alert Logic

**Files:**
- Create: `src/com/local/monitor/PointRole.java`
- Create: `src/com/local/monitor/GroupAlertStatus.java`
- Create: `src/com/local/monitor/GroupMonitorPoint.java`
- Create: `src/com/local/monitor/GroupAlertRule.java`
- Create: `src/com/local/monitor/PointGroupDefinition.java`
- Create: `src/com/local/monitor/GroupRuntimeState.java`
- Create: `src/com/local/monitor/GroupEvaluation.java`
- Create: `src/com/local/monitor/GroupMonitorLogic.java`
- Test: `test/com/local/monitor/GroupMonitorLogicTest.java`

- [x] Write failing tests for healthy, pending, active, acknowledged, and recovered group states.
- [x] Implement the core classes and state transition logic.
- [x] Run `GroupMonitorLogicTest` and existing monitor tests.
- [x] Commit and push the branch.

### Task 2: Group Config Store

**Files:**
- Create: `src/com/local/monitor/GroupConfigStore.java`
- Test: `test/com/local/monitor/GroupConfigStoreTest.java`

- [x] Write failing tests for saving/loading groups and rejecting invalid group definitions.
- [x] Implement a UTF-8 properties-backed local config format under `data/group-config.properties`.
- [x] Run config and group logic tests.
- [x] Commit and push the branch.

### Task 3: Group Log Writer

**Files:**
- Create: `src/com/local/monitor/GroupLogWriter.java`
- Test: `test/com/local/monitor/GroupLogWriterTest.java`

- [x] Write failing tests for `check-log.csv` and `event-log.csv` headers and rows.
- [x] Implement CSV escaping and append behavior.
- [x] Run log writer tests.
- [x] Commit and push the branch.

### Task 4: Local Test Data And Build

**Files:**
- Modify: `src/com/local/monitor/LocalTestDatabase.java`
- Modify: `build.ps1`
- Modify: existing tests that assume only two seeded points

- [x] Seed one use point and four backup points in the public local H2 database.
- [x] Add new tests to the build script.
- [x] Run full build.
- [x] Commit and push the branch.

### Task 5: Alert Page UI And Monitoring Loop

**Files:**
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

- [x] Replace point-level alert UI with group list, group detail fields, point table, rule controls, runtime status, and monitor buttons.
- [x] Use current readonly connection and `PointRepository.fetch` for enabled group points.
- [x] Evaluate groups every 60 seconds.
- [x] Show group-level dialogs and write logs.
- [x] Run UI and full build tests.
- [x] Commit and push the branch.

### Task 6: Manual And Version Prep

**Files:**
- Create: `docs/manuals/point-shortage-alert-user-manual.md`
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `VERSION`
- Create: `docs/releases/v0.2.0.md`

- [x] Write an Obsidian-ready Chinese user manual.
- [x] Update version to `0.2.0`.
- [x] Update changelog and release notes.
- [x] Run full build and privacy scans.
- [ ] Commit, push, merge to main, tag `v0.2.0`, and create a GitHub release.
