package com.local.monitor;

import java.util.List;

public final class GroupStatusText {
    private GroupStatusText() {
    }

    public static String statusText(GroupAlertStatus status) {
        if (status == GroupAlertStatus.NORMAL) {
            return "正常";
        }
        if (status == GroupAlertStatus.PENDING_ALERT) {
            return "观察中";
        }
        if (status == GroupAlertStatus.ACTIVE_ALERT) {
            return "需关注";
        }
        if (status == GroupAlertStatus.ACKED_ALERT) {
            return "已关注";
        }
        return "未查到";
    }

    public static String minutesText(int seconds) {
        if (seconds <= 0) {
            return "0";
        }
        return String.valueOf((seconds + 59) / 60);
    }

    public static String summary(
            PointGroupDefinition group,
            GroupAlertStatus status,
            boolean usePointEmpty,
            int backupTotal,
            int backupAvailable,
            int continuousMatchedSeconds,
            int alertDurationSeconds,
            List<PointStatusView> pointStatuses) {
        return summary(
                group.areaName(),
                group.groupName(),
                group.materialName(),
                status,
                usePointEmpty,
                backupTotal,
                backupAvailable,
                continuousMatchedSeconds,
                alertDurationSeconds,
                pointStatuses);
    }

    public static String summary(
            String areaName,
            String groupName,
            String materialName,
            GroupAlertStatus status,
            boolean usePointEmpty,
            int backupTotal,
            int backupAvailable,
            int continuousMatchedSeconds,
            int alertDurationSeconds,
            List<PointStatusView> pointStatuses) {
        StringBuilder builder = new StringBuilder();
        builder.append(areaName)
                .append(" / ")
                .append(groupName)
                .append(" / ")
                .append(materialName)
                .append("：")
                .append(statusText(status))
                .append("。");
        builder.append("使用位").append(usePointEmpty ? "无料" : "有料").append("；");
        builder.append("备用位有料 ").append(backupAvailable).append("/").append(backupTotal).append("；");
        builder.append("持续 ")
                .append(minutesText(continuousMatchedSeconds))
                .append(" 分钟，规则 ")
                .append(minutesText(alertDurationSeconds))
                .append(" 分钟。");
        builder.append("点位状态：").append(pointStatusSummary(pointStatuses));
        return builder.toString();
    }

    private static String pointStatusSummary(List<PointStatusView> pointStatuses) {
        if (pointStatuses == null || pointStatuses.isEmpty()) {
            return "未查到";
        }
        StringBuilder builder = new StringBuilder();
        for (PointStatusView view : pointStatuses) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append(view.alias())
                    .append(" ")
                    .append(roleText(view.role()))
                    .append(" ")
                    .append(materialStatusText(view.status()));
            if (view.shelfCode() != null && !view.shelfCode().isEmpty()) {
                builder.append(" ").append(view.shelfCode());
            }
        }
        return builder.toString();
    }

    private static String roleText(PointRole role) {
        if (role == PointRole.USE) {
            return "使用位";
        }
        if (role == PointRole.BACKUP) {
            return "备用位";
        }
        return "点位";
    }

    private static String materialStatusText(PointMaterialStatus status) {
        if (status == PointMaterialStatus.AVAILABLE) {
            return "有料";
        }
        if (status == PointMaterialStatus.EMPTY) {
            return "无料";
        }
        if (status == PointMaterialStatus.MISSING) {
            return "未查到";
        }
        if (status == PointMaterialStatus.DISABLED) {
            return "停用";
        }
        return "未查到";
    }
}
