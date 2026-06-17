# Register Approval + Chatbot API

Bản backend này đã bổ sung:

- Đăng ký tài khoản chờ Admin duyệt, có hỗ trợ `fullName`, `phone`, `email`
- Admin duyệt/từ chối/cấp quyền USER hoặc TECHNICIAN
- Chatbot Pha 1: hỏi đáp kiến thức nuôi tôm cơ bản
- Chatbot Pha 2: đọc dữ liệu hệ thống: cảm biến mới nhất, cảnh báo, relay, online/offline
- Lưu lịch sử chat vào PostgreSQL

## 1. Luồng đăng ký và duyệt tài khoản

### Người dùng tự đăng ký

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "khach01",
  "password": "123456",
  "fullName": "Chủ ao nuôi 01",
  "phone": "0987654321",
  "email": "khach01@example.com"
}
```

Sau khi đăng ký:

- `role = USER`
- `active = false`
- `approvalStatus = PENDING`

Người dùng chưa đăng nhập được cho tới khi Admin duyệt.

### Admin xem tài khoản chờ duyệt

```http
GET /api/users/pending
Authorization: Bearer <ADMIN_TOKEN>
```

### Admin duyệt tài khoản

```http
POST /api/users/{id}/approve
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Duyệt làm chủ ao và gán ao:

```json
{
  "role": "USER",
  "pondIds": [1],
  "accessType": "OWNER"
}
```

Hoặc duyệt làm kỹ thuật viên:

```json
{
  "role": "TECHNICIAN",
  "deviceIds": ["device_01"],
  "accessType": "READ_WRITE"
}
```

### Admin từ chối tài khoản

```http
POST /api/users/{id}/reject
Authorization: Bearer <ADMIN_TOKEN>
```

## 2. Chatbot Pha 1 — hỏi đáp kiến thức cơ bản

```http
POST /api/chat/message
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "message": "pH thấp thì xử lý thế nào?"
}
```

Backend sẽ trả lời theo rule-based knowledge base và lưu lịch sử chat.

## 3. Chatbot Pha 2 — đọc dữ liệu hệ thống

Hỏi dữ liệu cảm biến mới nhất:

```json
{
  "deviceId": "device_01",
  "message": "Ao hiện tại thế nào?"
}
```

Hỏi cảnh báo chưa xử lý:

```json
{
  "deviceId": "device_01",
  "message": "Có cảnh báo nào chưa xử lý không?"
}
```

Hỏi relay:

```json
{
  "deviceId": "device_01",
  "message": "Máy bơm và relay đang bật hay tắt?"
}
```

Hỏi thiết bị online/offline:

```json
{
  "deviceId": "device_01",
  "message": "Thiết bị có online không?"
}
```

## 4. Lấy lịch sử chat

```http
GET /api/chat/sessions
Authorization: Bearer <TOKEN>
```

```http
GET /api/chat/sessions/{sessionId}/messages
Authorization: Bearer <TOKEN>
```

## 5. Lưu ý

- Chatbot hiện chưa tự điều khiển relay bằng câu lệnh tự nhiên. Pha 1 và Pha 2 chỉ hỏi đáp + đọc dữ liệu.
- Nếu muốn làm Pha 3, nên bắt người dùng xác nhận trước khi chatbot tạo lệnh bật/tắt máy bơm.
- Arduino không cần sửa code cho chatbot.

## 6. Khóa / mở khóa tài khoản theo hướng chuyên nghiệp

Không xóa cứng tài khoản user. Admin nên khóa tài khoản để giữ lại lịch sử chat, lịch sử cảnh báo và lịch sử điều khiển relay.

### Khóa tài khoản

```http
PATCH /api/users/{id}/deactivate
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

```json
{
  "reason": "Người dùng không còn quản lý ao này"
}
```

### Mở khóa tài khoản

```http
PATCH /api/users/{id}/activate
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

```json
{
  "reason": "Mở lại tài khoản cho người dùng"
}
```

Backend đã chặn admin tự khóa chính mình, chặn khóa admin cuối cùng và thu hồi token cũ khi khóa user.
