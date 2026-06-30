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
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.LocalTestDatabaseTest
if ($LASTEXITCODE -ne 0) { throw "local database tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.PointScheduleTest
if ($LASTEXITCODE -ne 0) { throw "point schedule tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ConfigStoreTest
if ($LASTEXITCODE -ne 0) { throw "config store tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ConnectionProfileStoreTest
if ($LASTEXITCODE -ne 0) { throw "connection profile tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.DbMetadataRepositoryTest
if ($LASTEXITCODE -ne 0) { throw "metadata repository tests failed with exit code $LASTEXITCODE" }
& $java -cp "$driverJar;$h2Jar;$classes;$testClasses" com.local.monitor.ShelfPointMonitorAppUiTest
if ($LASTEXITCODE -ne 0) { throw "ui layout tests failed with exit code $LASTEXITCODE" }

if (Test-Path $dist) {
    Remove-Item -LiteralPath $dist -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $dist, (Join-Path $dist 'lib'), (Join-Path $dist 'data'), (Join-Path $dist 'logs') | Out-Null
Set-Content -Encoding ASCII -Path (Join-Path $dist 'VERSION') -Value $version

& $jar --create --file (Join-Path $dist 'ShelfPointMonitor.jar') --main-class com.local.monitor.ShelfPointMonitorApp -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }
Copy-Item -LiteralPath $driverJar -Destination (Join-Path $dist 'lib\postgresql-42.2.25.jar') -Force
Copy-Item -LiteralPath $h2Jar -Destination (Join-Path $dist 'lib\h2-2.2.224.jar') -Force

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
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -cp "ShelfPointMonitor.jar;lib\postgresql-42.2.25.jar;lib\h2-2.2.224.jar" com.local.monitor.ShelfPointMonitorApp
endlocal
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'ShelfPointMonitor.bat')

@'
@echo off
setlocal
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -cp "ShelfPointMonitor.jar;lib\h2-2.2.224.jar" com.local.monitor.LocalTestDbTool reset
pause
endlocal
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'LocalTest_Reset.bat')

@'
@echo off
setlocal
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -cp "ShelfPointMonitor.jar;lib\h2-2.2.224.jar" com.local.monitor.LocalTestDbTool normal
pause
endlocal
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'LocalTest_Normal.bat')

@'
@echo off
setlocal
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -cp "ShelfPointMonitor.jar;lib\h2-2.2.224.jar" com.local.monitor.LocalTestDbTool missing-use
pause
endlocal
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'LocalTest_MissingUse.bat')

@'
@echo off
setlocal
cd /d "%~dp0"
set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
"%JAVA_EXE%" -cp "ShelfPointMonitor.jar;lib\h2-2.2.224.jar" com.local.monitor.LocalTestDbTool missing-backup
pause
endlocal
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'LocalTest_MissingBackup.bat')

@'
host=127.0.0.1
dbType=postgres
localPath=data/local-test-db
port=5432
database=example_db
schema=public
user=readonly_user
sslmode=disable
intervalSeconds=10
points=\u4f7f\u7528\u4f4d=USE_POINT_001=1;\u5907\u7528\u4f4d=BACKUP_POINT_001=10
'@ | Set-Content -Encoding ASCII -Path (Join-Path $dist 'data\config.properties')

@'
currentProfile=prod
profile.count=2
profile.0.id=prod
profile.0.name=\u793a\u4f8b PostgreSQL
profile.0.dbType=postgres
profile.0.host=127.0.0.1
profile.0.port=5432
profile.0.database=example_db
profile.0.schema=public
profile.0.user=readonly_user
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
& $runtimeJava -cp "$dist\ShelfPointMonitor.jar;$dist\lib\h2-2.2.224.jar" com.local.monitor.LocalTestDbTool reset (Join-Path $dist 'data\local-test-db')
if ($LASTEXITCODE -ne 0) { throw "local test database creation failed with exit code $LASTEXITCODE" }

Get-ChildItem -Path (Join-Path $root 'dist') -Filter '*.zip' -File | Remove-Item -Force
$zip = Join-Path $root ("dist\ReadonlyBusinessDbTool-v$version.zip")
Compress-Archive -Path $dist -DestinationPath $zip -Force

Write-Host "Built: $dist"
Write-Host "Version: $version"
Write-Host "Zip:   $zip"


