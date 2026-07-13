# CLAUDE.md

## Project

This is a Windows Java Swing desktop tool for field operations.

The tool connects to a PostgreSQL business database with a read-only account and monitors AMR / shelf / point shortage status. It displays shortage alarms, query failures, group status, local logs, and system settings.

## Critical Safety Rules

* Never connect to, scan, test, or modify a real production database unless the user explicitly provides a safe test target.
* Never add SQL editing capability.
* Never add INSERT, UPDATE, DELETE, DDL, or other write operations.
* All database access must remain read-only.
* Passwords must not be persisted.
* Do not commit logs, local configs, H2 database files, build outputs, runtime files, or real site connection information.
* Do not expose passwords, tokens, usernames, IPv4 addresses, JDBC URLs, absolute paths, Java stack frames, or `.java:line` details in user-visible UI, logs, diagnostics, or reports.

## Build

Use PowerShell on Windows:

```powershell
.\build.ps1
git diff --check
git status --short
```

Do not introduce Maven, Gradle, external UI frameworks, online icon libraries, or external fonts.

## Core Domain Rules

* `QUERY_FAILED` is an independent state.
* Query failure must not be treated as shortage, normal, or recovered.
* Acknowledged means the operator noticed the alarm. It does not mean recovered.
* Recovered must come from real state recovery or a real recovered event.
* Existing query failure behavior, recovery timing, and read-only database boundary must not regress.

## Current Priority

Before RC packaging, fix:

1. `ShelfPointMonitorApp.java` source integrity.
2. Monitoring session race when stopping monitoring or switching connections.
3. Async separation between monitoring work and file I/O.
4. Unified user-visible error sanitization.
5. Remaining UI layout gaps:

   * Group management: left tree / center basic info / right rules / bottom point table.
   * Data source browser: left object tree / center metadata / right preview.

## Forbidden Workarounds

* Do not delete tests to pass the build.
* Do not hide exceptions silently.
* Do not replace real functionality with static UI.
* Do not claim a page is complete if buttons are non-functional.
* Do not perform broad unrelated formatting.
* Do not rewrite core classes mechanically or from decompiled output.

## Required Reporting

For each task, create a report in `docs/ai/` with:

* What changed.
* Why it changed.
* Tests added or updated.
* Build result.
* `git diff --check` output.
* `git status --short` output.
* Remaining risks.
