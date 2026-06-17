# Technician Device Management

## Mục tiêu

Cho phép TECHNICIAN quản lý thiết bị trong phạm vi ao được phân quyền, nhưng không mở toàn quyền như ADMIN.

## Quy tắc quyền

```text
ADMIN
- Tạo thiết bị mới, có thể chưa gán ao.
- Gán thiết bị vào mọi ao.
- Cập nhật toàn bộ thông tin thiết bị.
- Kích hoạt/vô hiệu hóa mọi thiết bị.

TECHNICIAN
- Chỉ được tạo thiết bị khi truyền pondId của ao mà tài khoản có accessType = OWNER / READ_WRITE / CONTROL.
- Chỉ được sửa thiết bị thuộc ao có accessType = OWNER / READ_WRITE / CONTROL.
- Chỉ được kích hoạt/vô hiệu hóa thiết bị thuộc ao có accessType = OWNER / READ_WRITE / CONTROL.
- Không được cập nhật trực tiếp connectionStatus qua PUT.

USER
- Không được tạo/sửa/kích hoạt/vô hiệu hóa/gán thiết bị.
```

## API tạo thiết bị bởi TECHNICIAN

```http
POST /api/devices?pondId=1
Authorization: Bearer <TECH_TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "deviceId": "device_04",
  "name": "Thiết bị ao 1 - phía Đông",
  "latitude": 10.2435,
  "longitude": 106.3752,
  "installationPosition": "Bờ ao phía Đông"
}
```

Điều kiện: TECHNICIAN phải có quyền OWNER / READ_WRITE / CONTROL ở ao `pondId=1`.

## API cập nhật thiết bị bởi TECHNICIAN

```http
PUT /api/devices/device_04
Authorization: Bearer <TECH_TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "name": "Thiết bị ao 1 - vị trí mới",
  "latitude": 10.2440,
  "longitude": 106.3760,
  "installationPosition": "Bờ ao phía Bắc"
}
```

TECHNICIAN chỉ cập nhật các trường thông tin vận hành/lắp đặt. Nếu gửi `status` hoặc `connectionStatus` trong body, backend sẽ bỏ qua.

## API vô hiệu hóa/kích hoạt lại thiết bị

```http
PATCH /api/devices/device_04/deactivate
Authorization: Bearer <TECH_TOKEN>
```

```http
PATCH /api/devices/device_04/activate
Authorization: Bearer <TECH_TOKEN>
```

Điều kiện: thiết bị phải thuộc ao mà TECHNICIAN có quyền OWNER / READ_WRITE / CONTROL.

## API gán thiết bị vào ao

```http
POST /api/devices/device_04/link?pondId=1
Authorization: Bearer <TECH_TOKEN>
```

Điều kiện:

```text
- TECHNICIAN phải có quyền OWNER / READ_WRITE / CONTROL ở ao đích.
- Nếu thiết bị đang thuộc ao khác, TECHNICIAN cũng phải có quyền OWNER / READ_WRITE / CONTROL ở ao hiện tại của thiết bị.
```
