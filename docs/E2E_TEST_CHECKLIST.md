# E2E Test Checklist — Demo bảo vệ đồ án

Mục tiêu: chứng minh hệ thống chạy được trọn luồng từ cảm biến đến backend, web/app, cảnh báo và điều khiển relay.

## 0. Chuẩn bị

```text
Backend IP: http://192.168.1.8:8080
API base:   http://192.168.1.8:8080/api
Device ID:  device_01
Admin:      admin / REPLACE_WITH_LOCAL_ADMIN_PASSWORD
User:       user / REPLACE_WITH_LOCAL_USER_PASSWORD
Tech:       tech / REPLACE_WITH_LOCAL_TECH_PASSWORD
API key:    REPLACE_WITH_LOCAL_IOT_API_KEY
Auto-control mặc định: OFF
```

Kiểm tra backend:

```http
GET http://192.168.1.8:8080/api/health
```

Kỳ vọng:

```json
{
  "success": true
}
```

---

## 1. Test đăng nhập admin

```http
POST /api/auth/login
```

Body:

```json
{
  "username": "admin",
  "password": "REPLACE_WITH_LOCAL_ADMIN_PASSWORD"
}
```

Kỳ vọng:

```text
success = true
response.data.token khác null
user.role = ADMIN
```

---

## 2. Test Arduino gửi dữ liệu cảm biến

```http
POST /api/readings
X-API-Key: REPLACE_WITH_LOCAL_IOT_API_KEY
```

Body bình thường:

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

Kỳ vọng:

```text
success = true
message = Sensor reading saved
status = NORMAL hoặc tương ứng
```

Body cảnh báo:

```json
{
  "deviceId": "device_01",
  "temperature": 32.5,
  "ph": 4.5,
  "ecValue": 0.9,
  "salinity": 0.4,
  "doValue": 2.8
}
```

Kỳ vọng:

```text
success = true
status = WARNING hoặc DANGER
alerts được tạo nếu vượt ngưỡng
```

---

## 3. Test Web/App đọc dữ liệu mới nhất

```http
GET /api/readings/latest?deviceId=device_01
Authorization: Bearer <ADMIN_TOKEN>
```

Kỳ vọng:

```text
data.deviceId = device_01
data.temperature có giá trị vừa gửi
data.ph có giá trị vừa gửi
data.doValue có giá trị vừa gửi
```

---

## 4. Test cảnh báo

```http
GET /api/alerts/open?deviceId=device_01
Authorization: Bearer <ADMIN_TOKEN>
```

Kỳ vọng sau khi gửi body cảnh báo:

```text
Có ít nhất 1 alert open nếu pH/DO vượt ngưỡng
```

Nếu có alert id, test xử lý:

```http
POST /api/alerts/{id}/resolve
Authorization: Bearer <ADMIN_TOKEN>
```

Kỳ vọng:

```text
alert.status = RESOLVED hoặc resolved=true tùy response
```

---

## 5. Test điều khiển relay thủ công

Bật máy bơm:

```http
POST /api/commands
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "deviceId": "device_01",
  "relayNo": 1,
  "action": "ON"
}
```

Kỳ vọng:

```text
success = true
command.status = PENDING
```

Arduino/giả lập Arduino lấy lệnh:

```http
GET /api/commands/pending?deviceId=device_01
X-API-Key: REPLACE_WITH_LOCAL_IOT_API_KEY
```

Kỳ vọng:

```text
Có command relayNo=1, action=ON
status chuyển sang SENT nếu backend có xử lý trạng thái này
```

ACK:

```http
POST /api/commands/{id}/ack
X-API-Key: REPLACE_WITH_LOCAL_IOT_API_KEY
```

Body:

```json
{
  "success": true,
  "message": "Relay 1 turned ON"
}
```

Kỳ vọng:

```text
command.status = ACK
relay state relayNo=1 = ON
```

Tắt máy bơm tương tự với `action = OFF`.

---

## 6. Test lịch sử điều khiển

```http
GET /api/commands/history?deviceId=device_01
Authorization: Bearer <ADMIN_TOKEN>
```

Kỳ vọng:

```text
Có command ON/OFF vừa tạo
Có trạng thái ACK sau khi Arduino xác nhận
```

---

## 7. Test trạng thái relay

