# In-app Notification API

Tài liệu này mô tả phần thông báo trong app/web cho hệ thống IoT ao nuôi.

## 1. Mục tiêu

Khi backend phát hiện cảnh báo môi trường, hệ thống tạo bản ghi thông báo trong bảng `notification_logs` với `channel = APP`. Frontend hoặc app mobile đọc các bản ghi này để hiển thị chuông thông báo, danh sách cảnh báo và trạng thái đã đọc/chưa đọc.

Cơ chế này không gửi tin nhắn ra SMS/Zalo/Email, nên phù hợp cho demo đồ án và tránh phụ thuộc nhà cung cấp ngoài.

## 2. Bảng dữ liệu

Bảng: `notification_logs`

Các cột quan trọng cho in-app notification:

| Cột | Ý nghĩa |
|---|---|
| `id` | Mã thông báo |
| `device_id` | Thiết bị/ao phát sinh cảnh báo |
| `event_key` | Khóa gom nhóm chống spam, ví dụ `ALERT:12:PH_LOW` |
| `alert_type` | Loại cảnh báo, ví dụ `PH_LOW`, `DO_LOW` |
| `severity` | Mức cảnh báo: `WARNING`, `DANGER` |
| `channel` | Với thông báo trong app là `APP` |
| `recipient_user_id` | ID người nhận trong hệ thống |
| `recipient_username` | Username người nhận |
| `message` | Nội dung thông báo |
| `status` | Trạng thái log, ví dụ `CREATED`, `SUPPRESSED_COOLDOWN` |
| `read_flag` | Đã đọc hay chưa |
| `read_at` | Thời điểm đọc |
| `read_by` | Người đánh dấu đã đọc |
| `suppressed` | Có bị chặn bởi anti-spam hay không |
| `cooldown_until` | Thời điểm hết cooldown nếu bị chặn |
| `created_at` | Thời điểm tạo thông báo |

## 3. Luồng tạo in-app notification

```text
Cảm biến gửi dữ liệu
→ Backend kiểm tra rule QCVN + AI final status
→ Nếu có cảnh báo mới
→ AlertService tạo alert OPEN
→ NotificationService tạo notification_logs channel = APP
→ Frontend đọc API hoặc nhận WebSocket
```

Người nhận in-app notification:

```text
- ADMIN active: nhận toàn bộ thông báo trong app
- USER/TECHNICIAN active được gán quyền với ao: nhận thông báo của ao đó
```

## 4. API cho frontend

Tất cả API cần header:

```http
Authorization: Bearer <token>
```

### 4.1. Lấy danh sách notification trong app

```http
GET /api/notifications/in-app?deviceId=device_01&unreadOnly=false&limit=50
```

Tham số:

| Tham số | Bắt buộc | Ý nghĩa |
|---|---:|---|
| `deviceId` | Có | Thiết bị cần xem thông báo |
| `unreadOnly` | Không | `true` chỉ lấy thông báo chưa đọc |
| `limit` | Không | Số bản ghi trả về, tối đa 100 |

Ví dụ response:

```json
{
  "success": true,
  "message": "In-app notifications",
  "data": [
    {
      "id": 101,
      "deviceId": "device_01",
      "alertType": "PH_LOW",
      "severity": "WARNING",
      "channel": "APP",
      "recipientUserId": 2,
      "recipientUsername": "tech",
      "title": "Cảnh báo: PH_LOW",
      "message": "pH thấp: 6.6, ngoài ngưỡng QCVN 7.0 - 9.0",
      "status": "CREATED",
      "read": false,
      "createdAt": "2026-06-13T21:20:00"
    }
  ],
  "timestamp": "2026-06-13T21:20:02"
}
```

### 4.2. Đếm notification chưa đọc

```http
GET /api/notifications/in-app/unread-count?deviceId=device_01
```

Response:

```json
{
  "success": true,
  "message": "Unread in-app notification count",
  "data": {
    "deviceId": "device_01",
    "unreadCount": 3
  }
}
```

### 4.3. Đánh dấu một notification là đã đọc

```http
PATCH /api/notifications/in-app/101/read
```

### 4.4. Đánh dấu tất cả notification của thiết bị là đã đọc

```http
PATCH /api/notifications/in-app/read-all?deviceId=device_01
```

Response:

```json
{
  "success": true,
  "message": "All in-app notifications marked as read",
  "data": {
    "deviceId": "device_01",
    "updated": 3
  }
}
```

## 5. WebSocket realtime

Backend publish notification mới vào:

```text
/topic/device/{deviceId}/notifications
/topic/user/{recipientUserId}/notifications
```

Frontend có thể dùng STOMP/SockJS kết nối endpoint:

```text
/ws
```

Nếu chưa làm realtime, frontend có thể polling:

```text
GET /api/notifications/in-app/unread-count?deviceId=device_01
mỗi 10-30 giây
```

## 6. Anti-spam

Anti-spam vẫn hoạt động trước khi tạo thông báo trong app.

```text
pH thấp lần đầu
→ tạo alert mới
→ tạo APP notification

pH vẫn thấp trong 30 phút
→ không tạo APP notification mới
→ chỉ ghi log SYSTEM / SUPPRESSED_COOLDOWN

pH bình thường trở lại
→ alert RESOLVED

pH thấp lại sau đó
→ tạo alert mới
→ tạo APP notification mới
```

Nhờ vậy, frontend/app không bị spam notification mỗi phút.
