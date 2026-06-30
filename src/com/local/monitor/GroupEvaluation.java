package com.local.monitor;

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
    private final int continuousMatchedMinutes;
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
            int continuousMatchedMinutes,
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
        this.continuousMatchedMinutes = continuousMatchedMinutes;
        this.shouldShowDialog = shouldShowDialog;
        this.message = message;
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

    public int continuousMatchedMinutes() {
        return continuousMatchedMinutes;
    }

    public boolean shouldShowDialog() {
        return shouldShowDialog;
    }

    public String message() {
        return message;
    }
}