```http
GET /api/relay-states/device_01
Authorization: Bearer <ADMIN_TOKEN>
```

Kỳ vọng:

```text
relayNo=1 có state ON hoặc OFF theo lệnh cuối cùng đã ACK
```

---

## 8. Test đăng ký và admin duyệt user

Register user mới:

```http
POST /api/auth/register
```

Body:

```json
{
  "username": "khach_demo",
  "password": "123456",
  "fullName": "Khách hàng Demo",
  "phone": "0987654321",
  "email": "khach.demo@example.com"
}
```

Kỳ vọng:

```text
approvalStatus = PENDING
active = false
```

Admin xem pending:

```http
GET /api/users/pending
Authorization: Bearer <ADMIN_TOKEN>
```

Duyệt user:

```http
POST /api/users/{id}/approve
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "role": "USER",
  "pondIds": [1],
  "deviceIds": ["device_01"],
  "accessType": "OWNER"
}
```

Kỳ vọng:

```text
active = true
approvalStatus = APPROVED
role = USER
```

---

## 9. Test khóa/mở khóa user

Khóa:

```http
PATCH /api/users/{id}/deactivate
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "reason": "Test khóa tài khoản trong demo"
}
```

Kỳ vọng:

```text
active = false
user không login được hoặc token cũ không còn dùng được
```

Mở khóa:

```http
PATCH /api/users/{id}/activate
Authorization: Bearer <ADMIN_TOKEN>
```

Body:

```json
{
  "reason": "Test mở khóa tài khoản"
}
```

Kỳ vọng:

```text
active = true
```

---

## 10. Test phân quyền USER / TECHNICIAN

### USER `user / REPLACE_WITH_LOCAL_USER_PASSWORD`

Kỳ vọng:

```text
- Xem được device_01 nếu thuộc ao được gán.
- Không gọi được GET /api/users.
- Không duyệt/khóa user được.
- Không xem được thiết bị/ao không được gán.
```

### TECHNICIAN `tech / REPLACE_WITH_LOCAL_TECH_PASSWORD`

Kỳ vọng:

```text
- Xem được ao/thiết bị được gán.
- Có thể điều khiển nếu accessType = READ_WRITE.
- Không quản trị user.
- Không duyệt/khóa user.
```

---

## 11. Test chatbot

Hỏi kiến thức:

```http
POST /api/chat/message
Authorization: Bearer <TOKEN>
```

Body:

```json
{
  "message": "pH thấp thì xử lý như thế nào?"
}
```

Kỳ vọng:

```text
botMessage giải thích được pH và hướng xử lý
```

Hỏi dữ liệu hệ thống:

```json
{
  "deviceId": "device_01",
  "message": "Ao hiện tại thế nào?"
}
```

Kỳ vọng:

```text
botMessage có nhắc đến nhiệt độ, pH, độ mặn, DO, trạng thái/cảnh báo nếu có
```

---

## 12. Test báo cáo

```http
GET /api/reports/summary?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
Authorization: Bearer <TOKEN>
```

Kỳ vọng:

```text
Có thống kê sensor, alert, command trong khoảng thời gian
```

Export CSV:

```http
GET /api/reports/sensors.csv?deviceId=device_01&from=2026-05-24T00:00:00&to=2026-05-24T23:59:59
```

Kỳ vọng:

```text
Trình duyệt/Postman tải CSV hoặc trả text/csv
```

---

## 13. Test bản đồ số backend

```http
GET /api/map/ponds
GET /api/map/devices
Authorization: Bearer <TOKEN>
```

Kỳ vọng:

```text
Có danh sách ao/thiết bị; nếu đã nhập latitude/longitude thì frontend hiển thị marker được
```

---

## 14. Kết luận pass/fail

Demo được xem là đạt nếu pass các luồng:

```text
1. Login admin thành công.
2. Arduino gửi dữ liệu và backend lưu được.
3. Web/App đọc dữ liệu mới nhất được.
4. Gửi dữ liệu vượt ngưỡng thì có cảnh báo.
5. Web/App tạo lệnh relay.
6. Arduino lấy lệnh và ACK.
7. Command history và relay state cập nhật.
8. User đăng ký, admin duyệt, khóa/mở khóa chạy được.
9. USER/TECHNICIAN không truy cập được API admin.
10. Chatbot trả lời được kiến thức và dữ liệu hệ thống.
```
