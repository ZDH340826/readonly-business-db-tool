package com.local.monitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GroupMonitorLogic {
    private GroupMonitorLogic() {
    }

    public static GroupEvaluation evaluate(
            PointGroupDefinition group,
            List<PointRecord> records,
            GroupRuntimeState state) {
        if (group == null) {
            throw new IllegalArgumentException("group is required");
        }
        if (state == null) {
            throw new IllegalArgumentException("runtime state is required");
        }

        Map<String, PointRecord> byCode = byCode(records);
        GroupMonitorPoint usePoint = null;
        boolean usePointEmpty = true;
        int backupTotal = 0;
        int backupAvailable = 0;

        for (GroupMonitorPoint point : group.points()) {
            if (!point.enabled()) {
                continue;
            }
            PointRecord record = byCode.get(point.code());
            boolean available = isAvailable(record);
            if (point.role() == PointRole.USE) {
                usePoint = point;
                usePointEmpty = !available;
            } else if (point.role() == PointRole.BACKUP) {
                backupTotal++;
                if (available) {
                    backupAvailable++;
                }
            }
        }

        GroupAlertRule rule = group.rule();
        boolean useCondition = !rule.requireUsePointEmpty() || usePointEmpty;
        boolean backupCondition = backupAvailable < rule.minBackupAvailable();
        boolean ruleMatched = group.enabled() && rule.enabled() && useCondition && backupCondition;

        GroupAlertStatus status;
        boolean shouldShowDialog = false;
        if (!ruleMatched) {
            state.reset();
            status = GroupAlertStatus.NORMAL;
        } else {
            state.markMatched();
            if (state.continuousMatchedMinutes() < rule.durationMinutes()) {
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

        return new GroupEvaluation(
                group.id(),
                group.areaName(),
                group.groupName(),
                group.materialName(),
                status,
                usePointEmpty,
                backupTotal,
                backupAvailable,
                backupTotal - backupAvailable,
                ruleMatched,
                state.continuousMatchedMinutes(),
                shouldShowDialog,
                message(group, usePoint, usePointEmpty, backupTotal, backupAvailable, rule));
    }

    static boolean isAvailable(PointRecord record) {
        return record != null
                && !MonitorLogic.isBlank(record.podCode())
                && record.status() == 1
                && record.indLock() == 0;
    }

    private static Map<String, PointRecord> byCode(List<PointRecord> records) {
        Map<String, PointRecord> byCode = new LinkedHashMap<>();
        if (records == null) {
            return byCode;
        }
        for (PointRecord record : records) {
            byCode.put(record.mapDataCode(), record);
        }
        return byCode;
    }

    private static String message(
            PointGroupDefinition group,
            GroupMonitorPoint usePoint,
            boolean usePointEmpty,
            int backupTotal,
            int backupAvailable,
            GroupAlertRule rule) {
        String useCode = usePoint == null ? "UNKNOWN_USE_POINT" : usePoint.code();
        return "area=" + group.areaName()
                + ", group=" + group.groupName()
                + ", material=" + group.materialName()
                + ", usePoint=" + useCode
                + ", usePointEmpty=" + usePointEmpty
                + ", backupAvailable=" + backupAvailable + "/" + backupTotal
                + ", minBackupAvailable=" + rule.minBackupAvailable()
                + ", durationMinutes=" + rule.durationMinutes();
    }
}
