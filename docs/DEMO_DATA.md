# Demo Data chuẩn

Backend tự tạo dữ liệu ban đầu bằng `DataInitializer` khi database trống.

## Tài khoản mặc định

| Username | Password | Role | Ghi chú |
|---|---|---|---|
| admin | admin123 | ADMIN | Quản trị hệ thống |
| user | user123 | USER | Chủ ao nuôi A |
| user2 | user123 | USER | Chủ ao nuôi B |
| tech | tech123 | TECHNICIAN | Kỹ thuật viên |

## Ao nuôi mặc định

| ID dự kiến | Tên ao | Vị trí | Diện tích | Ghi chú |
|---:|---|---|---:|---|
| 1 | Ao tôm thẻ 01 | Khu A - Bến Tre | 1000 m² | Ao demo chính |
| 2 | Ao tôm thẻ 02 | Khu A - Bến Tre | 1200 m² | Ao demo phụ |
| 3 | Ao tôm thẻ 03 | Khu B - Bến Tre | 1500 m² | Ao demo phân quyền |

Lưu ý: ID có thể khác nếu database đã có dữ liệu trước đó. Khi cần chắc chắn, gọi:

```http
GET /api/ponds
Authorization: Bearer <ADMIN_TOKEN>
```

## Thiết bị mặc định

| Device ID | Tên | Ao |
|---|---|---|
| device_01 | Bộ điều khiển Ao 1 | Ao tôm thẻ 01 |
| device_02 | Bộ điều khiển Ao 2 | Ao tôm thẻ 02 |
| device_03 | Bộ điều khiển Ao 3 | Ao tôm thẻ 03 |

## Quyền mặc định

```text
user  → Ao 1, Ao 2, accessType OWNER
user2 → Ao 3, accessType OWNER
tech  → Ao 1, accessType READ_WRITE
```

## Cảm biến device_01

```text
TEMPERATURE  DS18B20 / D6
PH           A0
EC           A1
SALINITY     A1
DO           A2
```

## Relay device_01

```text
relayNo 1 → Máy bơm lọc nước
relayNo 2 → Quạt tạo dòng oxy
relayNo 3 → Sục khí oxy chính
relayNo 4 → Đèn/dự phòng
```

## Dữ liệu cảm biến demo

### Bình thường

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

### Cảnh báo pH thấp + DO thấp

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

### Cảnh báo nhiệt độ cao

```json
{
  "deviceId": "device_01",
  "temperature": 38.0,
  "ph": 7.5,
  "ecValue": 1.1,
  "salinity": 12.5,
  "doValue": 5.2
}
```

## Lưu ý khi demo

`auto-control.enabled` mặc định là `false`. Nếu muốn tránh relay tự bật/tắt trong lúc bảo vệ, giữ nguyên cấu hình này.
