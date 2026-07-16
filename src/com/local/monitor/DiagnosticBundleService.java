package com.local.monitor;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DiagnosticBundleService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private DiagnosticBundleService() {
    }

    public static Path create(Path appRoot, Path outputDir, String version) throws Exception {
        Path root = appRoot.toAbsolutePath().normalize();
        Path destination = outputDir.toAbsolutePath().normalize();
        Files.createDirectories(destination);
        Path zip = destination.resolve("diagnostic-" + FILE_TIME.format(LocalDateTime.now()) + ".zip");
        try (ZipOutputStream output = new ZipOutputStream(
                Files.newOutputStream(zip, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                StandardCharsets.UTF_8)) {
            writeEntry(output, "VERSION", SensitiveTextSanitizer.sanitize(version) + "\r\n");
            writeEntry(output, "preflight-latest.txt", preflightText(root, destination));
            writeEntry(output, "check-log.csv", sanitizedTail(root, root.resolve("logs/check-log.csv"), 1000));
            writeEntry(output, "event-log.csv", sanitizedTail(root, root.resolve("logs/event-log.csv"), 1000));
            writeEntry(output, "monitor.log", sanitizedTail(root, root.resolve("logs/monitor.log"), 200));
            writeEntry(output, "environment.txt", environmentText(version));
        }
        return zip;
    }

    private static String preflightText(Path root, Path outputDir) throws Exception {
        Path inOutput = outputDir.resolve("preflight-latest.txt");
        Path inRoot = root.resolve("preflight-latest.txt");
        Path source = Files.isRegularFile(inOutput) ? inOutput : inRoot;
        if (!Files.isRegularFile(source)) {
            return "未执行现场部署检查\r\n";
        }
        return sanitizedTail(root, source, 500);
    }

    private static String sanitizedTail(Path root, Path source, int limit) throws Exception {
        if (!isAllowedRegularFile(root, source)) {
            return "";
        }
        Deque<String> lines = new ArrayDeque<>();
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() == limit) {
                    lines.removeFirst();
                }
                lines.addLast(SensitiveTextSanitizer.sanitize(line));
            }
        }
        return String.join("\r\n", lines) + (lines.isEmpty() ? "" : "\r\n");
    }

    private static boolean isAllowedRegularFile(Path root, Path source) throws Exception {
        Path normalized = source.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || Files.isSymbolicLink(normalized) || !Files.isRegularFile(normalized)) {
            return false;
        }
        Path realRoot = root.toRealPath();
        return normalized.toRealPath().startsWith(realRoot);
    }

    private static String environmentText(String version) {
        List<String> lines = new ArrayList<>();
        lines.add("软件版本=" + SensitiveTextSanitizer.sanitize(version));
        lines.add("Java版本=" + SensitiveTextSanitizer.sanitize(System.getProperty("java.version", "未知")));
        lines.add("操作系统=" + SensitiveTextSanitizer.sanitize(System.getProperty("os.name", "未知")));
        lines.add("操作系统版本=" + SensitiveTextSanitizer.sanitize(System.getProperty("os.version", "未知")));
        lines.add("系统架构=" + SensitiveTextSanitizer.sanitize(System.getProperty("os.arch", "未知")));
        lines.add("生成时间=" + LocalDateTime.now());
        return String.join("\r\n", lines) + "\r\n";
    }

    private static void writeEntry(ZipOutputStream output, String name, String text) throws Exception {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        output.putNextEntry(entry);
        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
