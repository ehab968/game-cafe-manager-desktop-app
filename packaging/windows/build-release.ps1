[CmdletBinding()]
param(
    [ValidateSet("AppImage", "Installer", "All")]
    [string]$PackageType = "All",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$applicationName = "GameCafeManager"
$moduleName = "com.gamecafe.gamecafemanager"
$mainClass = "com.gamecafe.gamecafemanager.presentation.GameCafeApplication"
$vendor = "Ehab Salah"
$upgradeUuid = "5f60dbf0-5d34-4db2-8f24-39d29e2481e9"
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory "..\.."))
$targetDirectory = Join-Path $projectRoot "target"
$workDirectory = Join-Path $targetDirectory "jpackage"
$dependencyDirectory = Join-Path $workDirectory "dependencies"
$modulePathDirectory = Join-Path $workDirectory "module-path"
$releaseDirectory = Join-Path $projectRoot "release\windows"
$appImageParent = Join-Path $releaseDirectory "app-image"
$appImageDirectory = Join-Path $appImageParent $applicationName
$installerWorkDirectory = Join-Path $workDirectory "installer"
$iconPath = Join-Path $scriptDirectory "assets\GameCafeManager.ico"
$pomPath = Join-Path $projectRoot "pom.xml"

function Assert-PathWithinProject {
    param([Parameter(Mandatory = $true)][string]$Path)

    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    $projectPrefix = $projectRoot.TrimEnd('\') + '\'
    if (-not $resolvedPath.StartsWith(
            $projectPrefix,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify a path outside the project: $resolvedPath"
    }
}

function Remove-ProjectDirectory {
    param([Parameter(Mandatory = $true)][string]$Path)

    Assert-PathWithinProject -Path $Path
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

function Find-Executable {
    param(
        [Parameter(Mandatory = $true)][string[]]$Names,
        [string[]]$Fallbacks = @()
    )

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($null -ne $command) {
            return $command.Source
        }
    }
    foreach ($fallback in $Fallbacks) {
        if (Test-Path -LiteralPath $fallback -PathType Leaf) {
            return $fallback
        }
    }
    return $null
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$Executable,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Executable"
    }
}

function Find-WixDirectory {
    $wixCommand = Get-Command wix.exe -ErrorAction SilentlyContinue
    if ($null -ne $wixCommand) {
        return Split-Path -Parent $wixCommand.Source
    }

    $candleCommand = Get-Command candle.exe -ErrorAction SilentlyContinue
    if ($null -ne $candleCommand) {
        return Split-Path -Parent $candleCommand.Source
    }

    $localWixRoot = Join-Path $projectRoot ".build-tools\wix314"
    if (Test-Path -LiteralPath $localWixRoot -PathType Container) {
        $localCandle = Get-ChildItem -LiteralPath $localWixRoot -Filter candle.exe -File -Recurse |
                Select-Object -First 1
        if ($null -ne $localCandle) {
            return $localCandle.DirectoryName
        }
    }

    return $null
}

if (-not (Test-Path -LiteralPath $iconPath -PathType Leaf)) {
    throw "Windows icon not found: $iconPath"
}

$maven = Find-Executable -Names @("mvn.cmd", "mvn.exe", "mvn") -Fallbacks @(
    "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
)
if ($null -eq $maven) {
    throw "Maven was not found. Maven is required only on the build computer."
}

$jpackage = Find-Executable -Names @("jpackage.exe", "jpackage")
if ($null -eq $jpackage) {
    throw "jpackage was not found. Run this script with a JDK that includes jpackage."
}

[xml]$pom = Get-Content -LiteralPath $pomPath -Raw
$applicationVersion = [string]$pom.project.version
if ([string]::IsNullOrWhiteSpace($applicationVersion) -or
        $applicationVersion.EndsWith("-SNAPSHOT", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "pom.xml must contain a stable release version before packaging."
}

Push-Location $projectRoot
try {
    if ($SkipTests) {
        Invoke-Checked -Executable $maven -Arguments @("clean", "package", "-DskipTests")
    } else {
        Invoke-Checked -Executable $maven -Arguments @("clean", "verify")
    }

    Remove-ProjectDirectory -Path $workDirectory
    Remove-ProjectDirectory -Path $releaseDirectory
    New-Item -ItemType Directory -Force -Path $dependencyDirectory | Out-Null
    New-Item -ItemType Directory -Force -Path $modulePathDirectory | Out-Null
    New-Item -ItemType Directory -Force -Path $appImageParent | Out-Null

    Invoke-Checked -Executable $maven -Arguments @(
        "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies",
        "-DincludeScope=runtime",
        "-DoutputDirectory=$dependencyDirectory"
    )

    $applicationJar = Join-Path $targetDirectory "$applicationName-$applicationVersion.jar"
    if (-not (Test-Path -LiteralPath $applicationJar -PathType Leaf)) {
        throw "Application JAR not found after Maven build: $applicationJar"
    }
    Copy-Item -LiteralPath $applicationJar -Destination $modulePathDirectory

    foreach ($dependency in Get-ChildItem -LiteralPath $dependencyDirectory -Filter *.jar -File) {
        # OpenJFX publishes tiny platform-neutral marker JARs next to the real Windows modules.
        if ($dependency.Name -like "javafx-*.jar" -and $dependency.Length -lt 1024) {
            continue
        }
        Copy-Item -LiteralPath $dependency.FullName -Destination $modulePathDirectory
    }

    $applicationModule = "$moduleName/$mainClass"
    Invoke-Checked -Executable $jpackage -Arguments @(
        "--type", "app-image",
        "--name", $applicationName,
        "--app-version", $applicationVersion,
        "--vendor", $vendor,
        "--description", "Desktop management for gaming cafe stations, sessions, inventory, checkout, invoices, users, reports, and settings.",
        "--copyright", "Copyright (c) Ehab Salah",
        "--module-path", $modulePathDirectory,
        "--module", $applicationModule,
        "--java-options", "--enable-native-access=javafx.graphics,org.xerial.sqlitejdbc",
        "--icon", $iconPath,
        "--dest", $appImageParent
    )

    if (-not (Test-Path -LiteralPath (Join-Path $appImageDirectory "$applicationName.exe") -PathType Leaf)) {
        throw "jpackage did not create the expected application image: $appImageDirectory"
    }

    if ($PackageType -in @("Installer", "All")) {
        $wixDirectory = Find-WixDirectory
        if ($null -eq $wixDirectory) {
            throw @"
The self-contained application image was created successfully, but WiX was not found.
WiX 3.0 or later is required by jpackage only to build a Windows EXE/MSI installer.
Place the official WiX 3.14.1 binaries under .build-tools\wix314 or add WiX to PATH,
then run this script again with -PackageType Installer.
"@
        }

        New-Item -ItemType Directory -Force -Path $installerWorkDirectory | Out-Null
        $previousPath = $env:Path
        try {
            $env:Path = "$wixDirectory;$previousPath"
            Invoke-Checked -Executable $jpackage -Arguments @(
                "--type", "exe",
                "--name", $applicationName,
                "--app-version", $applicationVersion,
                "--vendor", $vendor,
                "--description", "Game Cafe Manager desktop application",
                "--copyright", "Copyright (c) Ehab Salah",
                "--app-image", $appImageDirectory,
                "--dest", $installerWorkDirectory,
                "--install-dir", $applicationName,
                "--win-per-user-install",
                "--win-dir-chooser",
                "--win-menu",
                "--win-menu-group", $applicationName,
                "--win-shortcut",
                "--win-upgrade-uuid", $upgradeUuid
            )
        } finally {
            $env:Path = $previousPath
        }

        $generatedInstaller = Get-ChildItem -LiteralPath $installerWorkDirectory -Filter *.exe -File |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
        if ($null -eq $generatedInstaller) {
            throw "jpackage completed without producing an EXE installer."
        }

        $finalInstaller = Join-Path $releaseDirectory "$applicationName-Setup.exe"
        Move-Item -LiteralPath $generatedInstaller.FullName -Destination $finalInstaller -Force
        Write-Host "Installer: $finalInstaller"
    }

    Write-Host "Application image: $appImageDirectory"
    Write-Host "Application version: $applicationVersion"
} finally {
    Pop-Location
}
