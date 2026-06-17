# Frontend Handoff — Những điểm bắt buộc khi nối Web/App

## Base URL

```js
const API_BASE_URL = "http://192.168.1.8:8080/api";
const DEVICE_ID = "device_01";
```

Nếu web chạy từ máy khác, không dùng `localhost:8080` vì `localhost` là máy chạy web, không phải máy chạy backend.

## Axios interceptor chuẩn

```js
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  config.headers = config.headers || {};

  if (!config.skipAuth && token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
```

Không đưa dòng này vào frontend:

```js
config.headers["X-API-Key"] = API_KEY;
```

`X-API-Key` chỉ dùng cho Arduino.

## API không được gọi từ Web/App

```http
GET /api/commands/pending?deviceId=device_01
POST /api/commands/{id}/ack
POST /api/readings
```

Các API này thuộc Arduino/device. Nếu web gọi `/pending`, web có thể lấy mất lệnh trước Arduino.

## API Web/App nên dùng

```http
POST /api/auth/login
POST /api/auth/register
GET  /api/auth/me
PUT  /api/users/profile

GET  /api/readings/latest?deviceId=device_01
GET  /api/readings/history?deviceId=device_01&limit=50
GET  /api/alerts/open?deviceId=device_01
POST /api/commands
GET  /api/commands/history?deviceId=device_01
GET  /api/relay-states/device_01
POST /api/chat/message
GET  /api/dashboard/summary?deviceId=device_01
```

Admin dùng thêm:

```http
GET   /api/users
GET   /api/users/pending
POST  /api/users/{id}/approve
POST  /api/users/{id}/reject
PATCH /api/users/{id}/deactivate
PATCH /api/users/{id}/activate
GET   /api/audit-logs
```

## Xử lý token expired

Nếu backend trả:

```json
{
  "success": false,
  "message": "Token is expired"
}
```

Frontend nên:

```text
1. Xóa localStorage token.
2. Đưa người dùng về màn hình login.
3. Hiển thị: Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.
```

## CORS

Backend đã allow các origin:

```text
http://localhost:3000
http://localhost:5173
http://192.168.1.8:3000
http://192.168.1.8:5173
```

Nếu web chạy cổng khác, cần thêm origin đó vào `application.yml` hoặc `CorsConfig.java`.
