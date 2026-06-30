package com.local.monitor;

import java.time.LocalDateTime;

public final class PointRecord {
    private final String mapDataCode;
    private final String podCode;
    private final String podStatus;
    private final int status;
    private final int indLock;
    private final String areaCode;
    private final String relateAreaCode;
    private final LocalDateTime dateChg;
    private final LocalDateTime markDate;

    public PointRecord(
            String mapDataCode,
            String podCode,
            String podStatus,
            int status,
            int indLock,
            String areaCode,
            String relateAreaCode,
            LocalDateTime dateChg,
            LocalDateTime markDate) {
        this.mapDataCode = mapDataCode;
        this.podCode = podCode;
        this.podStatus = podStatus;
        this.status = status;
        this.indLock = indLock;
        this.areaCode = areaCode;
        this.relateAreaCode = relateAreaCode;
        this.dateChg = dateChg;
        this.markDate = markDate;
    }

    public String mapDataCode() {
        return mapDataCode;
    }

    public String podCode() {
        return podCode;
    }

    public String podStatus() {
        return podStatus;
    }

    public int status() {
        return status;
    }

    public int indLock() {
        return indLock;
    }

    public String areaCode() {
        return areaCode;
    }

    public String relateAreaCode() {
        return relateAreaCode;
    }

    public LocalDateTime dateChg() {
        return dateChg;
    }

    public LocalDateTime markDate() {
        return markDate;
    }
}


