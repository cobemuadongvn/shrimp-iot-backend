# Relay Lock / Unlock API

Mục tiêu: khóa/mở khóa từng bơm/relay riêng lẻ của một thiết bị.

- `ACTIVE device + unlocked relay`: cho phép gửi command ON/OFF.
- `ACTIVE device + locked relay`: backend từ chối command ON/OFF cho relay đó.
- `Unlock relay`: chỉ cho phép điều khiển lại, không tự bật bơm.
- Role được phép khóa/mở khóa theo Cách 1: `ADMIN`, `TECHNICIAN` có quyền quản lý ao chứa thiết bị.

## Lấy danh sách relay của thiết bị

```http
GET /api/devices/{deviceId}/relays
Authorization: Bearer <TOKEN>
```

Ví dụ:

```http
GET http://175.16.16.108:8080/api/devices/device_01/relays
Authorization: Bearer <TOKEN>
```

Mỗi relay trả về có các trường:

```json
{
  "relayNo": 1,
  "name": "Bơm nước vào buồng đo",
  "status": "OFF",
  "locked": false,
  "lockedBy": null,
  "lockedAt": null
}
```

## Khóa một relay/bơm

```http
PATCH /api/devices/{deviceId}/relays/{relayNo}/lock
Authorization: Bearer <TOKEN>
```

Ví dụ khóa bơm 1:

```http
PATCH http://175.16.16.108:8080/api/devices/device_01/relays/1/lock
Authorization: Bearer <TOKEN>
```

Khi khóa, backend sẽ expire các command `PENDING`/`SENT` còn tồn tại của relay đó để tránh lệnh cũ bật lại bơm.

## Mở khóa một relay/bơm

```http
PATCH /api/devices/{deviceId}/relays/{relayNo}/unlock
Authorization: Bearer <TOKEN>
```

Ví dụ mở khóa bơm 1:

```http
PATCH http://175.16.16.108:8080/api/devices/device_01/relays/1/unlock
Authorization: Bearer <TOKEN>
```

## Gửi command ON/OFF

```http
POST /api/commands
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "deviceId": "device_01",
  "relayNo": 1,
  "action": "ON"
}
```

Nếu relay đang khóa, backend trả lỗi và không publish MQTT:

```json
{
  "success": false,
  "message": "Relay 1 of device device_01 is locked. Please unlock it before sending ON/OFF commands.",
  "data": null
}
```
