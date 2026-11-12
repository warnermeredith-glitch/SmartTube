param(
    [string]$FireStickIp = ""
)

$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$projectDir = "c:\Users\warne\.gemini\antigravity\Projects\Technical and Systems Issues\Kidtube-SmartTube"

if (-not (Test-Path $adb)) {
    Write-Host "ERROR: Could not find adb.exe at $adb" -ForegroundColor Red
    exit 1
}

# Locate the newest signed Universal Kidtube APK (Release preferred, Debug fallback)
$apkCandidates = @(
    Get-ChildItem "$projectDir\smarttubetv\build\outputs\apk\stkidtube\release\*universal*.apk" -ErrorAction SilentlyContinue
    Get-ChildItem "$projectDir\smarttubetv\build\outputs\apk\stkidtube\debug\*universal*.apk" -ErrorAction SilentlyContinue
) | Sort-Object LastWriteTime -Descending

if ($apkCandidates.Count -eq 0) {
    Write-Host "ERROR: No built Kidtube universal APK found yet in smarttubetv\build\outputs\apk\stkidtube\" -ForegroundColor Red
    exit 1
}

$apkPath = $apkCandidates[0].FullName
Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   KIDTUBE WIRELESS FIRE STICK INSTALLER" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "APK Ready : $($apkCandidates[0].Name) ($([Math]::Round($apkCandidates[0].Length / 1MB, 1)) MB)" -ForegroundColor Green

$targets = New-Object System.Collections.Generic.List[string]

if (-not [string]::IsNullOrWhiteSpace($FireStickIp)) {
    $targets.Add($FireStickIp.Trim())
} else {
    Write-Host ""
    Write-Host "Scanning your home Wi-Fi (192.168.1.x) for Fire Sticks with ADB Debugging ON (port 5555)..." -ForegroundColor Yellow

    # Check known Fire Stick 192.168.1.187 first, then sweep subnet in parallel
    $ipsToCheck = @("192.168.1.187") + (1..254 | ForEach-Object { "192.168.1.$_" } | Where-Object { $_ -ne "192.168.1.187" -and $_ -ne "192.168.1.153" })

    $openIps = $ipsToCheck | ForEach-Object {
        $ip = $_
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($ip, 5555, $null, $null)
        if ($iar.AsyncWaitHandle.WaitOne(35, $false) -and $client.Connected) {
            $client.Close()
            $ip
        } else {
            $client.Close()
        }
    }

    foreach ($found in $openIps) {
        if (-not $targets.Contains($found)) {
            $targets.Add($found)
        }
    }
}

if ($targets.Count -eq 0) {
    Write-Host ""
    Write-Host "No Fire TV Sticks responded on port 5555 yet." -ForegroundColor Yellow
    Write-Host "On your Fire TV Stick, use your remote to turn on ADB Debugging:" -ForegroundColor White
    Write-Host "  1. Go to Settings (gear icon) -> My Fire TV -> About"
    Write-Host "  2. Highlight 'Fire TV Stick' and click the center Select button 7 times rapidly until it says 'You are already a developer'."
    Write-Host "  3. Press Back once -> click 'Developer Options' -> turn 'ADB Debugging' ON (and 'Install unknown apps' if listed)."
    Write-Host "  4. Re-run this script (or ask Carl to push Kidtube)!"
    exit 2
}

& $adb start-server | Out-Null

foreach ($ip in $targets) {
    $serial = "${ip}:5555"
    Write-Host ""
    Write-Host "Connecting to Fire TV Stick at $serial ..." -ForegroundColor Cyan
    & $adb connect $serial

    # Wait up to 45s if TV displays the one-time 'Allow USB debugging?' prompt
    $authorized = $false
    for ($attempt = 1; $attempt -le 15; $attempt++) {
        $devicesOut = (& $adb devices) -join "`n"
        if ($devicesOut -match "$([Regex]::Escape($serial))\s+device\b") {
            $authorized = $true
            break
        }
        if ($attempt -eq 1) {
            Write-Host "  -> Look at your TV screen! Check 'Always allow from this computer' and click OK on your remote..." -ForegroundColor Yellow
        }
        Start-Sleep -Seconds 3
        & $adb connect $serial | Out-Null
    }

    if (-not $authorized) {
        Write-Host "  -> Timed out waiting for remote approval on $ip. Please click Allow on the TV and try again." -ForegroundColor Red
        continue
    }

    Write-Host "  -> Connected! Installing Kidtube onto $ip (takes ~20-30 seconds)..." -ForegroundColor Green
    & $adb -s $serial install -r "$apkPath"

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  -> SUCCESS! Launching Kidtube on your TV now..." -ForegroundColor Green
        & $adb -s $serial shell monkey -p org.kidtube.tv -c android.intent.category.LEANBACK_LAUNCHER 1 | Out-Null
        Write-Host "  -> Kidtube is live on $ip! (Default Parent PIN: 1414)" -ForegroundColor Cyan
    } else {
        Write-Host "  -> Install reported exit code $LASTEXITCODE on $ip." -ForegroundColor Red
    }
}
