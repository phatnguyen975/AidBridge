#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Fix VS Code Java Language Server cache issues
.DESCRIPTION
    Cleans Gradle build cache and VS Code workspace to force IDE re-index
#>

Write-Host "=== Fixing VS Code Java Language Server Cache ===" -ForegroundColor Cyan

# Step 1: Clean Gradle build
Write-Host "`n[1/4] Cleaning Gradle build..." -ForegroundColor Yellow
& .\gradlew.bat clean --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Gradle clean failed" -ForegroundColor Red
    exit 1
}

# Step 2: Delete Gradle cache
Write-Host "`n[2/4] Removing Gradle cache directories..." -ForegroundColor Yellow
$cacheDirs = @(
    "build",
    ".gradle/build-cache"
)
foreach ($dir in $cacheDirs) {
    if (Test-Path $dir) {
        Write-Host "  Removing: $dir" -ForegroundColor Gray
        Remove-Item -Recurse -Force $dir -ErrorAction SilentlyContinue
    }
}

# Step 3: Compile Java
Write-Host "`n[3/4] Compiling Java sources..." -ForegroundColor Yellow
& .\gradlew.bat compileJava --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Compilation failed" -ForegroundColor Red
    exit 1
}

# Step 4: Instructions for VS Code
Write-Host "`n[4/4] Next steps for VS Code:" -ForegroundColor Yellow
Write-Host "  1. Press Ctrl+Shift+P" -ForegroundColor White
Write-Host "  2. Type: Java: Clean Java Language Server Workspace" -ForegroundColor White
Write-Host "  3. Select 'Reload and delete'" -ForegroundColor White
Write-Host "  4. Wait for VS Code to reload and re-index" -ForegroundColor White

Write-Host "`n✅ Gradle build complete!" -ForegroundColor Green
Write-Host "   Now clean the Java Language Server workspace in VS Code" -ForegroundColor Green
