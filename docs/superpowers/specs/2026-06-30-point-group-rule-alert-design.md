# Point Group Rule Alert Design

## Goal

Upgrade the current point-level shortage monitor into a group-level rule-based alert system. Operators should be able to divide monitored points by area and material group, define group-specific alert rules, and receive alerts only when a material group needs manual attention.

## Problem

The current monitor evaluates each point independently. A single empty use point can trigger an alert even when backup points still have sufficient shelves. This creates unnecessary attention load for operators.

The desired behavior is group-level evaluation:

- A material group contains one use point and one or more backup points.
- Operators configure rules for each group.
- The system checks all enabled groups every minute.
- A group alerts only after its rule remains true for a configured duration.
- Every shortage condition is logged, including conditions that have not yet reached alert duration.

## Scope

Included:

- Area-based organization for distributed operations.
- Point groups with use and backup roles.
- Configurable group-level alert rules.
- Continuous shortage duration tracking.
- Group-level alert acknowledgment.
- CSV-based check and event logs.
- Readonly database access only.

Excluded from this phase:

- Central server synchronization between areas.
- User account and permission management.
- Remote notification channels.
- Rule scripting language.
- Database writes.

## Business Model

### Area

An area represents the responsibility scope of a local operations team.

Fields:

- `areaId`
- `areaName`
- `description`
- `enabled`

Each workstation can configure only the areas and material groups it owns.

### Point Group

A point group represents one material flow unit.

Fields:

- `groupId`
- `areaId`
- `groupName`
- `materialName`
- `enabled`
- `checkIntervalSeconds`

Default `checkIntervalSeconds` is `60`.

### Monitor Point

A monitor point belongs to one group and has a role.

Roles:

- `USE`: the active use point.
- `BACKUP`: backup storage points.

Fields:

- `pointId`
- `groupId`
- `pointCode`
- `pointAlias`
- `role`
- `sortOrder`
- `enabled`

Validation:

- Each enabled group must contain exactly one enabled `USE` point.
- Each enabled group must contain at least one enabled `BACKUP` point.
- A point code cannot appear twice inside the same group.

## Rule Model

This phase uses fixed rule templates rather than a scripting language.

Fields:

- `ruleId`
- `groupId`
- `enabled`
- `requireUsePointEmpty`
- `minBackupAvailable`
- `durationMinutes`

Rule semantics:

```text
if requireUsePointEmpty == true
and use point is empty
and backupAvailableCount < minBackupAvailable
and condition has remained true for durationMinutes
then alert
```

Example:

```text
Group: Material A
Use point: empty
Backup points: 4 total, 2 available
minBackupAvailable: 3
durationMinutes: 5
Result: alert after the condition is true for 5 consecutive checks
```

## Runtime Evaluation

The system checks every enabled group every 60 seconds.

For each group, compute:

- `usePointEmpty`
- `backupTotal`
- `backupAvailableCount`
- `backupMissingCount`
- `ruleMatched`
- `continuousMatchedChecks`
- `continuousMatchedMinutes`
- `alertState`

If a rule becomes false before reaching the configured duration, the continuous counter resets to zero.

## Group Alert State Machine

States:

- `NORMAL`: rule is not matched.
- `PENDING_ALERT`: rule is matched but duration threshold is not reached.
- `ACTIVE_ALERT`: rule is matched and duration threshold is reached.
- `ACKED_ALERT`: operator clicked acknowledged for the active alert.

Transitions:

```text
NORMAL -> PENDING_ALERT
  when the group rule first matches

PENDING_ALERT -> ACTIVE_ALERT
  when matched duration reaches durationMinutes

ACTIVE_ALERT -> ACKED_ALERT
  when operator clicks acknowledged

PENDING_ALERT / ACTIVE_ALERT / ACKED_ALERT -> NORMAL
  when the group rule no longer matches
```

Repeated dialogs:

- An acknowledged group alert must not show the same dialog again until the group recovers.
- Detection and shortage logs continue while the alert is acknowledged.

## Logging Requirements

### Check Log

Write a row for every check where the group is not fully healthy or where the rule is matched.

File:

```text
logs/check-log.csv
```

Columns:

- `time`
- `areaName`
- `groupName`
- `usePointEmpty`
- `backupTotal`
- `backupAvailableCount`
- `backupMissingCount`
- `ruleMatched`
- `continuousMatchedMinutes`
- `alertState`

### Event Log

Write a row for significant state transitions.

File:

```text
logs/event-log.csv
```

Event types:

- `GROUP_RULE_MATCHED`
- `GROUP_ALERT_TRIGGERED`
- `GROUP_ALERT_ACKED`
- `GROUP_RECOVERED`
- `CHECK_FAILED`

Columns:

- `time`
- `eventType`
- `areaName`
- `groupName`
- `message`

## UI Requirements

Add a group management workflow under the shortage alert page.

Recommended layout:

```text
Left: group list
Right top: group details
Right middle: point table
Right bottom: rule configuration and current state
```

Group fields:

- Area name
- Group name
- Material name
- Enabled

Point table fields:

- Alias
- Point code
- Role: USE or BACKUP
- Enabled

Rule fields:

- Require use point empty
- Minimum backup available count
- Continuous shortage minutes
- Rule enabled

Runtime view:

- Current use point status
- Backup available count
- Continuous matched minutes
- Current group alert state

## Data Storage

Store local configuration in a UTF-8 properties or CSV-backed format under `data/`.

Passwords remain runtime-only and must not be stored.

Suggested files:

```text
data/group-config.properties
data/group-points.csv
data/group-rules.csv
```

These files are local runtime configuration and remain ignored by Git.

## Database Boundary

The feature must preserve readonly behavior.

Allowed database operations:

- Fixed SELECT for configured point codes.
- Metadata reads already present in the database browser.

Disallowed:

- INSERT
- UPDATE
- DELETE
- DROP
- ALTER
- TRUNCATE
- Stored procedure calls that modify state

All database access must continue through the readonly connection factory.

## Acceptance Criteria

1. Operator can create and edit areas.
2. Operator can create and edit point groups.
3. A group can contain one use point and multiple backup points.
4. Operator can configure minimum backup available count.
5. Operator can configure continuous shortage minutes.
6. System checks enabled groups every 60 seconds.
7. A group does not alert when the use point is empty but backup availability is above threshold.
8. A group enters pending state when the rule matches but duration has not been reached.
9. A group alerts when the rule remains true for the configured duration.
10. Clicking acknowledged suppresses repeated dialogs until recovery.
11. Recovery resets continuous duration and acknowledgment state.
12. Check logs record shortage and pending states.
13. Event logs record rule match, alert, acknowledgment, recovery, and check failures.
14. No database write SQL is introduced.

## Test Strategy

Core tests:

- `GroupMonitorLogicTest`
  - healthy group does not alert
  - use empty but enough backup does not alert
  - rule match before duration is pending
  - rule match after duration alerts
  - recovery resets counter
  - acknowledged alert is suppressed until recovery

- `GroupConfigStoreTest`
  - saves and loads areas, groups, points, and rules
  - rejects groups without one use point
  - rejects invalid durations and thresholds

- `GroupLogWriterTest`
  - writes check log headers and rows
  - writes event log headers and rows

Integration tests:

- local H2 scenario with one group and five points
- automatic minute checks simulated with deterministic timestamps

## Release Plan

Target version: `0.2.0`

Reason:

- This is a backward-compatible feature expansion.
- It changes alert semantics from point-level to group-level but does not remove readonly database behavior.

Public release must keep sample area, group, point, and material names generic.
