# Shrimp IoT Device Provisioning Contract v1

## Contract status

- Contract: `FROZEN_FOR_IMPLEMENTATION`
- Firmware implementation: `HARDWARE_VERIFIED`
- Backend implementation: `IMPLEMENTED_AND_TESTED`
- Version: `1`
- Frozen date: `2026-06-27`

Thay đổi field, endpoint hoặc state của v1 phải cập nhật changelog. Không âm thầm đổi contract sau khi đội app bắt đầu tích hợp.

## 1. Mục tiêu

Contract này thống nhất ba thành phần:

1. App quét QR và claim device với backend.
2. App gửi Wi-Fi credential trực tiếp cho chip qua Access Point tạm.
3. Chip kết nối MQTT cloud; backend báo trạng thái online cho app.

Backend không nhận và không lưu Wi-Fi password. App không giữ database credential, IoT API key hoặc MQTT credential.

## 2. QR format

QR chứa một URI theo custom scheme:

```text
shrimp-iot://provision?v=1&deviceId={deviceId}&claimCode={claimCode}&setupSsid={setupSsid}&setupPassword={setupPassword}
```

Tất cả query values phải được percent-encode theo UTF-8. App bỏ qua field chưa biết để tương thích phiên bản sau, nhưng phải từ chối nếu thiếu field bắt buộc hoặc có field bắt buộc bị lặp.

### Field rules

| Field | Bắt buộc | Quy tắc |
|---|---|---|
| `v` | Có | Phải bằng `1` |
| `deviceId` | Có | Regex `^[a-z0-9][a-z0-9_-]{2,63}$` |
| `claimCode` | Có | Chuỗi opaque 20-128 ký tự; dùng một lần; backend chỉ lưu hash |
| `setupSsid` | Có | 1-32 byte UTF-8; Access Point do chip phát |
| `setupPassword` | Có | 12-63 ký tự; mật khẩu WPA2 setup tạm |

### QR security

- QR chứng minh người dùng có quyền tiếp cận vật lý thiết bị, không thay thế đăng nhập app.
- Claim yêu cầu Bearer token và quyền trên pond.
- `claimCode` chỉ sử dụng thành công một lần; lần claim lại trả conflict.
- Không log `claimCode` hoặc `setupPassword` vào analytics/crash report.
- QR không chứa Wi-Fi password của nơi lắp, MQTT password, IoT API key hay database credential.

## 3. Local chip provisioning API

### Network

- Setup AP mặc định: giá trị `setupSsid` trong QR.
- WPA2 password: `setupPassword` trong QR.
- Base URL: `http://192.168.4.1`
- API version prefix: `/v1`
- Mutating request phải gửi `X-Setup-Code: {setupPassword}`.
- Chip không trả lại hoặc log Wi-Fi password.
- Setup AP tự đóng sau 10 phút không hoạt động; có thể bật lại bằng thao tác reset provisioning vật lý.

### GET `/v1/provision/status`

Response `200`:

```json
{
  "version": 1,
  "deviceId": "device_01",
  "state": "SETUP_AP",
  "lastError": null
}
```

Local states:

- `SETUP_AP`
- `WIFI_CONNECTING`
- `MQTT_CONNECTING`
- `ONLINE`
- `WIFI_FAILED`
- `MQTT_FAILED`

### GET `/v1/provision/networks`

Header: `X-Setup-Code`.

Response `200`:

```json
{
  "networks": [
    {"ssid": "Farm-WiFi", "rssi": -52, "secure": true}
  ]
}
```

Không trả password đã lưu. Có thể loại SSID trùng và sắp xếp RSSI giảm dần.

### POST `/v1/provision/wifi`

Header: `X-Setup-Code`.

Request:

```json
{
  "ssid": "Farm-WiFi",
  "password": "user-entered-password"
}
```

Response `202` trước khi chip chuyển mạng:

```json
{
  "accepted": true,
  "deviceId": "device_01",
  "state": "WIFI_CONNECTING",
  "cloudPollAfterMs": 3000
}
```

Chip chỉ thay thế cấu hình Wi-Fi đang hoạt động sau khi kết nối mới thành công. Nếu thất bại, chip mở lại setup AP và báo `WIFI_FAILED`.

### DELETE `/v1/provision/wifi`

Header: `X-Setup-Code`.

Response `204`. Chip xóa Wi-Fi đã lưu, tắt relay theo fail-safe và trở về `SETUP_AP`.

### Local API errors

```json
{
  "error": {
    "code": "INVALID_SETUP_CODE",
    "message": "Setup code is invalid"
  }
}
```

