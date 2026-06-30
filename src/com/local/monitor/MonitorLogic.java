package com.local.monitor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class MonitorLogic {
    private MonitorLogic() {
    }

    public static MonitorEvaluation evaluate(
            List<PointDefinition> points,
            List<PointRecord> records,
            AlertState state) {
        Map<String, PointRecord> byCode = new LinkedHashMap<>();
        for (PointRecord record : records) {
            byCode.put(record.mapDataCode(), record);
        }

        List<PointAlert> allAlerts = new ArrayList<>();
        for (PointDefinition point : points) {
            PointRecord record = byCode.get(point.code());
            PointAlert alert = evaluatePoint(point, record);
            if (alert != null) {
                allAlerts.add(alert);
            }
        }
        allAlerts.sort(Comparator.comparing(PointAlert::code));

        Set<String> alertCodes = new HashSet<>();
        for (PointAlert alert : allAlerts) {
            alertCodes.add(alert.code());
        }
        List<String> healthyCodes = new ArrayList<>();
        for (PointDefinition point : points) {
            if (!alertCodes.contains(point.code())) {
                healthyCodes.add(point.code());
            }
        }
        state.clearAcknowledgementsForCodes(healthyCodes);

        List<PointAlert> visibleAlerts = new ArrayList<>();
        for (PointAlert alert : allAlerts) {
            if (!state.isAcknowledged(alertKey(alert))) {
                visibleAlerts.add(alert);
            }
        }

        if (allAlerts.isEmpty()) {
            return new MonitorEvaluation(allAlerts, "", false);
        }
        if (visibleAlerts.isEmpty()) {
            return new MonitorEvaluation(visibleAlerts, alertKey(allAlerts), true);
        }
        return new MonitorEvaluation(visibleAlerts, alertKey(visibleAlerts), false);
    }

    private static PointAlert evaluatePoint(PointDefinition point, PointRecord record) {
        if (record == null) {
            return new PointAlert(point.code(), point.alias(), "数据库未返回该点位");
        }

        List<String> problems = new ArrayList<>();
        if (isBlank(record.podCode())) {
            problems.add("无货架");
        }
        if (record.status() != 1) {
            problems.add("点位状态异常 status=" + record.status());
        }
        if (record.indLock() != 0) {
            problems.add("点位被锁定 lock_state=" + record.indLock());
        }
        if (problems.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner("；");
        for (String problem : problems) {
            joiner.add(problem);
        }
        return new PointAlert(point.code(), point.alias(), joiner.toString());
    }

    private static String alertKey(PointAlert alert) {
        return alert.code() + ":" + alert.message();
    }

    private static String alertKey(List<PointAlert> alerts) {
        StringJoiner joiner = new StringJoiner("|");
        for (PointAlert alert : alerts) {
            joiner.add(alertKey(alert));
        }
        return joiner.toString();
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


