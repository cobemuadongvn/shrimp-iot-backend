# Test phân quyền ADMIN / USER / TECHNICIAN

## 1. Đăng nhập

### ADMIN
```bash
curl.exe -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"REPLACE_WITH_LOCAL_ADMIN_PASSWORD\"}"
```

### USER - Chủ ao / khách hàng
```bash
curl.exe -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"user\",\"password\":\"REPLACE_WITH_LOCAL_USER_PASSWORD\"}"
```

### TECHNICIAN
```bash
curl.exe -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"tech\",\"password\":\"REPLACE_WITH_LOCAL_TECH_PASSWORD\"}"
```

Lấy `data.token` trong response.

## 2. Gọi API app/web bằng token

```bash
curl.exe "http://localhost:8080/api/readings/latest?deviceId=device_01" ^
  -H "Authorization: Bearer TOKEN_CUA_BAN"
```

## 3. Tạo lệnh bật relay

```bash
curl.exe -X POST http://localhost:8080/api/commands ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer TOKEN_CUA_BAN" ^
  -d "{\"deviceId\":\"device_01\",\"relayNo\":1,\"action\":\"ON\"}"
```

## 4. Arduino vẫn dùng X-API-Key

```bash
curl.exe -X POST http://localhost:8080/api/readings ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: REPLACE_WITH_LOCAL_IOT_API_KEY" ^
  -d "{\"deviceId\":\"device_01\",\"temperature\":28.5,\"ph\":7.2,\"ecValue\":1.8,\"salinity\":12.8,\"doValue\":5.6}"
```
