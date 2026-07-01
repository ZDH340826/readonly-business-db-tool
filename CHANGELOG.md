# Changelog

This project follows Semantic Versioning: `MAJOR.MINOR.PATCH`.

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
