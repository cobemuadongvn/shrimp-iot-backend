# Test nhanh bằng curl trên Windows PowerShell

Thay `<ADMIN_TOKEN>` bằng token login admin.

## 1. Health

```powershell
curl.exe -i "http://192.168.1.8:8080/api/health"
```

## 2. Login admin

```powershell
curl.exe -i -X POST "http://192.168.1.8:8080/api/auth/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

## 3. Arduino giả lập gửi dữ liệu sensor

```powershell
curl.exe -i -X POST "http://192.168.1.8:8080/api/readings" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: MY_SECRET_KEY" ^
  -d "{\"deviceId\":\"device_01\",\"temperature\":30.5,\"ph\":7.2,\"ecValue\":1.1,\"salinity\":12.5,\"doValue\":5.8}"
```

## 4. Gửi dữ liệu tạo cảnh báo

```powershell
curl.exe -i -X POST "http://192.168.1.8:8080/api/readings" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: MY_SECRET_KEY" ^
  -d "{\"deviceId\":\"device_01\",\"temperature\":32.5,\"ph\":4.5,\"ecValue\":0.9,\"salinity\":0.4,\"doValue\":2.8}"
```

## 5. Lấy sensor mới nhất

```powershell
curl.exe -i "http://192.168.1.8:8080/api/readings/latest?deviceId=device_01" ^
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

## 6. Tạo command bật relay 1

```powershell
curl.exe -i -X POST "http://192.168.1.8:8080/api/commands" ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <ADMIN_TOKEN>" ^
  -d "{\"deviceId\":\"device_01\",\"relayNo\":1,\"action\":\"ON\"}"
```

## 7. Arduino lấy pending command

```powershell
curl.exe -i "http://192.168.1.8:8080/api/commands/pending?deviceId=device_01" ^
  -H "X-API-Key: MY_SECRET_KEY"
```

## 8. Arduino ACK command

Thay `<COMMAND_ID>` bằng id command lấy ở bước pending.

```powershell
curl.exe -i -X POST "http://192.168.1.8:8080/api/commands/<COMMAND_ID>/ack" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: MY_SECRET_KEY" ^
  -d "{\"success\":true,\"message\":\"Relay 1 turned ON\"}"
```

## 9. Test CORS PATCH preflight

```powershell
curl.exe -i -X OPTIONS "http://192.168.1.8:8080/api/users/5/deactivate" ^
  -H "Origin: http://192.168.1.8:3000" ^
  -H "Access-Control-Request-Method: PATCH" ^
  -H "Access-Control-Request-Headers: authorization,content-type"
```

Kỳ vọng có:

```http
Access-Control-Allow-Origin: http://192.168.1.8:3000
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
```
