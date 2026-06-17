# E2E test AI_AUTO phương án B - Windows curl

## 1. Login admin

```powershell
curl.exe -X POST "http://192.168.1.8:8080/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

Copy `data.token` vào biến:

```powershell
$TOKEN="PASTE_TOKEN_HERE"
```

## 2. Chuyển sang AI_AUTO

```powershell
curl.exe -X PATCH "http://192.168.1.8:8080/api/devices/device_01/operation-mode" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"operationMode\":\"AI_AUTO\"}"
```

## 3. Cấu hình AI theo mặc định đồ án

```powershell
curl.exe -X PATCH "http://192.168.1.8:8080/api/devices/device_01/salinity-control-config" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"salinityAutoEnabled\":true,\"salinityHighThreshold\":35.0,\"salinityStopThreshold\":32.0,\"salinityDrainDurationSeconds\":20,\"freshwaterDurationSeconds\":25,\"mixingWaitSeconds\":120,\"maxRetryCount\":2,\"cooldownMinutes\":10,\"readingMaxAgeSeconds\":120,\"autoRemeasureEnabled\":true,\"safetyLockEnabled\":false}"
```

## 4. Cấu hình chu kỳ đo

```powershell
curl.exe -X PATCH "http://192.168.1.8:8080/api/devices/device_01/measurement-config" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"fillDurationSeconds\":20,\"stabilizingSeconds\":45,\"measurementDurationSeconds\":30,\"measurementDrainDurationSeconds\":20}"
```

## 5. Bấm Đo ngay

```powershell
curl.exe -X POST "http://192.168.1.8:8080/api/sampling/measurement/measure-now" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"deviceId\":\"device_01\",\"sampleSource\":\"Buồng đo trung tâm\"}"
```

## 6. Xem trạng thái chu kỳ đo

```powershell
curl.exe "http://192.168.1.8:8080/api/sampling/measurement/current?deviceId=device_01" ^
  -H "Authorization: Bearer %TOKEN%"
```

## 7. Xem trạng thái xử lý độ mặn AI

```powershell
curl.exe "http://192.168.1.8:8080/api/sampling/salinity/current?deviceId=device_01" ^
  -H "Authorization: Bearer %TOKEN%"
```

## Ghi chú

Trong PowerShell biến sẽ là `$TOKEN`, trong CMD là `%TOKEN%`. Nếu dùng PowerShell, thay `%TOKEN%` bằng `$TOKEN`.
