# Readonly Business DB Tool Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing shelf point monitor into a first-stage readonly business database tool with connection management, a metadata browser, and the existing shelf point alert workflow.

**Architecture:** Keep Java Swing and JDBC. Add focused model/repository classes for connection profiles and metadata browsing, route all JDBC connections through one readonly connection factory, and refactor the app window into navigation pages.

**Tech Stack:** Java 21-compatible source, Swing, JDBC, PostgreSQL JDBC driver, H2 local test database, PowerShell build script.

---

### Task 1: Connection Profile Model And Store

**Files:**
- Create: `src/com/local/monitor/ConnectionProfile.java`
- Create: `src/com/local/monitor/ConnectionProfileStore.java`
- Test: `test/com/local/monitor/ConnectionProfileStoreTest.java`

- [ ] Write `ConnectionProfileStoreTest` that saves two profiles, reloads them, and asserts no password key is written.
- [ ] Run compile/test and confirm it fails because the classes do not exist.
- [ ] Implement `ConnectionProfile` with immutable fields and `toDbConfig(int intervalSeconds)`.
- [ ] Implement `ConnectionProfileStore` using UTF-8 `.properties` format with `currentProfile` and `profile.N.*` keys.
- [ ] Run the test and confirm it passes.

### Task 2: Readonly Connection Factory

**Files:**
- Create: `src/com/local/monitor/ReadOnlyConnectionFactory.java`
- Modify: `src/com/local/monitor/PointRepository.java`
- Test: update `test/com/local/monitor/LocalTestDatabaseTest.java` if needed

- [ ] Move readonly connection creation out of `PointRepository`.
- [ ] Keep local H2 create-if-missing behavior before opening local test connections.
- [ ] For PostgreSQL, execute `SET TRANSACTION READ ONLY` and `SET statement_timeout='8s'`.
- [ ] Update `PointRepository` to call the factory.
- [ ] Run existing point and local DB tests.

### Task 3: Database Metadata Browser Backend

**Files:**
- Create: `src/com/local/monitor/DbMetadataRepository.java`
- Create: `src/com/local/monitor/SchemaInfo.java`
- Create: `src/com/local/monitor/TableInfo.java`
- Create: `src/com/local/monitor/ColumnInfo.java`
- Create: `src/com/local/monitor/TablePreview.java`
- Test: `test/com/local/monitor/DbMetadataRepositoryTest.java`

- [ ] Write tests against local H2: schemas contain `public`, tables contain `shelf_point_status`, columns contain `point_code`, preview returns rows.
- [ ] Add a test that `previewTable` rejects invalid schema/table names such as `public;drop`.
- [ ] Run tests and confirm failure before implementation.
- [ ] Implement metadata reads using `DatabaseMetaData`.
- [ ] Implement preview using fixed readonly SELECT with validated identifiers and bounded limit.
- [ ] Run the metadata test and confirm it passes.

### Task 4: Main UI Refactor

**Files:**
- Modify: `src/com/local/monitor/ShelfPointMonitorApp.java`
- Modify: `test/com/local/monitor/ShelfPointMonitorAppUiTest.java`

- [ ] Update UI test to recursively scan all `GridBagLayout` containers for overlapping cells.
- [ ] Refactor app to left navigation plus pages: connection management, database browser, point alert.
- [ ] Store current connection profile and current password in memory only.
- [ ] Connection page supports new/save/delete/test.
- [ ] Browser page supports refresh schemas, load tables/views, load columns, preview first 100 rows.
- [ ] Point alert page keeps existing point table and monitoring controls.
- [ ] Run UI test and existing tests.

### Task 5: Build Script And Documentation

**Files:**
- Modify: `build.ps1`
- Modify: `README.md`

- [ ] Add new test classes to `build.ps1`.
- [ ] Include default `connections.properties` in `dist/ShelfPointMonitor/data`.
- [ ] Update README with first-stage tool scope and readonly boundary.
- [ ] Run full build.
- [ ] Run packaged `--self-test`.
- [ ] Confirm `dist/ShelfPointMonitor-MVP.zip` is regenerated.


