[CmdletBinding()]
param(
    [string]$DeviceSerial,
    [ValidateNotNullOrEmpty()]
    [string]$ApplicationId = "com.example.focuslab"
)

$ErrorActionPreference = "Stop"

try {
    $adbCommand = Get-Command adb -ErrorAction Stop
    $deviceOutput = & $adbCommand.Source devices
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed with exit code $LASTEXITCODE."
    }

    $readyDevices = @(
        foreach ($line in $deviceOutput) {
            if ($line -match '^(\S+)\s+device$') {
                $Matches[1]
            }
        }
    )

    if ([string]::IsNullOrWhiteSpace($DeviceSerial)) {
        if ($readyDevices.Count -ne 1) {
            throw "Expected exactly one ready ADB device, found $($readyDevices.Count). Pass -DeviceSerial explicitly."
        }
        $DeviceSerial = $readyDevices[0]
    } elseif ($DeviceSerial -notin $readyDevices) {
        throw "ADB device '$DeviceSerial' is not connected and ready."
    }

    $clearOutput = & $adbCommand.Source -s $DeviceSerial shell pm clear $ApplicationId
    $clearExitCode = $LASTEXITCODE
    if ($clearExitCode -ne 0 -or ($clearOutput -join "`n") -notmatch '^Success\s*$') {
        throw "Failed to clear '$ApplicationId' on '$DeviceSerial': $($clearOutput -join ' ')"
    }

    Write-Host "Cleared app data for '$ApplicationId' on '$DeviceSerial'."
    Write-Host "The app was not launched. Personalize app_name and defaults before the first classroom Run."
    exit 0
} catch {
    Write-Error $_.Exception.Message -ErrorAction Continue
    exit 1
}
