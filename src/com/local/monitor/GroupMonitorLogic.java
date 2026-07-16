package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class GroupMonitorLogic {
    private GroupMonitorLogic() {
    }

    public static GroupEvaluation evaluate(
            PointGroupDefinition group,
            List<PointRecord> records,
            GroupRuntimeState state) {
        return evaluate(group, records, state, LocalDateTime.now());
    }

    public static GroupEvaluation evaluate(
            PointGroupDefinition group,
            List<PointRecord> records,
            GroupRuntimeState state,
            LocalDateTime now) {
        if (group == null) {
            throw new IllegalArgumentException("group is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("runtime state is required");
        }
        if (now == null) {
            throw new IllegalArgumentException("now is required");
        }

        List<PointStatusView> pointStatuses = PointStatusMapper.map(group.points(), records);
        boolean anyUsePointUnavailable = false;
        int backupTotal = 0;
        int backupAvailable = 0;

        for (PointStatusView point : pointStatuses) {
            if (!point.enabled()) {
                continue;
            }
            if (point.role() == PointRole.USE) {
                if (!point.available()) {
                    anyUsePointUnavailable = true;
                }
            } else if (point.role() == PointRole.BACKUP) {
                backupTotal++;
                if (point.available()) {
                    backupAvailable++;
                }
            }
        }

        GroupAlertRule rule = group.rule();
        boolean useCondition = !rule.requireUsePointEmpty() || anyUsePointUnavailable;
        boolean backupCondition = !rule.backupThresholdParticipates()
                || backupAvailable < rule.minBackupAvailable();
        boolean ruleMatched = group.enabled() && rule.enabled() && useCondition && backupCondition;

        GroupAlertStatus status;
        boolean shouldShowDialog = false;
        int continuousSeconds = 0;
        if (!ruleMatched) {
            state.reset();
            state.markChecked(now);
            status = GroupAlertStatus.NORMAL;
        } else {
            state.markMatched(now);
            continuousSeconds = state.continuousMatchedSeconds(now);
            state.markChecked(now);
            if (continuousSeconds < rule.durationSeconds()) {
                status = GroupAlertStatus.PENDING_ALERT;
            } else if (state.isAcknowledged()) {
                status = GroupAlertStatus.ACKED_ALERT;
            } else {
                status = GroupAlertStatus.ACTIVE_ALERT;
                shouldShowDialog = !state.activeDialogShown();
                if (shouldShowDialog) {
                    state.markActiveDialogShown();
                }
            }
        }

        int backupMissing = backupTotal - backupAvailable;
        return new GroupEvaluation(
                group.id(),
                group.areaName(),
                group.groupName(),
                group.materialName(),
                status,
                anyUsePointUnavailable,
                backupTotal,
                backupAvailable,
                backupMissing,
                ruleMatched,
                continuousSeconds,
                rule.durationSeconds(),
                pointStatuses,
                shouldShowDialog,
                GroupStatusText.summary(
                        group,
                        status,
                        anyUsePointUnavailable,
                        backupTotal,
                        backupAvailable,
                        continuousSeconds,
                        rule.durationSeconds(),
                        pointStatuses));
    }

    public static GroupEvaluation queryFailed(
            PointGroupDefinition group,
            GroupRuntimeState state,
            LocalDateTime now,
            String errorSummary) {
        if (group == null) {
            throw new IllegalArgumentException("group is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("runtime state is required");
        }
        if (now == null) {
            throw new IllegalArgumentException("now is required");
        }
        state.markQueryFailed(now);
        int backupTotal = 0;
        for (GroupMonitorPoint point : group.points()) {
            if (point.enabled() && point.role() == PointRole.BACKUP) {
                backupTotal++;
            }
        }
        String message = errorSummary == null || errorSummary.isBlank()
                ? "查询失败：本次未能从数据库获得点位状态。"
                : errorSummary;
        return new GroupEvaluation(
                group.id(),
                group.areaName(),
                group.groupName(),
                group.materialName(),
                GroupAlertStatus.QUERY_FAILED,
                false,
                backupTotal,
                0,
                0,
                false,
                0,
                group.rule().durationSeconds(),
                List.of(),
                false,
                message);
    }

    static boolean isAvailable(PointRecord record) {
        return record != null
                && !MonitorLogic.isBlank(record.podCode())
                && record.status() == 1
                && record.indLock() == 0;
    }

}
