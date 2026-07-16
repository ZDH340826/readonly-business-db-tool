package com.local.monitor;

public final class ErrorSanitizationTest {
    private static final String[] UNSAFE_MESSAGES = {
            "FATAL: password authentication failed for user \"readonly_user\"",
            "FATAL: password authentication failed for user readonly_user",
            "jdbc:postgresql://192.0.2.10:2345/cms_web?user=readonly_user&password=Secret-123",
            "Access denied for user 'readonly_user'@'192.0.2.10'",
            "role \"readonly_user\" does not exist",
            "uid=readonly_user password=Secret-123 token=Token-456 secret=Secret-789 Bearer Bearer-999",
            "C:\\Users\\operator\\AppData\\Local\\tool\\monitor.log",
            "C:\\Program Files\\Shelf Monitor\\monitor.log",
            "C:/Users/operator/AppData/Local/tool/monitor.log",
            "at com.local.monitor.PointRepository.fetch(PointRepository.java:24)",
            "at java.base/java.lang.Thread.run(Thread.java:840)"
    };

    public static void main(String[] args) {
        userVisibleErrorUsesSanitizedSummary();
        throwableUsesSameSanitizedSummary();
        userVisibleErrorKeepsChineseOperationalCategory();
        queryFailureMessageKeepsCategoryAndHidesSecrets();
        visibleLogSanitizerCoversMonitorLogInputs();
        System.out.println("ErrorSanitizationTest PASS");
    }

    private static void userVisibleErrorUsesSanitizedSummary() {
        String text = ShelfPointMonitorApp.userVisibleErrorMessage(new IllegalStateException(joinUnsafeMessages()));
        assertSafe(text, "user visible error");
    }

    private static void throwableUsesSameSanitizedSummary() {
        String text = ShelfPointMonitorApp.userVisibleErrorMessage(new AssertionError(joinUnsafeMessages()));
        assertSafe(text, "throwable user visible error");
    }

    private static void userVisibleErrorKeepsChineseOperationalCategory() {
        assertContains(
                ShelfPointMonitorApp.userVisibleErrorMessage(new IllegalStateException("connection timeout")),
                "连接超时",
                "timeout should retain a Chinese operational category");
        assertContains(
                ShelfPointMonitorApp.userVisibleErrorMessage(
                        new IllegalStateException("password authentication failed for user \"readonly_user\"")),
                "认证失败",
                "authentication failure should retain a Chinese operational category");
        assertContains(
                ShelfPointMonitorApp.userVisibleErrorMessage(new IllegalStateException("connection refused")),
                "连接被拒绝",
                "connection refusal should retain a Chinese operational category");
    }

    private static void queryFailureMessageKeepsCategoryAndHidesSecrets() {
        String text = ShelfPointMonitorApp.queryFailureMessage(new IllegalStateException(joinUnsafeMessages()));
        assertContains(text, "查询失败", "query failure message should keep useful Chinese category");
        assertSafe(text, "query failure message");
    }

    private static void visibleLogSanitizerCoversMonitorLogInputs() {
        String text = ShelfPointMonitorApp.sanitizeVisibleLog("查询失败 " + joinUnsafeMessages());
        assertContains(text, "查询失败", "monitor log text should keep useful category");
        assertSafe(text, "monitor log text");
    }

    private static String joinUnsafeMessages() {
        return String.join(" ; ", UNSAFE_MESSAGES);
    }

    private static void assertSafe(String text, String context) {
        assertNotContains(text, "readonly_user", context + " must not contain database username");
        assertNotContains(text, "Secret-123", context + " must not contain password");
        assertNotContains(text, "Token-456", context + " must not contain token");
        assertNotContains(text, "Secret-789", context + " must not contain secret");
        assertNotContains(text, "Bearer-999", context + " must not contain bearer token");
        assertNotContains(text, "192.0.2.10", context + " must not contain IPv4 address");
        assertNotContains(text, "jdbc:postgresql:", context + " must not contain full JDBC URL prefix");
        assertNotContains(text, "C:\\Users\\", context + " must not contain local absolute path");
        assertNotContains(text, "C:\\Program Files", context + " must not contain local absolute path with spaces");
        assertNotContains(text, "C:/Users/", context + " must not contain slash-style local absolute path");
        assertNotContains(text, "PointRepository.java:24", context + " must not contain Java stack frame location");
        assertNotContains(text, "java.base/java.lang.Thread.run", context + " must not contain Java module stack frame");
    }

    private static void assertContains(String text, String expected, String message) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + text);
        }
    }

    private static void assertNotContains(String text, String unexpected, String message) {
        if (text != null && text.contains(unexpected)) {
            throw new AssertionError(message + " unexpected=" + unexpected + " actual=" + text);
        }
    }
}
