# Test In-app Notification API trên Windows PowerShell

## 1. Login lấy token

```powershell
$base = "http://localhost:8080"
$loginBody = '{"username":"admin","password":"admin123"}'
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }
```

## 2. Tạo notification test

```powershell
$body = '{"deviceId":"device_01","message":"Test in-app notification"}'
Invoke-RestMethod -Uri "$base/api/notifications/test" -Method POST -Headers $headers -ContentType "application/json" -Body $body
```

## 3. Xem danh sách in-app notification

```powershell
Invoke-RestMethod -Uri "$base/api/notifications/in-app?deviceId=device_01&unreadOnly=false&limit=20" -Method GET -Headers $headers
```

## 4. Đếm notification chưa đọc

```powershell
Invoke-RestMethod -Uri "$base/api/notifications/in-app/unread-count?deviceId=device_01" -Method GET -Headers $headers
```

## 5. Đánh dấu một notification đã đọc

Thay `101` bằng ID thật từ danh sách notification.

```powershell
Invoke-RestMethod -Uri "$base/api/notifications/in-app/101/read" -Method PATCH -Headers $headers
```

## 6. Đánh dấu tất cả đã đọc

```powershell
Invoke-RestMethod -Uri "$base/api/notifications/in-app/read-all?deviceId=device_01" -Method PATCH -Headers $headers
```
