package com.local.monitor;

import java.time.LocalDateTime;
import java.util.List;

public final class PointStatusMapperTest {
    public static void main(String[] args) {
        mapsAvailablePoint();
        mapsEmptyPointWhenShelfCodeIsBlank();
        mapsEmptyPointWhenLocked();
        mapsMissingPointWhenRecordIsAbsent();
        mapsDisabledPoint();
        System.out.println("PointStatusMapperTest PASS");
    }

    private static void mapsAvailablePoint() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("use", "USE_POINT_001", "使用位", PointRole.USE, true, 1)),
                List.of(record("USE_POINT_001", "SHELF_USE_001", 1, 0)));
        PointStatusView view = views.get(0);
        TestSupport.assertEquals(PointMaterialStatus.AVAILABLE, view.status(), "available point status");
        TestSupport.assertEquals("有料", view.statusText(), "available point text");
        TestSupport.assertEquals("SHELF_USE_001", view.shelfCode(), "available shelf code");
    }

    private static void mapsEmptyPointWhenShelfCodeIsBlank() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("use", "USE_POINT_001", "使用位", PointRole.USE, true, 1)),
                List.of(record("USE_POINT_001", "", 1, 0)));
        TestSupport.assertEquals(PointMaterialStatus.EMPTY, views.get(0).status(), "blank shelf should be empty");
        TestSupport.assertEquals("无货架", views.get(0).reason(), "blank shelf reason");
    }

    private static void mapsEmptyPointWhenLocked() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2)),
                List.of(record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 1)));
        TestSupport.assertEquals(PointMaterialStatus.EMPTY, views.get(0).status(), "locked point should be empty");
        TestSupport.assertEquals("锁定", views.get(0).reason(), "locked reason");
    }

    private static void mapsMissingPointWhenRecordIsAbsent() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2)),
                List.of());
        TestSupport.assertEquals(PointMaterialStatus.MISSING, views.get(0).status(), "missing record status");
        TestSupport.assertEquals("未查到", views.get(0).statusText(), "missing record text");
    }

    private static void mapsDisabledPoint() {
        List<PointStatusView> views = PointStatusMapper.map(
                List.of(point("backup", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, false, 2)),
                List.of(record("BACKUP_POINT_001", "SHELF_BACKUP_001", 1, 0)));
        TestSupport.assertEquals(PointMaterialStatus.DISABLED, views.get(0).status(), "disabled point status");
        TestSupport.assertEquals("已停用", views.get(0).reason(), "disabled reason");
    }

    private static GroupMonitorPoint point(String id, String code, String alias, PointRole role, boolean enabled, int sortOrder) {
        return new GroupMonitorPoint(id, code, alias, role, enabled, sortOrder);
    }

    private static PointRecord record(String code, String podCode, int status, int lock) {
        return new PointRecord(
                code,
                podCode,
                "1",
                status,
                lock,
                "AREA",
                "NEXT",
                LocalDateTime.of(2026, 7, 3, 6, 0),
                LocalDateTime.of(2026, 7, 3, 6, 0));
    }

    private static final class TestSupport {
        static void assertEquals(Object expected, Object actual, String message) {
            if (!java.util.Objects.equals(expected, actual)) {
                throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
            }
        }
    }
}
