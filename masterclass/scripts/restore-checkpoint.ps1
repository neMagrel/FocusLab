[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateNotNullOrEmpty()]
    [string]$Checkpoint
)

$ErrorActionPreference = "Stop"

function Resolve-ChildPath {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$RelativePath
    )

    if ([IO.Path]::IsPathRooted($RelativePath)) {
        throw "Absolute paths are not allowed in the checkpoint manifest: $RelativePath"
    }

    $rootPath = [IO.Path]::GetFullPath($Root).TrimEnd(
        [IO.Path]::DirectorySeparatorChar,
        [IO.Path]::AltDirectorySeparatorChar
    )
    $candidate = [IO.Path]::GetFullPath((Join-Path $rootPath $RelativePath))
    $prefix = $rootPath + [IO.Path]::DirectorySeparatorChar
    if (-not $candidate.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Path escapes its expected root: $RelativePath"
    }
    return $candidate
}

try {
    $scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
    $repositoryRoot = [IO.Path]::GetFullPath((Join-Path $scriptDirectory "..\.."))
    $checkpointsRoot = Resolve-ChildPath $repositoryRoot "masterclass/checkpoints"
    $manifestPath = Resolve-ChildPath $checkpointsRoot "manifest.json"

    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Checkpoint manifest not found: $manifestPath"
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    $matches = @($manifest.checkpoints | Where-Object { $_.id -eq $Checkpoint })
    if ($matches.Count -ne 1) {
        $available = @($manifest.checkpoints | Where-Object { $_.status -eq "available" } | ForEach-Object { $_.id })
        throw "Unknown checkpoint '$Checkpoint'. Available: $($available -join ', ')"
    }

    $entry = $matches[0]
    if ($entry.status -ne "available" -or [string]::IsNullOrWhiteSpace($entry.directory)) {
        throw "Checkpoint '$Checkpoint' is planned but not available yet."
    }

    $snapshotRoot = Resolve-ChildPath $checkpointsRoot ([string]$entry.directory)
    $studentFiles = @($manifest.studentFiles)
    if ($studentFiles.Count -eq 0) {
        throw "Checkpoint manifest contains no student files."
    }

    $restoreItems = foreach ($relativePathValue in $studentFiles) {
        $relativePath = [string]$relativePathValue
        $source = Resolve-ChildPath $snapshotRoot $relativePath
        $destination = Resolve-ChildPath $repositoryRoot $relativePath
        if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Snapshot is incomplete; missing file: $source"
        }
        [PSCustomObject]@{
            RelativePath = $relativePath
            Source = $source
            Destination = $destination
        }
    }

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
    $backupRoot = Resolve-ChildPath $repositoryRoot ".recovery/$timestamp-$Checkpoint"
    New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

    foreach ($item in $restoreItems) {
        if (Test-Path -LiteralPath $item.Destination -PathType Leaf) {
            $backupPath = Resolve-ChildPath $backupRoot $item.RelativePath
            New-Item -ItemType Directory -Path (Split-Path -Parent $backupPath) -Force | Out-Null
            Copy-Item -LiteralPath $item.Destination -Destination $backupPath -Force
        }
    }

    foreach ($item in $restoreItems) {
        New-Item -ItemType Directory -Path (Split-Path -Parent $item.Destination) -Force | Out-Null
        Copy-Item -LiteralPath $item.Source -Destination $item.Destination -Force
    }

    Write-Host "Restored checkpoint '$Checkpoint'."
    Write-Host "Backup: $backupRoot"

    $gradleWrapper = Resolve-ChildPath $repositoryRoot "gradlew.bat"
    Push-Location $repositoryRoot
    try {
        & $gradleWrapper assembleDebug --offline
        $buildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    if ($buildExitCode -ne 0) {
        throw "Offline debug build failed with exit code $buildExitCode. Backup remains at $backupRoot"
    }

    Write-Host "Offline debug build passed."
    exit 0
} catch {
    Write-Error $_.Exception.Message -ErrorAction Continue
    exit 1
}
