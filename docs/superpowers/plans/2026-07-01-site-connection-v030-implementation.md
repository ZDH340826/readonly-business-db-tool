# v0.3.0 Site Connection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a site-ready v0.3.0 that uses Chinese connection UI labels, local reusable connection profiles, and the real readonly site point table `public.tcs_map_data`.

**Architecture:** Keep the existing Swing/JDBC architecture. Replace the point monitor query table shape from the public sample `shelf_point_status` to site-compatible `tcs_map_data`, while keeping all queries parameterized and readonly. Keep credentials out of Git; only save non-secret connection profile fields locally.

**Tech Stack:** Java Swing, JDBC, PostgreSQL driver, H2 local test database, PowerShell build script, Markdown documentation.

---

### Task 1: Site Point Query

**Files:**
- Modify: `src/com/local/monitor/PointQuery.java`
- Modify: `src/com/local/monitor/PointRepository.java`
- Modify: `test/com/local/monitor/MonitorLogicTest.java`

- [ ] Write a failing test that `PointQuery.buildSelectSql("public", 2)` selects from `public.tcs_map_data`.
- [ ] Assert the SQL selects site columns with stable aliases: `map_data_code as point_code`, `pod_code as shelf_code`, `ind_lock as lock_state`, `date_chg as updated_at`, `date_cr as marked_at`.
- [ ] Implement the SQL change.
- [ ] Run `.\build.ps1` and verify the query test passes.
- [ ] Commit and push.

### Task 2: Local H2 Site Table

**Files:**
- Modify: `src/com/local/monitor/LocalTestDatabase.java`
- Modify: `test/com/local/monitor/LocalTestDatabaseTest.java`
- Modify: `test/com/local/monitor/DbMetadataRepositoryTest.java`

- [ ] Write failing tests that the local database exposes `public.tcs_map_data` and seeds five point records.
- [ ] Change the H2 schema creation and insert/update statements to use `tcs_map_data` with site-compatible columns.
- [ ] Keep sample point codes only: `USE_POINT_001`, `BACKUP_POINT_001` through `BACKUP_POINT_004`.
- [ ] Run `.\build.ps1`.
- [ ] Commit and push.

### Task 3: Chinese Connection UI And Defaults

**Files:**
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `src/com/local/monitor/ConnectionProfileStore.java`
- Modify: `test/com/local/monitor/ConnectionProfileStoreTest.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`
- Modify: `build.ps1`

- [ ] Write failing tests that default PostgreSQL profile uses `cms_web`, `2345`, `public`, `disable`, and placeholder host/user values.
- [ ] Write a UI test that connection page labels include `服务器地址/IP`, `数据库名`, `数据库空间/Schema`, `用户名`, `密码`, `SSL模式`.
- [ ] Replace operator-facing connection labels/buttons with Chinese text.
- [ ] Update new-profile defaults and packaged `connections.properties` defaults.
- [ ] Run `.\build.ps1`.
- [ ] Commit and push.

### Task 4: Documentation, Version, And Release Prep

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `VERSION`
- Modify: `docs/manuals/point-shortage-alert-user-manual.md`
- Create: `docs/releases/v0.3.0.md`

- [ ] Update version to `0.3.0`.
- [ ] Document that users enter IP/username/password locally and passwords are not saved.
- [ ] Document the public-safe site table mapping.
- [ ] Document that real IPs, usernames, passwords, and point groups must not be committed.
- [ ] Run full build and privacy scans.
- [ ] Commit, push, tag `v0.3.0`, and create a GitHub release.
