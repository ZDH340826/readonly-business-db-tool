package com.local.monitor;

public final class SensitiveTextSanitizer {
    private SensitiveTextSanitizer() {
    }

    public static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String sanitized = text.replace('\r', ' ').replace('\n', ' ');
        sanitized = sanitized.replaceAll("\\bat\\s+[A-Za-z0-9_.$@/:-]+\\([^)]*\\)", "");
        sanitized = sanitized.replaceAll("[A-Za-z0-9_.$-]+\\.java:\\d+", "<代码位置>");
        sanitized = sanitized.replaceAll("(?i)jdbc:[^\\s,;]+", "<JDBC>");
        sanitized = sanitized.replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "<IP>");
        sanitized = sanitized.replaceAll("(?i)\\\\\\\\[^\\s,;]+", "<本地路径>");
        sanitized = sanitized.replaceAll("(?i)[A-Z]:[\\\\/][^\\r\\n]*", "<本地路径>");
        sanitized = sanitized.replaceAll("(?<![:A-Za-z0-9])/(?:[^\\s,;]+/)*[^\\s,;]+", "<本地路径>");
        sanitized = sanitized.replaceAll(
                "(?i)((?:password|passwd|pwd|token|access_token|secret)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        sanitized = sanitized.replaceAll(
                "(?i)\\b((?:password|passwd|pwd|token|access[_-]?token|secret))\\s+(?:is\\s+)?[^\\s,;]+", "$1 ***");
        sanitized = sanitized.replaceAll("(?i)\\bbearer\\s+[^\\s,;]+", "Bearer ***");
        sanitized = sanitized.replaceAll(
                "(?i)((?:user\\s*id|uid|user(?:name)?|databaseUser|db_user|role)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        sanitized = sanitized.replaceAll(
                "(?i)\\b((?:user\\s*id|uid|user(?:name)?|databaseUser|db_user))\\s+[^\\s,;]+", "$1 ***");
        sanitized = sanitized.replaceAll("(?i)((?:host|port)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        sanitized = sanitized.replaceAll(
                "(?i)for\\s+user\\s+(?:\"[^\"]+\"|'[^']+'|[A-Za-z0-9_.@-]+)", "for user ***");
        sanitized = sanitized.replaceAll(
                "(?i)user\\s+(?:\"[^\"]+\"|'[^']+'|[A-Za-z0-9_.@-]+)", "user ***");
        sanitized = sanitized.replaceAll("(?i)role\\s+\"[^\"]+\"", "role ***");
        sanitized = sanitized.replaceAll("(?i)role\\s+'[^']+'", "role ***");
        sanitized = sanitized.replaceAll("(?i)role\\s+[A-Za-z0-9_.@-]+", "role ***");
        sanitized = sanitized.replaceAll(
                "(?i)\\b(?:[A-Za-z0-9-]+\\.)+(?:internal|local|lan|corp|com|net|org|cn)\\b", "<主机名>");
        return sanitized.replaceAll("\\s+", " ").trim();
    }
}
