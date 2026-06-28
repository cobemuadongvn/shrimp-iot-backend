param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$secretFile = Join-Path $ProjectRoot '.env.supabase.local'
$mqttSecretFile = Join-Path $ProjectRoot '.env.mqtt-cloud.local'
$jar = Join-Path $ProjectRoot 'target\shrimp-iot-0.0.1-SNAPSHOT.jar'

if (-not (Test-Path -LiteralPath $secretFile)) {
    throw "Missing local Supabase configuration: $secretFile"
}
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Missing backend JAR: $jar"
}

Get-Content -LiteralPath $secretFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
        $parts = $line.Split('=', 2)
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1], 'Process')
    }
}

if (Test-Path -LiteralPath $mqttSecretFile) {
    Get-Content -LiteralPath $mqttSecretFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1], 'Process')
        }
    }
}

$required = @(
    'TARGET_DB_HOST', 'TARGET_DB_PORT', 'TARGET_DB_NAME',
    'TARGET_DB_USER', 'TARGET_DB_PASSWORD', 'TARGET_DB_SSLMODE'
)
foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq 'PASTE_DATABASE_PASSWORD_HERE') {
        throw "Missing Supabase setting: $name"
    }
}

$env:DB_HOST = $env:TARGET_DB_HOST
$env:DB_PORT = $env:TARGET_DB_PORT
$env:DB_NAME = $env:TARGET_DB_NAME
$env:DB_USER = $env:TARGET_DB_USER
$env:DB_PASSWORD = $env:TARGET_DB_PASSWORD
$env:DB_SSLMODE = $env:TARGET_DB_SSLMODE
$env:DB_POOL_MAX_SIZE = '5'
$env:DB_POOL_MIN_IDLE = '1'
$env:FLYWAY_ENABLED = 'true'
$env:FLYWAY_BASELINE_ON_MIGRATE = 'false'
$env:JPA_DDL_AUTO = 'validate'

$java = 'D:\jdk-21\bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    $java = (Get-Command java).Source
}

$process = Start-Process `
    -FilePath $java `
    -ArgumentList @('-jar', $jar) `
    -WorkingDirectory $ProjectRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $ProjectRoot '.codex-backend.out.log') `
    -RedirectStandardError (Join-Path $ProjectRoot '.codex-backend.err.log') `
    -PassThru

Write-Output "Backend started with Supabase: PID=$($process.Id)"
