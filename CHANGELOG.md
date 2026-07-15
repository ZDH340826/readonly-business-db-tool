# Changelog

This project follows Semantic Versioning: `MAJOR.MINOR.PATCH`.

## [Unreleased]

### Added

- Eight local H2 demo groups backed by 33 synthetic rows, covering multi-use shortage, healthy use points, sufficient backups, abnormal status, lock, disabled, missing, normal, and threshold-edge behavior.
- Separate Chinese actions for adding use points and backup points, plus grouped status-board sections and explicit land-code/cargo-code labels.
- Regression coverage for multiple enabled use points, any-use-unavailable semantics, full demo packaging, and rejection of incomplete demo catalogs.

### Changed

- Point groups now accept one or more enabled use points. If any enabled use point is unavailable or missing, the existing use-point alarm condition is satisfied.
- Operator-facing `map_data_code` and `pod_code` terminology is now consistently “地码” and “货码” in the affected query and group-management views.
- The release build generates the local H2 database and all eight group definitions from one catalog instead of maintaining a duplicate one-group PowerShell configuration.

### Security

- Demo reset still rejects non-H2 configurations, and all production database access remains parameterized and read-only.
- No real-looking point codes, site credentials, private network addresses, or field configuration were added.

## [0.5.0-rc.1] - 2026-07-14

### Added

- Eight real operational pages with a shared Swing theme, fixed application shell, semantic status badges, and responsive split layouts.
- Structured read-only data query, current-page UTF-8 CSV export, metadata browser, and 100-row preview.
- Sanitized diagnostic ZIP, offline field preflight, embedded-runtime launchers, and Chinese operations documents.
- Named regression coverage for 27 field scenarios, Chinese/spaced Windows paths, configuration damage, and package scripts.

### Changed

- Query failures are an independent `QUERY_FAILED` state and never count as shortage, normal, or recovered business state.
- Monitoring uses immutable `MonitoringSession` snapshots, generation gates, task-local password copies, and separate monitor/I/O executors.
- Configuration readers safely fall back when Java properties syntax is damaged.

### Security

- PostgreSQL access remains parameterized and read-only; local H2 monitor connections are opened with database-level read-only access.
- Passwords remain memory-only and are excluded from configuration, logs, diagnostics, documents, and release archives.
- Diagnostics use an explicit six-entry allowlist and redact credentials, usernames, network identifiers, absolute paths, and stack/source locations.

## [0.4.0] - 2026-07-04

### Added

- Point status dashboard showing `有料`, `无料`, `未查到`, and `停用`.
- Per-group check interval.
- Per-group alert duration.
- Elapsed-time alert evaluation based on real elapsed time.
- Chinese operator-facing alert text.

### Changed

- The point shortage page now prioritizes the dashboard over raw runtime logs.
- Automatic monitoring now checks due groups based on each group's check interval.

### Security

- Database access remains readonly.
- Passwords remain runtime-only and are not saved.
- Public package still contains sample point identifiers only.

## [0.3.0] - 2026-07-01

### Added

- Site-ready PostgreSQL point query for `public.tcs_map_data`.
- Chinese connection-management labels for onsite operators.
- Default connection profile for onsite setup using placeholder host/user values, port `2345`, database `cms_web`, schema `public`, and `sslmode=disable`.
- Local H2 test database now mirrors the site point table shape with `tcs_map_data`.
- UI and profile-store tests covering Chinese labels and site connection defaults.

### Changed

- Point monitoring now reads site fields through stable internal aliases: `map_data_code`, `pod_code`, `pod_status`, `status`, `ind_lock`, and `date_chg`.
- Packaged connection templates no longer use generic `example_db` defaults.

### Security

- Passwords are still runtime-only and are not saved to `connections.properties`.
- Public repository stores only table/field mapping and sample point identifiers.
- Real IPs, usernames, passwords, and production point groups must remain local.

## [0.2.0] - 2026-06-30

### Added

- Point group shortage monitoring for one use point plus multiple backup points.
- Configurable group-level rules: use point empty, minimum available backups, and sustained duration in minutes.
- Fixed 60-second group monitoring loop aligned with the operational rule model.
- CSV logs for every check (`check-log.csv`) and alert events (`event-log.csv`).
- Obsidian-ready Chinese user manual under `docs/manuals/point-shortage-alert-user-manual.md`.
- Local H2 test data now seeds one use point and four backup points.

### Changed

- The alert page now focuses on area/material point groups instead of independent per-point alert intervals.
- Build validation now runs group logic, group config, and group log tests.
- Packaged defaults include `data/group-config.properties`.

### Security

- Group monitoring still uses fixed readonly SELECT queries through the existing repository.
- Group config stores local rules and point codes only; database passwords are not stored.

## [0.1.0] - 2026-06-30

### Added

- Initial public release of the readonly business database tool.
- Connection management for PostgreSQL and local H2 test profiles.
- Readonly database browser for schemas, tables/views, columns, and first-100-row previews.
- Shelf point shortage monitoring workflow with per-point monitoring intervals.
- Local H2 test database and scenario scripts for offline validation.
- Safety boundary: no SQL editor, no data editing UI, readonly JDBC transactions, and identifier validation.
- PowerShell build script with compilation, tests, local runtime packaging, and versioned zip output.

### Security

- Public release uses sanitized sample connection settings and sample point identifiers.
- Runtime state, database files, logs, dependency jars, and build output are excluded from Git.
