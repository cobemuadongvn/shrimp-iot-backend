
# ============================================================
# SHRIMP IOT - TEST SUITE (curl.exe + @file)
# ============================================================
$BASE = "http://localhost:8080"

function Sep($title, $color="Cyan") {
    Write-Host "`n=====================================================" -ForegroundColor DarkCyan
    Write-Host "  $title" -ForegroundColor $color
    Write-Host "=====================================================" -ForegroundColor DarkCyan
}

# TEST 1 — Health Check
Sep "TEST 1: Health Check"
curl.exe -s "$BASE/api/health"

# TEST 2 — Arduino POST readings
Sep "TEST 2: Arduino POST /api/readings (X-API-Key)"
curl.exe -s -X POST "$BASE/api/readings" -H "Content-Type: application/json" -H "X-API-Key: REPLACE_WITH_LOCAL_IOT_API_KEY" --data "@d:\shrimp-iot-complete-work\body_reading.json"

# TEST 3 — Arduino GET pending commands
Sep "TEST 3: Arduino GET /api/commands/pending (X-API-Key)"
curl.exe -s "$BASE/api/commands/pending?deviceId=device_01" -H "X-API-Key: REPLACE_WITH_LOCAL_IOT_API_KEY"

# TEST 4 — Login 3 accounts
Sep "TEST 4a: Login admin"
$adminJson = curl.exe -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" --data "@d:\shrimp-iot-complete-work\body_admin.json"
Write-Host $adminJson
$adminToken = ($adminJson | ConvertFrom-Json).data.token

Sep "TEST 4b: Login user"
$userJson = curl.exe -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" --data "@d:\shrimp-iot-complete-work\body_user.json"
Write-Host $userJson
$userToken = ($userJson | ConvertFrom-Json).data.token

Sep "TEST 4c: Login tech"
$techJson = curl.exe -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" --data "@d:\shrimp-iot-complete-work\body_tech.json"
Write-Host $techJson
$techToken = ($techJson | ConvertFrom-Json).data.token

Write-Host "`n[TOKENS]"
Write-Host "admin: $($adminToken.Substring(0,[Math]::Min(30,$adminToken.Length)))..." -ForegroundColor Green
Write-Host "user : $($userToken.Substring(0,[Math]::Min(30,$userToken.Length)))..." -ForegroundColor Green
Write-Host "tech : $($techToken.Substring(0,[Math]::Min(30,$techToken.Length)))..." -ForegroundColor Green

# TEST 5 — USER -> device_01 (200)
Sep "TEST 5: USER -> device_01 (expect 200)"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/readings/latest?deviceId=device_01" -H "Authorization: Bearer $userToken"

# TEST 6 — USER -> device_02 (200)
Sep "TEST 6: USER -> device_02 (expect 200)"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/readings/latest?deviceId=device_02" -H "Authorization: Bearer $userToken"

# TEST 7 — USER -> device_03 (403)
Sep "TEST 7: USER -> device_03 (expect 403 DENIED)" "Yellow"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/readings/latest?deviceId=device_03" -H "Authorization: Bearer $userToken"

# TEST 8 — TECH -> device_01 (200)
Sep "TEST 8: TECH -> device_01 (expect 200)"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/readings/latest?deviceId=device_01" -H "Authorization: Bearer $techToken"

# TEST 9 — TECH -> device_02 (403)
Sep "TEST 9: TECH -> device_02 (expect 403 DENIED)" "Yellow"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/readings/latest?deviceId=device_02" -H "Authorization: Bearer $techToken"

# TEST 10 — ADMIN -> device_03 (200)
Sep "TEST 10: ADMIN -> device_03 (expect 200)"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/readings/latest?deviceId=device_03" -H "Authorization: Bearer $adminToken"

# TEST 11 — ADMIN -> GET /api/ponds
Sep "TEST 11: ADMIN -> GET /api/ponds (expect 200 list)"
curl.exe -s "$BASE/api/ponds" -H "Authorization: Bearer $adminToken"

# TEST 12 — ADMIN -> GET /api/devices
Sep "TEST 12: ADMIN -> GET /api/devices (expect 200 list)"
curl.exe -s "$BASE/api/devices" -H "Authorization: Bearer $adminToken"

# TEST 13 — USER -> GET /api/ponds (403)
Sep "TEST 13: USER -> GET /api/ponds (expect 403)" "Yellow"
curl.exe -s -w "`nHTTP:%{http_code}" "$BASE/api/ponds" -H "Authorization: Bearer $userToken"

Write-Host "`n=====================================================" -ForegroundColor Green
Write-Host "  KIEM THU HOAN TAT" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
