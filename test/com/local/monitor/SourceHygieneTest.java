package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public final class SourceHygieneTest {
    private static final Path APP_SOURCE = Path.of("src", "com", "local", "monitor", "ShelfPointMonitorApp.java");

    private SourceHygieneTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(APP_SOURCE, StandardCharsets.UTF_8);

        assertAbsent(source, "WARNING - Removed try catching itself");
        assertAbsent(source, "possible behaviour change");
        assertAbsent(source, "new String[0]");
        assertAbsent(source, "new LinkOption[0]");
        assertAbsent(source, "new FileAttribute[0]");
        assertAbsent(source, "new OpenOption[0]");
        assertAbsent(source, "// empty catch block");
        assertAbsent(source, "this.pointRepository.fetch(dbConfig, this.currentPassword)");

        assertPatternAbsent(source, "\\bJPanel\\s+jPanel\\d*\\b");
        assertPatternAbsent(source, "\\bJButton\\s+jButton\\d*\\b");
        assertPatternAbsent(source, "\\bJLabel\\s+jLabel\\d*\\b");
        assertPatternAbsent(source, "\\bString\\s+string\\d*\\b");
        assertPatternAbsent(source, "\\bObject\\s+object\\d*\\b");
        assertPatternAbsent(source, "\\bchar\\[\\]\\s+cArray\\d*\\b");
        assertPatternAbsent(source, "\\bint\\s+n\\d*\\b");
        assertPatternAbsent(source, "catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}");

        System.out.println("SourceHygieneTest PASS");
    }

    private static void assertAbsent(String source, String forbiddenText) {
        if (source.contains(forbiddenText)) {
            throw new AssertionError("反编译式源码痕迹仍存在: " + forbiddenText);
        }
    }

    private static void assertPatternAbsent(String source, String forbiddenPattern) {
        if (Pattern.compile(forbiddenPattern).matcher(source).find()) {
            throw new AssertionError("反编译式变量名仍存在: " + forbiddenPattern);
        }
    }
}
