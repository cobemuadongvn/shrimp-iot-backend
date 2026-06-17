# Test xử lý mất kết nối thiết bị tại ao trên Windows PowerShell

## 1. Login lấy token

```powershell
$login = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"admin123"}'
$token = $login.token
```

## 2. Giả lập MQTT/status OFFLINE bằng API nội bộ không có sẵn

Trong hệ thống thật, `OFFLINE` đến từ MQTT topic:

```text
shrimp-iot/devices/device_01/status
```

Payload:

```text
OFFLINE
```

Cách test nhanh bằng MQTTX hoặc mosquitto_pub:

```powershell
mosquitto_pub -h 127.0.0.1 -p 1883 -t "shrimp-iot/devices/device_01/status" -m "OFFLINE"
```

Kết quả mong đợi:

```text
devices.connection_status = OFFLINE
alert DEVICE_OFFLINE được tạo
in-app notification được tạo
```

## 3. Kiểm tra alert đang mở

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/alerts/open?deviceId=device_01" `
  -Headers @{ Authorization = "Bearer $token" }
```

## 4. Kiểm tra thông báo trong app

```powershell
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/notifications/in-app?deviceId=device_01&unreadOnly=false&limit=20" `
  -Headers @{ Authorization = "Bearer $token" }
```

## 5. Giả lập thiết bị online lại

```powershell
mosquitto_pub -h 127.0.0.1 -p 1883 -t "shrimp-iot/devices/device_01/status" -m "ONLINE"
```

Kết quả mong đợi:

```text
devices.connection_status = ONLINE
alert DEVICE_OFFLINE được RESOLVED
```
