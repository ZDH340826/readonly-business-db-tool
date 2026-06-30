# Changelog

This project follows Semantic Versioning: `MAJOR.MINOR.PATCH`.

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
