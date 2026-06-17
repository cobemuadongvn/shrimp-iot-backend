# API Contract cho Web/App — Shrimp IoT Backend

Tài liệu này dùng để bàn giao cho bạn làm Web/App. Backend đang chạy local theo cấu hình:

```text
BASE_URL = http://192.168.1.8:8080/api
DEVICE_ID_DEMO = device_01
```

Nếu backend chạy trên máy khác hoặc IP đổi, chỉ thay `BASE_URL`.

## 1. Quy ước chung

### Header cho Web/App

Các API cho Web/App dùng token đăng nhập:

```http
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

### Header cho Arduino

Arduino dùng API key, không dùng token user:

```http
X-API-Key: MY_SECRET_KEY
Content-Type: application/json
```

Frontend **không gửi `X-API-Key`** vì API key thiết bị không được đưa lên trình duyệt.

### Response chuẩn

Backend trả response dạng:

```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "2026-05-24T10:00:00"
}
```

Frontend lấy dữ liệu chính ở `response.data.data` nếu dùng axios.

---

## 2. Health check

```http
GET /api/health
```

Dùng để kiểm tra backend có sống không.

---

## 3. Auth

### Login

```http
POST /api/auth/login
```

Body:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response cần lưu:

```json
{
  "data": {
    "token": "...",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "username": "admin",
      "fullName": "Quản trị hệ thống",
      "role": "ADMIN",
      "active": true
    }
  }
}
```

Tài khoản seed mặc định:

```text
admin / admin123        role ADMIN
user  / user123         role USER
user2 / user123         role USER
tech  / tech123         role TECHNICIAN
```

### Register

```http
POST /api/auth/register
```

Body:

```json
{
  "username": "khach01",
  "password": "123456",
  "fullName": "Chủ ao nuôi 01",
  "phone": "0987654321",
  "email": "khach01@example.com"
}
```

Sau đăng ký:

```text
role = USER
active = false
approvalStatus = PENDING
```

User chưa login được cho tới khi admin duyệt.

### Me

```http
GET /api/auth/me
Authorization: Bearer <TOKEN>
```

### Logout

```http
POST /api/auth/logout
Authorization: Bearer <TOKEN>
```

### Change password

```http
POST /api/auth/change-password
Authorization: Bearer <TOKEN>
```

Body:

```json
{
  "oldPassword": "user123",
  "newPassword": "12345678"
}
```

---

### Cập nhật hồ sơ cá nhân — USER / TECHNICIAN / ADMIN

Frontend dùng API này để user hoặc kỹ thuật viên tự cập nhật hồ sơ của chính mình. Backend lấy user hiện tại từ `Authorization: Bearer <TOKEN>`, không cho client truyền `userId` để tránh sửa nhầm tài khoản khác.

```http
PUT /api/users/profile
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Tên mới",
  "email": "email@gmail.com",
  "phone": "090xxxxxxx"
}
```

Response:

```json
{
  "success": true,
  "message": "Cập nhật hồ sơ thành công",
  "data": {
    "id": 1,
    "username": "tech01",
    "fullName": "Tên mới",
    "email": "email@gmail.com",
    "phone": "090xxxxxxx",
    "role": "TECHNICIAN"
  }
}
```

Lỗi thường gặp:

- `401 Unauthorized`: thiếu token, token sai, token hết hạn, tài khoản bị khóa hoặc chưa duyệt.
- `400 Bad Request`: `fullName` rỗng, `email` sai định dạng hoặc email đã thuộc tài khoản khác.

---

## 4. User Management — Admin only

### Lấy danh sách user

```http
GET /api/users
Authorization: Bearer <ADMIN_TOKEN>
```

### Lấy user chờ duyệt

```http
GET /api/users/pending
Authorization: Bearer <ADMIN_TOKEN>
```

### Admin tạo user trực tiếp

```http
POST /api/users
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "username": "kythuat02",
  "password": "123456",
  "fullName": "Kỹ thuật viên 02",
  "phone": "0900000002",
  "email": "tech02@example.com",
  "role": "TECHNICIAN"
}
```

### Duyệt user

```http
POST /api/users/{id}/approve
Authorization: Bearer <ADMIN_TOKEN>
```

Body duyệt chủ ao:

```json
{
  "role": "USER",
  "pondIds": [1],
  "deviceIds": ["device_01"],
  "accessType": "OWNER"
}
```

Body duyệt kỹ thuật viên:

```json
{
  "role": "TECHNICIAN",
  "pondIds": [1],
  "deviceIds": ["device_01"],
  "accessType": "READ_WRITE"
}
```

### Từ chối user

```http
POST /api/users/{id}/reject
Authorization: Bearer <ADMIN_TOKEN>
```

### Đổi role

```http
PUT /api/users/{id}/role
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "role": "TECHNICIAN"
}
```

### Khóa tài khoản

```http
PATCH /api/users/{id}/deactivate
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "reason": "Người dùng không còn quản lý ao này"
}
```

### Mở khóa tài khoản

```http
PATCH /api/users/{id}/activate
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "reason": "Mở lại tài khoản cho người dùng"
}
```

### Reset password

```http
POST /api/users/reset-password?username=user
Authorization: Bearer <ADMIN_TOKEN>
```

---

## 5. Pond Management

### Tạo ao — ADMIN

```http
POST /api/ponds
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "name": "Ao demo 01",
  "location": "Khu A - Bến Tre",
  "areaSquareMeters": 1000,
  "speciesType": "Tôm thẻ chân trắng",
  "pondType": "Ao nuôi bán thâm canh",
  "waterVolumeCubicMeters": 1500,
  "region": "Khu A",
  "status": "ACTIVE",
  "latitude": 10.2435,
  "longitude": 106.3752,
  "description": "Ao demo phục vụ bảo vệ đồ án"
}
```

### Danh sách ao

```http
GET /api/ponds
Authorization: Bearer <TOKEN>
```

ADMIN xem tất cả; USER/TECHNICIAN xem ao được gán.

### Chi tiết ao

```http
GET /api/ponds/{id}
Authorization: Bearer <TOKEN>
```

### Cập nhật ao — ADMIN

```http
PUT /api/ponds/{id}
Authorization: Bearer <ADMIN_TOKEN>
```

### Vô hiệu hóa/kích hoạt ao — ADMIN

```http
PATCH /api/ponds/{id}/deactivate
PATCH /api/ponds/{id}/activate
```

### Cấp quyền ao — ADMIN

```http
POST /api/ponds/{pondId}/access?username=user&accessType=OWNER
Authorization: Bearer <ADMIN_TOKEN>
```

`accessType` khuyến nghị:

```text
OWNER       Chủ ao, có quyền xem và điều khiển.
READ_WRITE  Kỹ thuật viên, có quyền xem và điều khiển.
READ_ONLY   Chỉ xem.
```

---

## 6. Device Management

### Đăng ký thiết bị — ADMIN / TECHNICIAN

```http
POST /api/devices?pondId=1
Authorization: Bearer <ADMIN_OR_TECH_TOKEN>
```

Body:

```json
{
  "deviceId": "device_04",
  "name": "Bộ điều khiển Ao demo",
  "status": "ACTIVE",
  "connectionStatus": "OFFLINE",
  "latitude": 10.2435,
  "longitude": 106.3752,
  "installationPosition": "Bờ ao phía Đông"
}
```

Quy tắc quyền:

```text
ADMIN: được tạo thiết bị, có thể tạo thiết bị chưa gán ao.
TECHNICIAN: chỉ được tạo thiết bị khi truyền pondId của ao mà tài khoản có accessType = OWNER / READ_WRITE / CONTROL.
USER: không được tạo thiết bị.
```

### Danh sách thiết bị

```http
GET /api/devices
Authorization: Bearer <TOKEN>
```

### Chi tiết thiết bị

```http
GET /api/devices/{deviceId}
Authorization: Bearer <TOKEN>
```

### Gán thiết bị vào ao — ADMIN / TECHNICIAN

```http
POST /api/devices/{deviceId}/link?pondId=1
Authorization: Bearer <ADMIN_OR_TECH_TOKEN>
```

Quy tắc quyền:

```text
ADMIN: được gán mọi thiết bị vào mọi ao.
TECHNICIAN: chỉ được gán thiết bị vào ao mà họ có accessType = OWNER / READ_WRITE / CONTROL. Nếu thiết bị đã thuộc ao khác thì TECHNICIAN cũng phải có quyền quản lý ao hiện tại của thiết bị.
USER: không được gán thiết bị.
```

### Cảm biến/relay của thiết bị

```http
GET /api/devices/{deviceId}/sensors
GET /api/devices/{deviceId}/relays
Authorization: Bearer <TOKEN>
```

### Cập nhật / kích hoạt / vô hiệu hóa thiết bị — ADMIN / TECHNICIAN

```http
PUT /api/devices/{deviceId}
PATCH /api/devices/{deviceId}/deactivate
PATCH /api/devices/{deviceId}/activate
Authorization: Bearer <ADMIN_OR_TECH_TOKEN>
```

Quy tắc quyền:

```text
ADMIN: được cập nhật toàn bộ thông tin thiết bị, gồm status và connectionStatus.
TECHNICIAN: được cập nhật name, latitude, longitude, installationPosition và được activate/deactivate thiết bị thuộc ao được gán quyền OWNER / READ_WRITE / CONTROL.
TECHNICIAN không được cập nhật trực tiếp connectionStatus qua PUT.
USER: không được cập nhật/kích hoạt/vô hiệu hóa thiết bị.
```

---

## 7. Sensor Reading

### Arduino gửi dữ liệu cảm biến

```http
POST /api/readings
X-API-Key: MY_SECRET_KEY
Content-Type: application/json
```

Body:

```json
{
  "deviceId": "device_01",
  "temperature": 30.5,
  "ph": 7.2,
  "ecValue": 1.1,
  "salinity": 12.5,
  "doValue": 5.8
}
```

### Web/App lấy dữ liệu mới nhất

```http
GET /api/readings/latest?deviceId=device_01
Authorization: Bearer <TOKEN>
```

### Lịch sử cảm biến

```http
GET /api/readings/history?deviceId=device_01&limit=50
Authorization: Bearer <TOKEN>
```

### Dữ liệu theo khoảng thời gian

```http
GET /api/readings/range?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
Authorization: Bearer <TOKEN>
```

---

## 8. Alert

```http
GET  /api/alerts/open?deviceId=device_01
GET  /api/alerts/history?deviceId=device_01
POST /api/alerts/{id}/resolve
Authorization: Bearer <TOKEN>
```

---

## 9. Command / Relay Control

### Web/App tạo lệnh điều khiển

```http
POST /api/commands
Authorization: Bearer <TOKEN>
```

Body bật máy bơm:

```json
{
  "deviceId": "device_01",
  "relayNo": 1,
  "action": "ON"
}
```

Body tắt máy bơm:

```json
{
  "deviceId": "device_01",
  "relayNo": 1,
  "action": "OFF"
}
```

Mapping relay demo:

```text
relayNo 1 = Máy bơm lọc nước
relayNo 2 = Quạt tạo dòng oxy
relayNo 3 = Sục khí oxy chính
relayNo 4 = Đèn/dự phòng
```

### Arduino lấy lệnh pending

```http
GET /api/commands/pending?deviceId=device_01
X-API-Key: MY_SECRET_KEY
```

Web/App **không gọi API này**.

### Arduino ACK lệnh

```http
POST /api/commands/{id}/ack
X-API-Key: MY_SECRET_KEY
```

Body:

```json
{
  "success": true,
  "message": "Relay 1 turned ON"
}
```

### Lịch sử lệnh

```http
GET /api/commands/history?deviceId=device_01
Authorization: Bearer <TOKEN>
```

---

## 10. Relay State

```http
GET /api/relay-states/device_01
GET /api/relay-states/device_01/1
Authorization: Bearer <TOKEN>
```

---

## 11. Dashboard

```http
GET /api/dashboard/summary?deviceId=device_01
Authorization: Bearer <TOKEN>
```

---

## 12. Chatbot

### Hỏi kiến thức nuôi tôm — Pha 1

```http
POST /api/chat/message
Authorization: Bearer <TOKEN>
```

Body:

```json
{
  "message": "pH thấp thì xử lý thế nào?"
}
```

### Hỏi dữ liệu hệ thống — Pha 2

```json
{
  "deviceId": "device_01",
  "message": "Ao hiện tại thế nào?"
}
```

Các câu hỏi nên hỗ trợ:

```text
Ao hiện tại thế nào?
Cảm biến mới nhất là bao nhiêu?
Có cảnh báo nào chưa xử lý không?
Máy bơm đang bật hay tắt?
Thiết bị có online không?
```

### Lịch sử chat

```http
GET /api/chat/sessions
GET /api/chat/sessions/{sessionId}/messages
Authorization: Bearer <TOKEN>
```

---

## 13. Map

```http
GET /api/map/ponds
GET /api/map/devices
Authorization: Bearer <TOKEN>
```

Backend trả tọa độ nếu `latitude`, `longitude` đã được nhập cho ao/thiết bị. Web dùng Leaflet/Google Maps/Mapbox để hiển thị.

---

## 14. Reports

### Summary report

```http
GET /api/reports/summary?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
Authorization: Bearer <TOKEN>
```

### Export CSV

```http
GET /api/reports/sensors.csv?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
GET /api/reports/alerts.csv?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
GET /api/reports/commands.csv?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
Authorization: Bearer <TOKEN>
```

---

## 15. Notifications

```http
GET /api/notifications?deviceId=device_01
POST /api/notifications/test
Authorization: Bearer <TOKEN>
```

Body test:

```json
{
  "deviceId": "device_01",
  "message": "Kiểm tra cảnh báo thử nghiệm"
}
```

Hiện tại SMS/email có thể đang ở mức log/webhook tùy cấu hình.

---

## 16. Audit log — ADMIN

```http
GET /api/audit-logs
GET /api/audit-logs?actor=admin
GET /api/audit-logs?deviceId=device_01
Authorization: Bearer <ADMIN_TOKEN>
```

Dùng để xem các thao tác quan trọng như duyệt user, khóa user, bật/tắt relay.

---

# Bổ sung: Calibration & Latest State

## Sensor calibration

```http
GET /devices/{deviceId}/calibrations
POST /devices/{deviceId}/calibrations
PUT /devices/{deviceId}/calibrations/{calibrationId}
DELETE /devices/{deviceId}/calibrations/{calibrationId}
Authorization: Bearer <TOKEN>
```

`POST/PUT/DELETE` chỉ dành cho `ADMIN`. `GET` dành cho người có quyền truy cập thiết bị.

Body mẫu:

```json
{
  "sensorType": "PH",
  "offsetValue": 0.35,
  "slopeValue": 1.02,
  "calibrationPoint1": 6.86,
  "calibrationPoint2": 9.18,
  "note": "Hiệu chuẩn pH bằng dung dịch chuẩn",
  "active": true
}
```

Backend áp dụng công thức:

```text
calibrated_value = raw_value * slope_value + offset_value
```

## Latest device state

```http
GET /devices/{deviceId}/latest-state
Authorization: Bearer <TOKEN>
```

Dùng cho dashboard cần lấy trạng thái mới nhất nhanh hơn thay vì query history.
