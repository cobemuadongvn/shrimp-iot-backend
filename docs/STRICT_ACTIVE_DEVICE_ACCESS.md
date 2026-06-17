# Strict active-device access

## Mục tiêu

Bản chỉnh này siết lại backend để các API không còn xử lý nhầm dữ liệu cũ như `deviceId = pond_01` trong bảng `devices`.

## Nguyên tắc mới

Backend chỉ coi là thiết bị thật nếu thỏa mãn đồng thời:

- `status = ACTIVE`
- `deviceId` không rỗng
- `deviceId` không bắt đầu bằng `pond_`

## Các phần đã siết lại

### AuthService

- `validateAccessToDevice(...)` luôn kiểm tra thiết bị là ACTIVE và là thiết bị thật, kể cả với ADMIN.
- `validateWriteAccessToDevice(...)` cũng kiểm tra ACTIVE/thiết bị thật trước khi cho tạo command.
- Thêm `requireActiveRealDevice(deviceId)` để các controller/service dùng chung.
- Thêm `isRealDeviceId(deviceId)` để chặn mã thiết bị dạng `pond_...`.

### DeviceController

- `GET /api/devices/{deviceId}` chỉ trả thiết bị ACTIVE thật.
- `GET /api/devices/{deviceId}/sensors` chỉ trả sensors của thiết bị ACTIVE thật.
- `GET /api/devices/{deviceId}/relays` chỉ trả relays của thiết bị ACTIVE thật.
- `PUT /api/devices/{deviceId}` chỉ cho sửa thiết bị ACTIVE thật.
- `PATCH /api/devices/{deviceId}/deactivate` chỉ cho vô hiệu hóa thiết bị ACTIVE thật.
- `PATCH /api/devices/{deviceId}/activate` cho kích hoạt lại thiết bị thật nhưng vẫn chặn `pond_...`.
- `POST /api/devices` không cho tạo deviceId dạng `pond_...`.

### SensorReadingService

- Telemetry MQTT/HTTP chỉ được lưu nếu device đang ACTIVE và là thiết bị thật.
- Không update lastSeen/ONLINE cho dữ liệu rác hoặc device INACTIVE.

### CommandService

- ACK/status MQTT chỉ cập nhật thiết bị nếu device đang ACTIVE và là thiết bị thật.
- Không mark ONLINE cho `pond_01` hoặc thiết bị INACTIVE.

## SQL xử lý dữ liệu cũ

Nên chạy một lần trong Docker PostgreSQL:

```bat
docker exec -it shrimp-postgres psql -U shrimp_user -d shrimp_iot -c "UPDATE devices SET status = 'INACTIVE' WHERE device_id = 'pond_01';"
```

Kiểm tra:

```bat
docker exec -it shrimp-postgres psql -U shrimp_user -d shrimp_iot -c "SELECT id, device_id, name, status FROM devices ORDER BY id;"
```

## Test mong muốn

- `GET /api/devices` chỉ trả `device_01`.
- `GET /api/devices/pond_01` trả lỗi.
- `POST /api/commands` với `deviceId = pond_01` trả lỗi.
- MQTT telemetry từ `pond_01` không được lưu thành reading mới.
