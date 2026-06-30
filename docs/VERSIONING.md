# Versioning Policy

## Version Format

Use Semantic Versioning:

```text
MAJOR.MINOR.PATCH
```

- `MAJOR`: incompatible user-facing or configuration changes.
- `MINOR`: backward-compatible features.
- `PATCH`: backward-compatible fixes, documentation updates, or build improvements.

The current version is stored in the repository root `VERSION` file.

## Release Branching

- `main` is the public stable branch.
- Feature work should be developed on short-lived branches named `codex/<topic>` or `feature/<topic>`.
- Releases are tagged from `main` after tests and privacy checks pass.

## Release Checklist

1. Update `VERSION`.
2. Update `CHANGELOG.md`.
3. Add or update `docs/releases/vX.Y.Z.md`.
4. Run the full build:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
   ```

5. Run a committed-source privacy scan using project-specific private patterns.
   Keep the private pattern list outside this public repository.

   ```powershell
   git grep -n -E "<private-patterns>" HEAD
   ```

   Expected result: no matches.

6. Commit release metadata.
7. Create an annotated tag:

   ```powershell
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   ```

8. Push main and the tag.
9. Create the GitHub release using the matching release notes.

## Artifact Naming

Build output uses:

```text
dist/ReadonlyBusinessDbTool-vX.Y.Z.zip
```

The package also contains a `VERSION` file with the same value.
