param(
    [string]$FireStickIp = ""
)

$ErrorActionPreference = "Continue"
$adb = if (Get-Command adb -ErrorAction SilentlyContinue) {
    (Get-Command adb).Source
} else {
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
}
$projectDir = $PSScriptRoot

if (-not (Test-Path $adb)) {
    Write-Host "ERROR: Could not find adb.exe at $adb" -ForegroundColor Red
    exit 1
}

# Locate the newest signed Universal Kidtube APK (dist/ or build outputs)
$apkCandidates = @(
    Get-ChildItem "$projectDir\dist\Kidtube_Universal_FireTV.apk" -ErrorAction SilentlyContinue
    Get-ChildItem "$projectDir\smarttubetv\build\outputs\apk\stkidtube\release\*universal*.apk" -ErrorAction SilentlyContinue
    Get-ChildItem "$projectDir\smarttubetv\build\outputs\apk\stkidtube\debug\*universal*.apk" -ErrorAction SilentlyContinue
) | Sort-Object LastWriteTime -Descending

if ($apkCandidates.Count -eq 0) {
    Write-Host "ERROR: No built Kidtube universal APK found yet in dist\ or smarttubetv\build\outputs\apk\stkidtube\" -ForegroundColor Red
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
    # Auto-detect active IPv4 /24 prefix (defaults to 192.168.1)
    $subnetPrefix = "192.168.1"
    $selfOctet = -1
    $localIpObj = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -match '^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[0-1])\.)' -and $_.PrefixOrigin -ne 'WellKnown' } |
        Select-Object -First 1
    if ($localIpObj) {
        $octets = $localIpObj.IPAddress.Split('.')
        $subnetPrefix = "$($octets[0]).$($octets[1]).$($octets[2])"
        $selfOctet = [int]$octets[3]
    }

    Write-Host ""
    Write-Host "Scanning home Wi-Fi ($subnetPrefix.x) for Fire Sticks with ADB Debugging ON (port 5555)..." -ForegroundColor Yellow

    # Scan in batches of 32 so Windows half-open socket rate-limiting never drops a Fire Stick
    $openIps = @()
    for ($batchStart = 1; $batchStart -le 254; $batchStart += 32) {
        $batchEnd = [Math]::Min(254, $batchStart + 31)
        $jobs = @()
        for ($i = $batchStart; $i -le $batchEnd; $i++) {
            if ($i -eq $selfOctet) { continue }
            $ip = "$subnetPrefix.$i"
            $c = New-Object System.Net.Sockets.TcpClient
            $jobs += @{ Ip = $ip; Client = $c; Iar = $c.BeginConnect($ip, 5555, $null, $null) }
        }
        Start-Sleep -Milliseconds 650
        foreach ($j in $jobs) {
            if ($j.Iar.IsCompleted -and $j.Client.Connected) {
                $openIps += $j.Ip
            }
            $j.Client.Close()
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
    Write-Host "  4. Re-run this script!"
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
        Write-Host "  -> Kidtube is live on $ip!" -ForegroundColor Cyan
    } else {
        Write-Host "  -> Install reported exit code $LASTEXITCODE on $ip." -ForegroundColor Red
    }
}
