package com.local.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PointGroupDefinition {
    public static final int DEFAULT_CHECK_INTERVAL_SECONDS = 60;
    public static final int MIN_CHECK_INTERVAL_SECONDS = 60;

    private final String id;
    private final String areaName;
    private final String groupName;
    private final String materialName;
    private final boolean enabled;
    private final int checkIntervalSeconds;
    private final List<GroupMonitorPoint> points;
    private final GroupAlertRule rule;

    public PointGroupDefinition(
            String id,
            String areaName,
            String groupName,
            String materialName,
            boolean enabled,
            int checkIntervalSeconds,
            List<GroupMonitorPoint> points,
            GroupAlertRule rule) {
        this.id = GroupMonitorPoint.requireText(id, "group id");
        this.areaName = GroupMonitorPoint.requireText(areaName, "area name");
        this.groupName = GroupMonitorPoint.requireText(groupName, "group name");
        this.materialName = GroupMonitorPoint.requireText(materialName, "material name");
        this.enabled = enabled;
        if (checkIntervalSeconds < MIN_CHECK_INTERVAL_SECONDS) {
            throw new IllegalArgumentException("checkIntervalSeconds must be >= 60");
        }
        this.checkIntervalSeconds = checkIntervalSeconds;
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("group points are required");
        }
        if (rule == null) {
            throw new IllegalArgumentException("group alert rule is required");
        }
        this.points = Collections.unmodifiableList(sorted(points));
        this.rule = rule;
        validatePoints(this.points);
    }

    public String id() {
        return id;
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

    public boolean enabled() {
        return enabled;
    }

    public int checkIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public List<GroupMonitorPoint> points() {
        return points;
    }

    public GroupAlertRule rule() {
        return rule;
    }

    public List<String> enabledPointCodes() {
        List<String> codes = new ArrayList<>();
        for (GroupMonitorPoint point : points) {
            if (point.enabled()) {
                codes.add(point.code());
            }
        }
        return Collections.unmodifiableList(codes);
    }

    private static List<GroupMonitorPoint> sorted(List<GroupMonitorPoint> source) {
        List<GroupMonitorPoint> copy = new ArrayList<>(source);
        copy.sort(Comparator.comparingInt(GroupMonitorPoint::sortOrder).thenComparing(GroupMonitorPoint::code));
        return copy;
    }

    private static void validatePoints(List<GroupMonitorPoint> points) {
        Set<String> ids = new HashSet<>();
        Set<String> codes = new HashSet<>();
        int useCount = 0;
        int backupCount = 0;
        for (GroupMonitorPoint point : points) {
            if (!ids.add(point.id())) {
                throw new IllegalArgumentException("duplicate point id: " + point.id());
            }
            if (!codes.add(point.code())) {
                throw new IllegalArgumentException("duplicate point code: " + point.code());
            }
            if (!point.enabled()) {
                continue;
            }
            if (point.role() == PointRole.USE) {
                useCount++;
            } else if (point.role() == PointRole.BACKUP) {
                backupCount++;
            }
        }
        if (useCount < 1) {
            throw new IllegalArgumentException("enabled group must contain at least one use point");
        }
        if (backupCount < 1) {
            throw new IllegalArgumentException("enabled group must contain at least one backup point");
        }
    }
}
