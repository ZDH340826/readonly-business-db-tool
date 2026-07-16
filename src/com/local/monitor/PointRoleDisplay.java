package com.local.monitor;

public final class PointRoleDisplay {
    public static final String USE = "使用位";
    public static final String BACKUP = "备用位";

    private PointRoleDisplay() {
    }

    public static String display(PointRole role) {
        if (role == PointRole.USE) {
            return USE;
        }
        if (role == PointRole.BACKUP) {
            return BACKUP;
        }
        throw new IllegalArgumentException("未知点位角色");
    }

    public static PointRole parse(String text) {
        String value = text == null ? "" : text.trim();
        if (USE.equals(value) || PointRole.USE.name().equalsIgnoreCase(value)) {
            return PointRole.USE;
        }
        if (BACKUP.equals(value) || PointRole.BACKUP.name().equalsIgnoreCase(value)) {
            return PointRole.BACKUP;
        }
        throw new IllegalArgumentException("点位角色只能选择使用位或备用位");
    }

    public static String[] options() {
        return new String[] {USE, BACKUP};
    }
}
