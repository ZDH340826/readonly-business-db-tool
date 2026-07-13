package com.local.monitor;

public final class SensitiveDataSanitizationTest {
    private SensitiveDataSanitizationTest() {
    }

    public static void main(String[] args) {
        visibleLogRemovesEveryFieldSensitiveClass();
        visibleErrorKeepsCategoryWithoutLeakingDetails();
        System.out.println("SensitiveDataSanitizationTest PASS");
    }

    private static void visibleLogRemovesEveryFieldSensitiveClass() {
        String raw = "connection timeout user=demo password=Demo-Secret "
                + "jdbc:postgresql://192.0.2.10:5432/demo "
                + "host=database.example.internal "
                + "C:/example/site/config.properties "
                + "at demo.Driver.run(Driver.java:42)";

        String safe = ShelfPointMonitorApp.sanitizeVisibleLog(raw);

        assertSafe(safe, "visible log");
    }

    private static void visibleErrorKeepsCategoryWithoutLeakingDetails() {
        String raw = "connection timeout for user demo at jdbc:postgresql://192.0.2.10:5432/demo "
                + "password=Demo-Secret C:\\example\\site\\config.properties "
                + "at demo.Driver.run(Driver.java:42)";

        String safe = ShelfPointMonitorApp.userVisibleErrorMessage(new IllegalStateException(raw));

        assertContains(safe, "连接超时", "operator category must remain visible");
        assertSafe(safe, "visible error");
    }

    private static void assertSafe(String text, String context) {
        assertNotContains(text, "demo", context + " must hide usernames and database names");
        assertNotContains(text, "Demo-Secret", context + " must hide passwords");
        assertNotContains(text, "192.0.2.10", context + " must hide IPv4 addresses");
        assertNotContains(text, "database.example.internal", context + " must hide hostnames");
        assertNotContains(text, "jdbc:postgresql:", context + " must hide JDBC URLs");
        assertNotContains(text, "C:/example/", context + " must hide slash-style absolute paths");
        assertNotContains(text, "C:\\example\\", context + " must hide Windows absolute paths");
        assertNotContains(text, "demo.Driver.run", context + " must hide Java frames");
        assertNotContains(text, "Driver.java:42", context + " must hide source locations");
    }

    private static void assertContains(String text, String expected, String message) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + text);
        }
    }

    private static void assertNotContains(String text, String unexpected, String message) {
        if (text != null && text.toLowerCase(java.util.Locale.ROOT)
                .contains(unexpected.toLowerCase(java.util.Locale.ROOT))) {
            throw new AssertionError(message + " unexpected=" + unexpected + " actual=" + text);
        }
    }
}
