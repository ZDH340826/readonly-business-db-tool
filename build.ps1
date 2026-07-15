$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$versionFile = Join-Path $root 'VERSION'
if (!(Test-Path $versionFile)) {
    throw "VERSION file is required"
}
$version = (Get-Content -Raw -Path $versionFile).Trim()
if ($version -notmatch '^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$') {
    throw "Invalid VERSION value: $version"
}
$src = Join-Path $root 'src'
$test = Join-Path $root 'test'
$build = Join-Path $root 'build'
$classes = Join-Path $build 'classes'
$testClasses = Join-Path $build 'test-classes'
$dist = Join-Path $root 'dist\ShelfPointMonitor'
$lib = Join-Path $root 'lib'
$driverJar = Join-Path $lib 'postgresql-42.2.25.jar'
$h2Jar = Join-Path $lib 'h2-2.2.224.jar'
$postgresUri = 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.25/postgresql-42.2.25.jar'
$h2Uri = 'https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar'

function Resolve-JavaTool($name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$name"
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    $command = Get-Command $name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    throw "Cannot find $name. Install JDK 17+ or set JAVA_HOME."
}

$javac = Resolve-JavaTool 'javac.exe'
$jar = Resolve-JavaTool 'jar.exe'
$java = Resolve-JavaTool 'java.exe'
$jlinkCommand = Get-Command 'jlink.exe' -ErrorAction SilentlyContinue
$jlinkPath = if ($jlinkCommand) { $jlinkCommand.Source } else { $null }
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jlink.exe'))) {
    $jlinkPath = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
}

New-Item -ItemType Directory -Force -Path $classes, $testClasses, $lib | Out-Null
if (!(Test-Path $driverJar)) {
    Invoke-WebRequest -UseBasicParsing -Uri $postgresUri -OutFile $driverJar -TimeoutSec 60
}
if (!(Test-Path $h2Jar)) {
    Invoke-WebRequest -UseBasicParsing -Uri $h2Uri -OutFile $h2Jar -TimeoutSec 60
}

$sourceFiles = Get-ChildItem -Path $src -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -cp "$driverJar;$h2Jar" -d $classes $sourceFiles
if ($LASTEXITCODE -ne 0) { throw "javac source failed with exit code $LASTEXITCODE" }

$testFiles = Get-ChildItem -Path $test -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -cp "$driverJar;$h2Jar;$classes" -d $testClasses $testFiles
if ($LASTEXITCODE -ne 0) { throw "javac test failed with exit code $LASTEXITCODE" }

& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.MonitorLogicTest
if ($LASTEXITCODE -ne 0) { throw "tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.GroupMonitorLogicTest
if ($LASTEXITCODE -ne 0) { throw "group logic tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.GroupCheckPlannerTest
if ($LASTEXITCODE -ne 0) { throw "group check planner tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PointStatusMapperTest
if ($LASTEXITCODE -ne 0) { throw "point status mapper tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.LocalTestDatabaseTest
if ($LASTEXITCODE -ne 0) { throw "local database tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PointScheduleTest
if ($LASTEXITCODE -ne 0) { throw "point schedule tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ConfigStoreTest
if ($LASTEXITCODE -ne 0) { throw "config store tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.GroupConfigStoreTest
if ($LASTEXITCODE -ne 0) { throw "group config store tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.GroupLogWriterTest
if ($LASTEXITCODE -ne 0) { throw "group log writer tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ConnectionProfileStoreTest
if ($LASTEXITCODE -ne 0) { throw "connection profile tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.DbMetadataRepositoryTest
if ($LASTEXITCODE -ne 0) { throw "metadata repository tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PointDataQueryRepositoryTest
if ($LASTEXITCODE -ne 0) { throw "point data query tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.UiPreferencesStoreTest
if ($LASTEXITCODE -ne 0) { throw "ui preferences tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ReadableTableTest
if ($LASTEXITCODE -ne 0) { throw "readable table tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PinnedColumnLayoutTest
if ($LASTEXITCODE -ne 0) { throw "pinned column layout tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.TableColumnLayoutStoreTest
if ($LASTEXITCODE -ne 0) { throw "table column layout store tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PinnedTablePaneTest
if ($LASTEXITCODE -ne 0) { throw "pinned table pane tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.AppThemeTest
if ($LASTEXITCODE -ne 0) { throw "app theme tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.AppShellTest
if ($LASTEXITCODE -ne 0) { throw "app shell tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.OverviewAlertPageTest
if ($LASTEXITCODE -ne 0) { throw "overview and alert page tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.GroupConnectionPageTest
if ($LASTEXITCODE -ne 0) { throw "group and connection page tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.CsvExportServiceTest
if ($LASTEXITCODE -ne 0) { throw "CSV export service tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.DataPagesTest
if ($LASTEXITCODE -ne 0) { throw "data page tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.DiagnosticBundleServiceTest
if ($LASTEXITCODE -ne 0) { throw "diagnostic bundle tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.LogsSettingsPageTest
if ($LASTEXITCODE -ne 0) { throw "logs and settings page tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.FieldDeliveryScenarioTest
if ($LASTEXITCODE -ne 0) { throw "field delivery scenario tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.WindowsPathPackagingTest
if ($LASTEXITCODE -ne 0) { throw "Windows path packaging tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.FieldDeploymentPreflightTest
if ($LASTEXITCODE -ne 0) { throw "field deployment preflight tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.WindowsLauncherScriptTest
if ($LASTEXITCODE -ne 0) { throw "Windows launcher script tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.MonitoringSessionTest
if ($LASTEXITCODE -ne 0) { throw "monitoring session tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.MonitoringSessionRaceTest
if ($LASTEXITCODE -ne 0) { throw "monitoring session race tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ErrorSanitizationTest
if ($LASTEXITCODE -ne 0) { throw "error sanitization tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.SensitiveDataSanitizationTest
if ($LASTEXITCODE -ne 0) { throw "sensitive data sanitization tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ReadOnlyConnectionTest
if ($LASTEXITCODE -ne 0) { throw "read-only connection tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ExecutorSeparationTest
if ($LASTEXITCODE -ne 0) { throw "executor separation tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.SystemHealthStatusTest
if ($LASTEXITCODE -ne 0) { throw "system health status tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.SourceHygieneTest
if ($LASTEXITCODE -ne 0) { throw "source hygiene tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ShelfPointMonitorAppUiTest
if ($LASTEXITCODE -ne 0) { throw "ui layout tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ShelfPointMonitorSelfTestTest
if ($LASTEXITCODE -ne 0) { throw "self-test validation tests failed with exit code $LASTEXITCODE" }

if (Test-Path $dist) {
    Remove-Item -LiteralPath $dist -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dist, (Join-Path $dist 'lib'), (Join-Path $dist 'data'), (Join-Path $dist 'logs'), (Join-Path $dist 'diagnostics') | Out-Null
Set-Content -Encoding ASCII -Path (Join-Path $dist 'VERSION') -Value $version

& $jar --create --file (Join-Path $dist 'ShelfPointMonitor.jar') --main-class com.local.monitor.ShelfPointMonitorApp -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }
Copy-Item -LiteralPath $driverJar -Destination (Join-Path $dist 'lib\postgresql-42.2.25.jar') -Force
Copy-Item -LiteralPath $h2Jar -Destination (Join-Path $dist 'lib\h2-2.2.224.jar') -Force
Copy-Item -LiteralPath (Join-Path $root 'docs\manuals\现场运维交付手册.md') -Destination (Join-Path $dist '现场运维交付手册.md') -Force
Copy-Item -LiteralPath (Join-Path $root 'docs\ops\现场验收清单.md') -Destination (Join-Path $dist '现场验收清单.md') -Force
Copy-Item -LiteralPath (Join-Path $root 'docs\ops\回滚说明.md') -Destination (Join-Path $dist '回滚说明.md') -Force

if ($version -match '-rc' -and !$jlinkPath) {
    throw "RC packaging requires jlink so the field package has an embedded runtime."
}
if ($jlinkPath) {
    & $jlinkPath `
        --add-modules java.desktop,java.sql,java.naming,java.logging,java.management,java.security.sasl,jdk.crypto.ec,jdk.charsets `
        --strip-debug `
        --no-header-files `
        --no-man-pages `
        --output (Join-Path $dist 'runtime')
    if ($LASTEXITCODE -ne 0) { throw "jlink failed with exit code $LASTEXITCODE" }
}

@'
@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" (
  where java >nul 2>nul
  if errorlevel 1 (
    echo [失败] 未找到可用的 Java，请确认发布包完整。
    endlocal & exit /b 2
  )
  set "JAVA_EXE=java"
)
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -cp "ShelfPointMonitor.jar;lib\postgresql-42.2.25.jar;lib\h2-2.2.224.jar" com.local.monitor.ShelfPointMonitorApp %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
'@ | Set-Content -Encoding UTF8 -Path (Join-Path $dist '启动工具.bat')

@'
@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" (
  where java >nul 2>nul
  if errorlevel 1 (
    echo [失败] 未找到可用的 Java，请确认发布包完整。
    endlocal & exit /b 2
  )
  set "JAVA_EXE=java"
)
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -cp "ShelfPointMonitor.jar;lib\postgresql-42.2.25.jar;lib\h2-2.2.224.jar" com.local.monitor.FieldDeploymentPreflight "%~dp0."
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
'@ | Set-Content -Encoding UTF8 -Path (Join-Path $dist '现场部署检查.bat')

@'
@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" (
  where java >nul 2>nul
  if errorlevel 1 (
    echo [失败] 未找到可用的 Java，请确认发布包完整。
    endlocal & exit /b 2
  )
  set "JAVA_EXE=java"
)
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -cp "ShelfPointMonitor.jar;lib\postgresql-42.2.25.jar;lib\h2-2.2.224.jar" com.local.monitor.DiagnosticBundleTool "%~dp0."
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
'@ | Set-Content -Encoding UTF8 -Path (Join-Path $dist '生成诊断包.bat')

foreach ($scriptPath in @(
        (Join-Path $dist '启动工具.bat'),
        (Join-Path $dist '现场部署检查.bat'),
        (Join-Path $dist '生成诊断包.bat'))) {
    $scriptText = [System.IO.File]::ReadAllText($scriptPath, [System.Text.Encoding]::UTF8)
    $scriptText = $scriptText -replace "`r?`n", "`r`n"
    [System.IO.File]::WriteAllText($scriptPath, $scriptText, [System.Text.UTF8Encoding]::new($false))
}

@'
host=__SITE_HOST__
dbType=postgres
localPath=data/local-test-db
port=2345
database=cms_web
schema=public
user=__SITE_USER__
sslmode=disable
intervalSeconds=10
points=Use=USE_POINT_001=1;Backup 1=BACKUP_POINT_001=10;Backup 2=BACKUP_POINT_002=10;Backup 3=BACKUP_POINT_003=10;Backup 4=BACKUP_POINT_004=10
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'data\config.properties')

@'
currentProfile=prod
profile.count=2
profile.0.id=prod
profile.0.name=\u73b0\u573a\u6570\u636e\u5e93
profile.0.dbType=postgres
profile.0.host=__SITE_HOST__
profile.0.port=2345
profile.0.database=cms_web
profile.0.schema=public
profile.0.user=__SITE_USER__
profile.0.sslmode=disable
profile.0.localPath=data/local-test-db
profile.1.id=local
profile.1.name=\u672c\u5730\u6d4b\u8bd5\u5e93
profile.1.dbType=h2
profile.1.host=local
profile.1.port=1
profile.1.database=local-test
profile.1.schema=public
profile.1.user=sa
profile.1.sslmode=disable
profile.1.localPath=data/local-test-db
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'data\connections.properties')

$runtimeJava = Join-Path $dist 'runtime\bin\java.exe'
if (!(Test-Path $runtimeJava)) {
    $runtimeJava = $java
}
& $runtimeJava -cp "$dist\ShelfPointMonitor.jar;$dist\lib\h2-2.2.224.jar" com.local.monitor.LocalTestDbTool reset (Join-Path $dist 'data\local-test-db') (Join-Path $dist 'data\group-config.properties')
if ($LASTEXITCODE -ne 0) { throw "local test database creation failed with exit code $LASTEXITCODE" }

& $runtimeJava "-Dshelf.monitor.appRoot=$dist" -cp "$dist\ShelfPointMonitor.jar;$dist\lib\postgresql-42.2.25.jar;$dist\lib\h2-2.2.224.jar" com.local.monitor.ShelfPointMonitorApp --self-test
if ($LASTEXITCODE -ne 0) { throw "packaged self-test failed with exit code $LASTEXITCODE" }
& $runtimeJava -cp "$dist\ShelfPointMonitor.jar;$dist\lib\postgresql-42.2.25.jar;$dist\lib\h2-2.2.224.jar" com.local.monitor.FieldDeploymentPreflight $dist
if ($LASTEXITCODE -ne 0) { throw "packaged preflight failed with exit code $LASTEXITCODE" }
& $runtimeJava -cp "$dist\ShelfPointMonitor.jar;$dist\lib\postgresql-42.2.25.jar;$dist\lib\h2-2.2.224.jar" com.local.monitor.DiagnosticBundleTool $dist
if ($LASTEXITCODE -ne 0) { throw "packaged diagnostic tool failed with exit code $LASTEXITCODE" }
Get-ChildItem -LiteralPath (Join-Path $dist 'diagnostics') -Filter 'diagnostic-*.zip' -File | Remove-Item -Force

Get-ChildItem -Path (Join-Path $root 'dist') -Filter '*.zip' -File | Remove-Item -Force
$zip = Join-Path $root ("dist\ReadonlyBusinessDbTool-v$version.zip")
Compress-Archive -Path $dist -DestinationPath $zip -Force
$zipName = [System.IO.Path]::GetFileName($zip)
$zipHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $zip).Hash.ToUpperInvariant()
$hashFile = Join-Path $root 'dist\SHA256SUMS.txt'
$hashLine = "$zipHash  $zipName"
[System.IO.File]::WriteAllText($hashFile, $hashLine + "`r`n", [System.Text.Encoding]::ASCII)
if ((Get-Content -Raw -LiteralPath $hashFile).Trim() -ne $hashLine) {
    throw "SHA256SUMS.txt verification failed."
}

Write-Host "Built: $dist"
Write-Host "Version: $version"
Write-Host "Zip:   $zip"
Write-Host "SHA256: $zipHash"


