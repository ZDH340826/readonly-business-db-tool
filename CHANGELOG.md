# Changelog

This project follows Semantic Versioning: `MAJOR.MINOR.PATCH`.

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
