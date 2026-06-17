# Backend hoàn thiện các module chính, bỏ qua Camera

Bản này mở rộng backend theo hướng hoàn thiện các module còn lại ngoài camera:

## 1. Quản trị người dùng & phân quyền
- Register với fullName, phone, email.
- Account PENDING, admin approve/reject.
- Role: ADMIN, USER, TECHNICIAN.
- Khóa/mở khóa user theo RESTful API.
- Thu hồi token khi user bị khóa.
- Audit log cho thao tác duyệt/khóa/mở khóa/tạo lệnh.

API chính:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users`
- `GET /api/users/pending`
- `POST /api/users/{id}/approve`
- `POST /api/users/{id}/reject`
- `PATCH /api/users/{id}/deactivate`
- `PATCH /api/users/{id}/activate`
- `GET /api/audit-logs`

## 2. Quản lý hồ nuôi & gán quyền
Bảng `ponds` đã bổ sung:
- `speciesType`
- `pondType`
- `waterVolumeCubicMeters`
- `region`
- `status`
- `latitude`
- `longitude`
- `description`

API chính:
- `GET /api/ponds`
- `POST /api/ponds`
- `PUT /api/ponds/{id}`
- `PATCH /api/ponds/{id}/activate`
- `PATCH /api/ponds/{id}/deactivate`
- `POST /api/ponds/{pondId}/access`
- `GET /api/ponds/{pondId}/access`

## 3. Bản đồ số hồ nuôi
Backend đã có dữ liệu tọa độ và API trả dữ liệu cho frontend dùng Leaflet/Google Maps/Mapbox:
- `GET /api/map/ponds`
- `GET /api/map/devices`

## 4. Giám sát dữ liệu cảm biến
Backend hiện hỗ trợ:
- temperature
- ph
- ecValue
- salinity
- doValue

API:
- `POST /api/readings`
- `GET /api/readings/latest?deviceId=device_01`
- `GET /api/readings/history?deviceId=device_01&limit=50`
- `GET /api/readings/range?deviceId=device_01&from=...&to=...`

## 5. Điều khiển thủ công
- Web/app tạo lệnh điều khiển relay.
- Arduino poll pending command và ACK.
- Backend lưu lịch sử command.
- Kiểm tra quyền `OWNER`, `READ_WRITE`, `CONTROL` khi điều khiển.

API:
- `POST /api/commands`
- `GET /api/commands/pending?deviceId=device_01`
- `POST /api/commands/{id}/ack`
- `GET /api/commands/history?deviceId=device_01`

## 6. Điều khiển tự động
Đã có threshold config, scenario, cooldown, max runtime, relay runtime monitor.

API:
- `POST /api/threshold-configs`
- `GET /api/threshold-configs/{pondId}`
- `POST /api/control-scenarios`
- `GET /api/control-scenarios/pond/{pondId}`

## 7. Quản lý thông báo
Backend đã có notification log và webhook gửi SMS/email theo cấu hình:
- Nếu chưa cấu hình webhook, backend không fail mà ghi log `SKIPPED_NOT_CONFIGURED`.
- App notification được ghi log `CREATED`.
- SMS/email lấy người nhận từ user được gán vào pond, hoặc default recipient trong `application.yml`.

Cấu hình:
```yaml
notification:
  app-enabled: true
  sms-enabled: false
  email-enabled: false
  sms-webhook-url: ""
  email-webhook-url: ""
  default-sms-recipient: ""
  default-email-recipient: ""
```

API:
- `GET /api/notifications?deviceId=device_01`
- `POST /api/notifications/test`

## 8. Báo cáo & phân tích
Backend có báo cáo tổng hợp và export CSV:
- `GET /api/reports/summary?deviceId=device_01&from=2026-05-23T00:00:00&to=2026-05-23T23:59:59`
- `GET /api/reports/sensors.csv?...`
- `GET /api/reports/alerts.csv?...`
- `GET /api/reports/commands.csv?...`

## 9. Chatbot
- Pha 1: hỏi kiến thức nuôi tôm, pH, DO, nhiệt độ, độ mặn, cảnh báo.
- Pha 2: hỏi dữ liệu hệ thống, cảnh báo, relay, thiết bị online/offline.
- Lưu lịch sử chat.

API:
- `POST /api/chat/message`
- `GET /api/chat/sessions`
- `GET /api/chat/sessions/{sessionId}/messages`

## Lưu ý về mức “100%”
Backend đã hoàn thiện API và database cho các module trên. Các phần còn phụ thuộc bên ngoài không thể tự hoàn tất trong backend nếu chưa có hạ tầng thật:
- Bản đồ số cần frontend tích hợp Leaflet/Google Maps/Mapbox.
- SMS/email thật cần webhook/provider thật như Zalo ZNS, Twilio, FPT SMS, SendGrid, SMTP gateway.
- Báo cáo PDF đẹp nên làm sau khi chốt mẫu báo cáo; bản này đã có CSV và summary JSON.
