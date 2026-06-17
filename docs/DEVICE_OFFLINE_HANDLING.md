# Xử lý mất kết nối thiết bị tại ao

## Mục tiêu

Tránh tình huống thiết bị tại ao mất WiFi, mất MQTT, mất nguồn hoặc Arduino treo nhưng giao diện vẫn hiển thị như đang hoạt động bình thường.

## Luồng xử lý

```text
Arduino gửi telemetry / command ACK / MQTT status ONLINE
→ Backend cập nhật devices.last_seen_at
→ Backend đặt devices.connection_status = ONLINE
→ Nếu đang có alert DEVICE_OFFLINE thì tự động RESOLVED
```

Nếu thiết bị mất kết nối:

```text
Cách 1 - MQTT Last Will/status OFFLINE:
Arduino/MQTT broker publish OFFLINE vào shrimp-iot/devices/{deviceId}/status
→ Backend đặt connection_status = OFFLINE
→ Backend tạo alert DEVICE_OFFLINE ngay
→ NotificationService tạo in-app notification, có anti-spam 30 phút
```

```text
Cách 2 - Không nhận được dữ liệu trong thời gian timeout:
DeviceConnectionMonitorService chạy định kỳ
→ Nếu last_seen_at quá DEVICE_OFFLINE_SECONDS
→ Backend đặt connection_status = OFFLINE
→ Backend tạo alert DEVICE_OFFLINE
```

## Cấu hình

Trong `application.yml`:

```yaml
device:
  offline:
    seconds: ${DEVICE_OFFLINE_SECONDS:60}
    check:
      ms: ${DEVICE_OFFLINE_CHECK_MS:30000}
```

Trong `.env`:

```env
DEVICE_OFFLINE_SECONDS=60
DEVICE_OFFLINE_CHECK_MS=30000
```

## Anti-spam thông báo

Khi thiết bị offline, backend tạo alert `DEVICE_OFFLINE` và notification trong app. Nếu thiết bị vẫn offline, các lần phát hiện tiếp theo không spam thông báo liên tục. NotificationService áp dụng cooldown theo cấu hình:

```env
NOTIFICATION_ANTI_SPAM_ENABLED=true
NOTIFICATION_COOLDOWN_MINUTES=30
```

## Trạng thái phục hồi

Khi thiết bị gửi telemetry, ACK hoặc status ONLINE trở lại:

```text
Backend cập nhật connection_status = ONLINE
Backend cập nhật last_seen_at = now
Backend tự resolve alert DEVICE_OFFLINE
Frontend/app thấy thiết bị online trở lại
```

## Cách trả lời hội đồng

Hệ thống có cơ chế phát hiện mất kết nối thiết bị tại ao. Mỗi thiết bị có `lastSeenAt` và `connectionStatus`. Khi thiết bị gửi dữ liệu hoặc phản hồi lệnh, backend cập nhật trạng thái `ONLINE`. Nếu thiết bị quá thời gian cấu hình mà không gửi dữ liệu, scheduler của Spring Boot chuyển thiết bị sang `OFFLINE`, tạo alert `DEVICE_OFFLINE` và ghi thông báo trong app. Ngoài ra, MQTT Last Will/status topic giúp backend nhận trạng thái `OFFLINE` ngay khi broker phát hiện thiết bị rớt kết nối, không cần chờ hết timeout. Khi thiết bị kết nối lại, backend tự chuyển `ONLINE` và resolve cảnh báo offline.

## Giới hạn hiện tại

Hệ thống phát hiện được trạng thái mất kết nối ở mức thiết bị không còn gửi dữ liệu hoặc báo OFFLINE. Hệ thống chưa phân loại chính xác nguyên nhân vật lý là mất điện, mất WiFi, mất broker hay Arduino treo. Đây là hướng phát triển tiếp theo.
