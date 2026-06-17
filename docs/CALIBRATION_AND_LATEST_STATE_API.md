# Calibration & Latest State API

Tài liệu này mô tả các phần đã bổ sung để xử lý 2 việc nên ưu tiên trước trong hệ thống IoT ao nuôi:

1. Hiệu chuẩn cảm biến bằng offset/slope.
2. Tối ưu lấy dữ liệu mới nhất bằng bảng `device_latest_states`.

## 1. Sensor calibration

### Mục đích

Cảm biến pH, EC, DO, nhiệt độ và độ mặn có thể bị lệch do môi trường nước, tuổi thọ đầu dò, nhiễu điện hoặc công thức chuyển đổi chưa chính xác. Backend bổ sung bảng `sensor_calibrations` để lưu hệ số hiệu chuẩn cho từng thiết bị và từng loại cảm biến.

Công thức áp dụng:

```text
calibrated_value = raw_value * slope_value + offset_value
```

Backend áp dụng calibration trước khi:

- validate khoảng vật lý;
- lưu `sensor_readings`;
- đánh giá ngưỡng cảnh báo;
- chạy smart/rule-based auto-control.

### Sensor types hỗ trợ

```text
TEMPERATURE
PH
EC
SALINITY
DO
```

Các alias như `DISSOLVED_OXYGEN`, `DO_VALUE`, `EC_VALUE`, `TEMP` được normalize về các type trên.

## 2. API calibration

### 2.1. Lấy danh sách calibration của thiết bị

```http
GET /api/devices/{deviceId}/calibrations
Authorization: Bearer <TOKEN>
```

Quyền:

- ADMIN: xem tất cả.
- USER/TECHNICIAN: xem nếu có quyền với ao chứa thiết bị.

### 2.2. Tạo calibration

```http
POST /api/devices/{deviceId}/calibrations
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Body ví dụ:

```json
{
  "sensorType": "PH",
  "offsetValue": 0.35,
  "slopeValue": 1.02,
  "calibrationPoint1": 6.86,
  "calibrationPoint2": 9.18,
  "note": "Hiệu chuẩn pH bằng dung dịch chuẩn 6.86 và 9.18",
  "active": true
}
```

Response:

```json
{
  "success": true,
  "message": "Sensor calibration created",
  "data": {
    "id": 1,
    "deviceId": "device_01",
    "sensorType": "PH",
    "offsetValue": 0.35,
    "slopeValue": 1.02,
    "active": true
  }
}
```

### 2.3. Cập nhật calibration

```http
PUT /api/devices/{deviceId}/calibrations/{calibrationId}
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

### 2.4. Vô hiệu hóa calibration

```http
DELETE /api/devices/{deviceId}/calibrations/{calibrationId}
Authorization: Bearer <ADMIN_TOKEN>
```

API này không xóa cứng, chỉ set `active = false` để giữ lịch sử hiệu chuẩn.

## 3. Latest device state

### Mục đích

Truy vấn latest reading bằng cách `ORDER BY created_at DESC LIMIT 1` vẫn dùng được nếu đã có index, nhưng khi dashboard gọi liên tục và dữ liệu tăng lớn, backend nên có bảng snapshot trạng thái mới nhất.

Backend đã bổ sung bảng:

```text
device_latest_states
```

Mỗi khi có `sensor_readings` mới, backend tự cập nhật latest state của thiết bị.

### API lấy latest state

```http
GET /api/devices/{deviceId}/latest-state
Authorization: Bearer <TOKEN>
```

Response ví dụ:

```json
{
  "success": true,
  "message": "Latest device state retrieved",
  "data": {
    "deviceId": "device_01",
    "latestReadingId": 123,
    "temperature": 31.38,
    "ph": 4.1,
    "ecValue": 0.48,
    "salinity": 0.24,
    "doValue": 2.16,
    "status": "WARNING",
    "message": "pH vượt ngưỡng thấp; Oxy hòa tan thấp",
    "updatedAt": "2026-06-09T15:30:00"
  }
}
```

Frontend dashboard nên ưu tiên API này nếu chỉ cần trạng thái mới nhất. API `/api/readings/latest` vẫn giữ để tương thích cũ.

## 4. Test nhanh bằng curl Windows

### Tạo calibration pH

```bat
curl.exe -X POST "http://localhost:8080/api/devices/device_01/calibrations" ^
  -H "Authorization: Bearer <ADMIN_TOKEN>" ^
  -H "Content-Type: application/json" ^
  -d "{\"sensorType\":\"PH\",\"offsetValue\":0.35,\"slopeValue\":1.02,\"calibrationPoint1\":6.86,\"calibrationPoint2\":9.18,\"note\":\"Hieu chuan pH\",\"active\":true}"
```

### Publish telemetry từ MQTTX

Topic:

```text
shrimp-iot/devices/device_01/telemetry
```

Payload:

```json
{
  "deviceId": "device_01",
  "temperature": 31.38,
  "ph": 4.10,
  "ecValue": 0.48,
  "salinity": 0.24,
  "doValue": 2.16
}
```

Nếu calibration pH trên đang active, giá trị pH lưu DB sẽ là:

```text
4.10 * 1.02 + 0.35 = 4.53
```

### Lấy latest state

```bat
curl.exe -X GET "http://localhost:8080/api/devices/device_01/latest-state" ^
  -H "Authorization: Bearer <TOKEN>"
```
