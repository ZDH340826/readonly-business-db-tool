package com.local.monitor;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExportService {
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvExportService() {
    }

    public static void writeUtf8(Path output, List<String> headers, List<List<String>> rows) throws Exception {
        Path parent = output.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream stream = Files.newOutputStream(output)) {
            stream.write(UTF8_BOM);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
                writeRow(writer, headers);
                for (List<String> row : rows) {
                    writeRow(writer, row);
                }
            }
        }
    }

    private static void writeRow(BufferedWriter writer, List<String> values) throws Exception {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escapeCell(values.get(index)));
        }
        writer.write("\r\n");
    }

    private static String escapeCell(String value) {
        String safe = value == null ? "" : protectFormula(value);
        boolean quote = safe.indexOf(',') >= 0
                || safe.indexOf('"') >= 0
                || safe.indexOf('\r') >= 0
                || safe.indexOf('\n') >= 0;
        String escaped = safe.replace("\"", "\"\"");
        return quote ? "\"" + escaped + "\"" : escaped;
    }

    private static String protectFormula(String value) {
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        if (index >= value.length()) {
            return value;
        }
        char first = value.charAt(index);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return value.substring(0, index) + "'" + value.substring(index);
        }
        return value;
    }
}
