package com.local.monitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GroupCheckPlanner {
    private GroupCheckPlanner() {
    }

    public static List<PointGroupDefinition> dueGroups(
            List<PointGroupDefinition> groups,
            Map<String, GroupRuntimeState> states,
            LocalDateTime now) {
        List<PointGroupDefinition> due = new ArrayList<>();
        for (PointGroupDefinition group : groups) {
            if (!group.enabled()) {
                continue;
            }
            GroupRuntimeState state = states.get(group.id());
            if (state == null || state.lastCheckedAt() == null) {
                due.add(group);
                continue;
            }
            long elapsedSeconds = Duration.between(state.lastCheckedAt(), now).getSeconds();
            if (elapsedSeconds >= group.checkIntervalSeconds()) {
                due.add(group);
            }
        }
        return due;
    }
}
