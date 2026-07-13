package com.local.monitor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WindowsLauncherScriptTest {
    private WindowsLauncherScriptTest() {
    }

    public static void main(String[] args) throws Exception {
        buildGeneratesThreeSafeChineseLaunchers();
        System.out.println("WindowsLauncherScriptTest passed");
    }

    private static void buildGeneratesThreeSafeChineseLaunchers() throws Exception {
        String build = Files.readString(Path.of("build.ps1"), StandardCharsets.UTF_8);
        assertContains(build, "启动工具.bat", "必须生成中文启动脚本");
        assertContains(build, "现场部署检查.bat", "必须生成中文预检脚本");
        assertContains(build, "生成诊断包.bat", "必须生成中文诊断脚本");
        assertContains(build, "%~dp0", "脚本必须以自身目录为根");
        assertContains(build, "runtime\\bin\\java.exe", "脚本必须优先内嵌运行时");
        assertContains(build, "where java", "脚本必须显式检查系统 Java 回退");
        assertContains(build, "%ERRORLEVEL%", "脚本必须保留子进程退出码");
        assertContains(build, "exit /b", "脚本必须把退出码返回调用者");
        assertContains(build, "未找到可用的 Java", "脚本必须输出中文运行时错误");
        assertContains(build, "UTF8Encoding]::new($false)", "批处理必须写为 UTF-8 无 BOM");
        assertContains(build, "`r`n", "批处理必须统一为 Windows CRLF 换行");
        assertContains(build, "Get-FileHash", "构建必须计算发布 ZIP 的 SHA-256");
        assertContains(build, "SHA256SUMS.txt", "构建必须生成哈希清单");
        assertNotContains(build, "password=", "脚本生成内容不得含密码");
    }

    private static void assertContains(String text, String expected, String message) {
        if (!text.contains(expected)) {
            throw new AssertionError(message + "，缺少：" + expected);
        }
    }

    private static void assertNotContains(String text, String unexpected, String message) {
        if (text.toLowerCase(java.util.Locale.ROOT).contains(unexpected.toLowerCase(java.util.Locale.ROOT))) {
            throw new AssertionError(message + "，发现：" + unexpected);
        }
    }
}
