package com.local.monitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PointStatusMapper {
    private PointStatusMapper() {
    }

    public static List<PointStatusView> map(List<GroupMonitorPoint> points, List<PointRecord> records) {
        Map<String, PointRecord> byCode = new LinkedHashMap<>();
        if (records != null) {
            for (PointRecord record : records) {
                byCode.put(record.mapDataCode(), record);
            }
        }

        List<PointStatusView> views = new ArrayList<>();
        for (GroupMonitorPoint point : points) {
            PointRecord record = byCode.get(point.code());
            views.add(mapOne(point, record));
        }
        return views;
    }

    private static PointStatusView mapOne(GroupMonitorPoint point, PointRecord record) {
        if (!point.enabled()) {
            return view(point, null, PointMaterialStatus.DISABLED, "", "已停用");
        }
        if (record == null) {
            return view(point, null, PointMaterialStatus.MISSING, "", "点位未返回");
        }
        if (MonitorLogic.isBlank(record.podCode())) {
            return view(point, record, PointMaterialStatus.EMPTY, "", "无货架");
        }
        if (record.status() != 1) {
            return view(point, record, PointMaterialStatus.EMPTY, record.podCode(), "点位状态异常");
        }
        if (record.indLock() != 0) {
            return view(point, record, PointMaterialStatus.EMPTY, record.podCode(), "锁定");
        }
        return view(point, record, PointMaterialStatus.AVAILABLE, record.podCode(), "正常");
    }

    private static PointStatusView view(
            GroupMonitorPoint point,
            PointRecord record,
            PointMaterialStatus status,
            String shelfCode,
            String reason) {
        return new PointStatusView(
                point.id(),
                point.code(),
                point.alias(),
                point.role(),
                point.enabled(),
                status,
                shelfCode,
                record == null ? null : record.dateChg(),
                reason);
    }
}
