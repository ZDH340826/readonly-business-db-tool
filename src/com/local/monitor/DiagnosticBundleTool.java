package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DiagnosticBundleTool {
    private DiagnosticBundleTool() {
    }

    public static void main(String[] args) {
        Path root = args.length == 0 ? Path.of("") : Path.of(args[0]);
        try {
            Path zip = create(root);
            System.out.println("诊断包已生成：" + zip.getFileName());
        } catch (Exception exception) {
            System.err.println("诊断包生成失败：" + SensitiveTextSanitizer.sanitize(exception.getMessage()));
            System.exit(2);
        }
    }

    static Path create(Path appRoot) throws Exception {
        Path root = appRoot.toAbsolutePath().normalize();
        String version = Files.readString(root.resolve("VERSION"), StandardCharsets.UTF_8).trim();
        return DiagnosticBundleService.create(root, root.resolve("diagnostics"), version);
    }
}