| HTTP | Code | Ý nghĩa |
|---|---|---|
| 400 | `INVALID_REQUEST` | JSON/field không hợp lệ |
| 401 | `INVALID_SETUP_CODE` | Thiếu hoặc sai setup code |
| 409 | `PROVISIONING_BUSY` | Chip đang thử kết nối |
| 422 | `WIFI_CREDENTIALS_REJECTED` | Wi-Fi credential không được chấp nhận |
| 500 | `INTERNAL_ERROR` | Lỗi firmware không dự kiến |

## 4. Backend cloud provisioning API

Namespace mới: `/api/device-provisioning`.

Các API này được triển khai ở bước 5. API device hiện có vẫn được giữ để xem, link, vận hành và latest-state.

Endpoint quản trị bổ sung, không dành cho app:

- `POST /api/device-provisioning/devices/{deviceId}/claim-code`: chỉ `ADMIN`, sinh claim code ngẫu nhiên 256-bit và chỉ trả giá trị thô một lần. Database chỉ lưu SHA-256.

### POST `/api/device-provisioning/claims`

Yêu cầu `Authorization: Bearer {token}`.

Request:

```json
{
  "version": 1,
  "deviceId": "device_01",
  "claimCode": "opaque-one-time-code",
  "pondId": 1
}
```

Response `201`:

```json
{
  "success": true,
  "message": "Device claimed successfully",
  "data": {
    "deviceId": "device_01",
    "pondId": 1,
    "claimStatus": "CLAIMED",
    "connectionStatus": "UNKNOWN"
  }
}
```

Quyền:

- `ADMIN`: claim vào mọi pond hợp lệ.
- `TECHNICIAN`: cần `OWNER`, `READ_WRITE` hoặc `CONTROL` trên pond.
- `USER`: cần access type `OWNER` trên pond.

Claim cùng device bởi cùng owner có thể trả kết quả idempotent. Claim code đã dùng bởi tài khoản khác trả `409 DEVICE_ALREADY_CLAIMED`.

### GET `/api/device-provisioning/devices/{deviceId}/status`

Yêu cầu Bearer token và quyền truy cập device.

Response `200`:

```json
{
  "success": true,
  "message": "Provisioning status retrieved",
  "data": {
    "deviceId": "device_01",
    "claimStatus": "CLAIMED",
    "connectionStatus": "ONLINE",
    "lastSeenAt": "2026-06-27T15:30:00+07:00"
  }
}
```

Cloud claim states: `UNCLAIMED`, `CLAIMED`, `REVOKED`.

Cloud connection states: `UNKNOWN`, `OFFLINE`, `ONLINE`.

### Backend errors

| HTTP | Code | Ý nghĩa |
|---|---|---|
| 400 | `UNSUPPORTED_PROVISIONING_VERSION` | App gửi version không hỗ trợ |
| 401 | `UNAUTHORIZED` | Thiếu/sai Bearer token |
| 403 | `POND_ACCESS_DENIED` | Không có quyền claim vào pond |
| 404 | `DEVICE_NOT_FOUND` | Device chưa được đăng ký |
| 404 | `POND_NOT_FOUND` | Pond không tồn tại |
| 409 | `DEVICE_ALREADY_CLAIMED` | Device đã thuộc chủ khác |
| 410 | `CLAIM_CODE_EXPIRED` | Claim code hết hạn/bị thu hồi |
| 422 | `INVALID_CLAIM_CODE` | Claim code không hợp lệ |

Error code nằm tại `data.code` trong envelope `ApiResponse`. App không so sánh nội dung `message`.

## 5. App state machine

```text
SCAN_QR
  -> CLAIMING
  -> JOINING_SETUP_AP
  -> SENDING_WIFI
  -> WAITING_FOR_CLOUD
  -> ONLINE
```

Failure states:

- `QR_INVALID`
- `CLAIM_FAILED`
- `SETUP_AP_UNREACHABLE`
- `WIFI_REJECTED`
- `CLOUD_TIMEOUT`

App chờ tối đa 90 giây ở `WAITING_FOR_CLOUD`, poll status mỗi 3 giây. Cho phép retry mà không yêu cầu scan QR lại nếu claim đã thành công.

## 6. Compatibility decisions

- App không được gửi MQTT/backend host cho chip; endpoint cloud được đóng trong firmware/config thiết bị để tránh redirect độc hại.
- App native gọi local HTTP; web portal nếu có phải được chip phục vụ cùng origin.
- Android/iOS phải xin quyền Local Network/Wi-Fi tương ứng.
- Backend response tiếp tục dùng envelope `ApiResponse` hiện có.
- Thời gian cloud dùng ISO-8601 có offset.

## 7. Sample artifacts

- `qr-payload.sample.txt`: URI chính xác được encode trong QR mẫu.
- `qr-payload.sample.json`: dữ liệu sau khi app parse.
- `sample-provisioning-qr.png`: QR quét được nhưng dùng claim/setup code demo, không claim thiết bị thật.
