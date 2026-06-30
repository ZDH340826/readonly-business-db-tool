package com.local.monitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PointSchedule {
    private final Map<String, LocalDateTime> nextCheckByCode = new HashMap<>();

    public List<PointDefinition> duePoints(List<PointDefinition> points, LocalDateTime now) {
        List<PointDefinition> due = new ArrayList<>();
        for (PointDefinition point : points) {
            LocalDateTime nextCheck = nextCheckByCode.get(point.code());
            if (nextCheck == null || !now.isBefore(nextCheck)) {
                due.add(point);
            }
        }
        return due;
    }

    public List<PointDefinition> forceAll(List<PointDefinition> points) {
        return new ArrayList<>(points);
    }

    public void markChecked(List<PointDefinition> points, LocalDateTime now) {
        for (PointDefinition point : points) {
            nextCheckByCode.put(point.code(), now.plusMinutes(point.intervalMinutes()));
        }
    }

    public void clear() {
        nextCheckByCode.clear();
    }
}


