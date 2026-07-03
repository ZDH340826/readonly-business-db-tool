package com.local.monitor;

import java.util.List;

public final class GroupEvaluation {
    private final String groupId;
    private final String areaName;
    private final String groupName;
    private final String materialName;
    private final GroupAlertStatus status;
    private final boolean usePointEmpty;
    private final int backupTotal;
    private final int backupAvailableCount;
    private final int backupMissingCount;
    private final boolean ruleMatched;
    private final int continuousMatchedSeconds;
    private final int alertDurationSeconds;
    private final List<PointStatusView> pointStatuses;
    private final boolean shouldShowDialog;
    private final String message;

    public GroupEvaluation(
            String groupId,
            String areaName,
            String groupName,
            String materialName,
            GroupAlertStatus status,
            boolean usePointEmpty,
            int backupTotal,
            int backupAvailableCount,
            int backupMissingCount,
            boolean ruleMatched,
            int continuousMatchedSeconds,
            int alertDurationSeconds,
            List<PointStatusView> pointStatuses,
            boolean shouldShowDialog,
            String message) {
        this.groupId = groupId;
        this.areaName = areaName;
        this.groupName = groupName;
        this.materialName = materialName;
        this.status = status;
        this.usePointEmpty = usePointEmpty;
        this.backupTotal = backupTotal;
        this.backupAvailableCount = backupAvailableCount;
        this.backupMissingCount = backupMissingCount;
        this.ruleMatched = ruleMatched;
        this.continuousMatchedSeconds = Math.max(0, continuousMatchedSeconds);
        this.alertDurationSeconds = Math.max(0, alertDurationSeconds);
        this.pointStatuses = pointStatuses == null ? List.of() : List.copyOf(pointStatuses);
        this.shouldShowDialog = shouldShowDialog;
        this.message = message;
    }

    public GroupEvaluation(
            String groupId,
            String areaName,
            String groupName,
            String materialName,
            GroupAlertStatus status,
            boolean usePointEmpty,
            int backupTotal,
            int backupAvailableCount,
            int backupMissingCount,
            boolean ruleMatched,
            int continuousMatchedMinutes,
            boolean shouldShowDialog,
            String message) {
        this(
                groupId,
                areaName,
                groupName,
                materialName,
                status,
                usePointEmpty,
                backupTotal,
                backupAvailableCount,
                backupMissingCount,
                ruleMatched,
                Math.max(0, continuousMatchedMinutes) * 60,
                Math.max(0, continuousMatchedMinutes) * 60,
                List.of(),
                shouldShowDialog,
                message);
    }

    public String groupId() {
        return groupId;
    }

    public String areaName() {
        return areaName;
    }

    public String groupName() {
        return groupName;
    }

    public String materialName() {
        return materialName;
    }

    public GroupAlertStatus status() {
        return status;
    }

    public boolean usePointEmpty() {
        return usePointEmpty;
    }

    public int backupTotal() {
        return backupTotal;
    }

    public int backupAvailableCount() {
        return backupAvailableCount;
    }

    public int backupMissingCount() {
        return backupMissingCount;
    }

    public boolean ruleMatched() {
        return ruleMatched;
    }

    public int continuousMatchedSeconds() {
        return continuousMatchedSeconds;
    }

    public int alertDurationSeconds() {
        return alertDurationSeconds;
    }

    public List<PointStatusView> pointStatuses() {
        return pointStatuses;
    }

    public int continuousMatchedMinutes() {
        if (continuousMatchedSeconds == 0) {
            return 0;
        }
        return (continuousMatchedSeconds + 59) / 60;
    }

    public boolean shouldShowDialog() {
        return shouldShowDialog;
    }

    public String message() {
        return message;
    }
}
