package com.local.monitor;

import java.time.LocalDateTime;

public final class PointStatusView {
    private final String pointId;
    private final String pointCode;
    private final String alias;
    private final PointRole role;
    private final boolean enabled;
    private final PointMaterialStatus status;
    private final String shelfCode;
    private final LocalDateTime updatedAt;
    private final String reason;

    public PointStatusView(
            String pointId,
            String pointCode,
            String alias,
            PointRole role,
            boolean enabled,
            PointMaterialStatus status,
            String shelfCode,
            LocalDateTime updatedAt,
            String reason) {
        this.pointId = pointId;
        this.pointCode = pointCode;
        this.alias = alias;
        this.role = role;
        this.enabled = enabled;
        this.status = status;
        this.shelfCode = shelfCode == null ? "" : shelfCode;
        this.updatedAt = updatedAt;
        this.reason = reason == null ? "" : reason;
    }

    public String pointId() { return pointId; }
    public String pointCode() { return pointCode; }
    public String alias() { return alias; }
    public PointRole role() { return role; }
    public boolean enabled() { return enabled; }
    public PointMaterialStatus status() { return status; }
    public String statusText() { return status.displayText(); }
    public String shelfCode() { return shelfCode; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public String reason() { return reason; }
    public boolean available() { return status == PointMaterialStatus.AVAILABLE; }
}
