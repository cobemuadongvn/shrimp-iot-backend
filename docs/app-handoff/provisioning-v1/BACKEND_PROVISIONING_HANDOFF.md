# Backend Device Provisioning v1 - App Handoff

## Trạng thái

- Backend implementation: hoàn thành ở bước 5.
- Service unit test: 5/5 PASS.
- HTTP integration test: PASS trên random port và H2 riêng.
- Namespace: `/api/device-provisioning`.
- App dùng Bearer token hiện có; không kết nối database hoặc MQTT trực tiếp.

## Luồng thật

1. ADMIN đăng ký device bằng API device hiện có.
2. ADMIN gọi endpoint phát claim code. Code chỉ xuất hiện trong response một lần.
3. Công cụ quản trị/sản xuất ghép `deviceId`, `claimCode`, setup SSID và setup password thành QR v1.
4. App đăng nhập, quét QR và gọi claim endpoint.
5. App kết nối vào AP của chip để gửi Wi-Fi.
6. App poll cloud status mỗi 3 giây, tối đa 90 giây.

## Endpoint dành cho app

### Claim device

`POST /api/device-provisioning/claims`

```json
{
  "version": 1,
  "deviceId": "device_01",
  "claimCode": "value-read-from-qr",
  "pondId": 1
}
```

- Thành công: HTTP 201, `claimStatus=CLAIMED`.
- Cùng tài khoản và cùng ao gọi lại: HTTP 201 idempotent.
- Claim code bị xóa khỏi bản ghi ngay sau claim thành công.

### Poll status

`GET /api/device-provisioning/devices/{deviceId}/status`

- Thành công: HTTP 200.
- `connectionStatus`: `UNKNOWN`, `OFFLINE` hoặc `ONLINE`.
- `lastSeenAt` có UTC offset `+07:00` khi có telemetry/status.

## Endpoint quản trị, app không gọi

`POST /api/device-provisioning/devices/{deviceId}/claim-code`

Yêu cầu role `ADMIN`.

```json
{
  "expiresInMinutes": 60
}
```

Giới hạn 5-10080 phút. Response chứa claim code một lần; database chỉ lưu SHA-256. Không đưa response này vào log, analytics hoặc ảnh chụp màn hình.

## Phân quyền claim

- `ADMIN`: mọi ao hợp lệ.
- `TECHNICIAN`: `OWNER`, `READ_WRITE` hoặc `CONTROL` trên ao.
- `USER`: bắt buộc `OWNER` trên ao.

## Response lỗi

```json
{
  "success": false,
  "message": "Claim code is invalid",
  "data": {
    "code": "INVALID_CLAIM_CODE"
  },
  "timestamp": "2026-06-27T16:30:00"
}
```

App rẽ nhánh theo `data.code`, không so sánh text trong `message`.

## Postman

Import `backend-device-provisioning-v1.postman_collection.json` và đặt password ở collection variables local. Không commit password/token/claim code thật.

Collection chạy theo thứ tự 1-8. Device dùng để test phải đã đăng ký và chưa claimed; user phải có đúng quyền trên `pondId`.
