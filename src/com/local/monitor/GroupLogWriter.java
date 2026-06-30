package com.local.monitor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public final class GroupLogWriter {
    private static final List<String> CHECK_HEADER = List.of(
            "checked_at",
            "group_id",
            "area_name",
            "group_name",
            "material_name",
            "status",
            "use_point_empty",
            "backup_total",
            "backup_available",
            "backup_missing",
            "rule_matched",
            "continuous_minutes",
            "message");
    private static final List<String> EVENT_HEADER = List.of(
            "event_at",
            "event_type",
            "group_id",
            "area_name",
            "group_name",
            "material_name",
            "status",
            "message");

    private final Path logDir;

    public GroupLogWriter(Path logDir) {
        this.logDir = logDir;
    }

    public void appendCheck(LocalDateTime checkedAt, GroupEvaluation evaluation) throws IOException {
        append(logDir.resolve("check-log.csv"), CHECK_HEADER, List.of(
                checkedAt.toString(),
                evaluation.groupId(),
                evaluation.areaName(),
                evaluation.groupName(),
                evaluation.materialName(),
                evaluation.status().name(),
                String.valueOf(evaluation.usePointEmpty()),
                String.valueOf(evaluation.backupTotal()),
                String.valueOf(evaluation.backupAvailableCount()),
                String.valueOf(evaluation.backupMissingCount()),
                String.valueOf(evaluation.ruleMatched()),
                String.valueOf(evaluation.continuousMatchedMinutes()),
                evaluation.message()));
    }

    public void appendEvent(LocalDateTime eventAt, String eventType, GroupEvaluation evaluation) throws IOException {
        append(logDir.resolve("event-log.csv"), EVENT_HEADER, List.of(
                eventAt.toString(),
                eventType,
                evaluation.groupId(),
                evaluation.areaName(),
                evaluation.groupName(),
                evaluation.materialName(),
                evaluation.status().name(),
                evaluation.message()));
    }

    private static void append(Path path, List<String> header, List<String> row) throws IOException {
        Files.createDirectories(path.getParent());
        boolean writeHeader = !Files.exists(path) || Files.size(path) == 0L;
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (writeHeader) {
                writer.write(csvLine(header));
                writer.newLine();
            }
            writer.write(csvLine(row));
            writer.newLine();
        }
    }

    private static String csvLine(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(escape(value));
        }
        return builder.toString();
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        boolean quoted = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        if (!quoted) {
            return safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
