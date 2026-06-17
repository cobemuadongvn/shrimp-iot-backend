# Test nhanh 2 chế độ vận hành trên Windows PowerShell

## 1. Login admin

```powershell
curl.exe -X POST "http://192.168.1.8:8080/api/auth/login" `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

Copy token trả về.

## 2. Xem mode

```powershell
curl.exe -X GET "http://192.168.1.8:8080/api/devices/device_01/operation-mode" `
  -H "Authorization: Bearer <TOKEN>"
```

## 3. Chọn MANUAL

```powershell
curl.exe -X PATCH "http://192.168.1.8:8080/api/devices/device_01/operation-mode" `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Content-Type: application/json" `
  -d "{\"operationMode\":\"MANUAL\"}"
```

## 4. Bấm “Đo ngay”

```powershell
curl.exe -X POST "http://192.168.1.8:8080/api/sampling/measurement/measure-now" `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Content-Type: application/json" `
  -d "{\"deviceId\":\"device_01\",\"sampleSource\":\"Buồng đo trung tâm\"}"
```

## 5. Chọn AI_AUTO

```powershell
curl.exe -X PATCH "http://192.168.1.8:8080/api/devices/device_01/operation-mode" `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Content-Type: application/json" `
  -d "{\"operationMode\":\"AI_AUTO\"}"
```

## 6. Cấu hình xử lý độ mặn

```powershell
curl.exe -X PATCH "http://192.168.1.8:8080/api/devices/device_01/salinity-control-config" `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Content-Type: application/json" `
  -d "{\"salinityAutoEnabled\":true,\"salinityHighThreshold\":35.0,\"salinityStopThreshold\":32.0,\"salinityDrainDurationSeconds\":30,\"freshwaterDurationSeconds\":30,\"mixingWaitSeconds\":180,\"maxRetryCount\":3,\"cooldownMinutes\":10}"
```

## 7. Giả lập Arduino gửi độ mặn cao

```powershell
curl.exe -X POST "http://192.168.1.8:8080/api/readings" `
  -H "Content-Type: application/json" `
  -H "X-API-Key: MY_SECRET_KEY" `
  -d "{\"deviceId\":\"device_01\",\"temperature\":30.5,\"ph\":7.2,\"ecValue\":1.1,\"salinity\":38.5,\"doValue\":5.8}"
```

Sau đó kiểm tra pending command Arduino:

```powershell
curl.exe -X GET "http://192.168.1.8:8080/api/commands/pending?deviceId=device_01" `
  -H "X-API-Key: MY_SECRET_KEY"
```
