param([switch]$PackageOnly)
$ErrorActionPreference='Stop'
$repoRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$workspaceRoot=Split-Path $repoRoot -Parent
if(-not $env:JAVA_HOME){$env:JAVA_HOME=Join-Path $workspaceRoot '.tools/openjdk21/jdk-21.0.2'}
if(-not(Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))){throw 'Set JAVA_HOME to a JDK 21 installation.'}
if(-not $env:GRADLE_USER_HOME){$env:GRADLE_USER_HOME=Join-Path $workspaceRoot '.tools/gradle-cache'}
$gradle=Join-Path $workspaceRoot '.tools/gradle-9.4.0/bin/gradle.bat'
if(-not(Test-Path $gradle)){$gradle=Join-Path $repoRoot 'gradlew.bat'}
Push-Location $repoRoot
try{
 & node web/tools/audit-upstream.mjs;if($LASTEXITCODE){throw 'Official-source audit failed.'}
 $jsTests=@((Get-ChildItem web/tests -Filter '*.test.mjs').FullName)
 & node --test --test-isolation=none @jsTests;if($LASTEXITCODE){throw 'Browser/save tests failed.'}
 if(-not $PackageOnly){& $gradle -p web test buildWeb --no-daemon --console=plain;if($LASTEXITCODE){throw 'Game build/native-save tests failed.'}}
 & node --check web/build/dist/webapp/dungeon.js;if($LASTEXITCODE){throw 'Browser output syntax failed.'}
 & node web/tools/package.mjs;if($LASTEXITCODE){throw 'Packaging failed.'}
}finally{Pop-Location}
