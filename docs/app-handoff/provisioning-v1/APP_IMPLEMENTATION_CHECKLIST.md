# App Implementation Checklist - Provisioning v1

## Trước khi code

- Đọc `PROVISIONING_CONTRACT_V1.md`.
- Không lấy Wi-Fi/API key từ source backend.
- Dùng QR mẫu để viết parser và test UI.
- Tách `API_BASE_URL` theo dev/staging/production.

## QR parser

- Chỉ nhận scheme `shrimp-iot` và host `provision`.
- Kiểm tra `v=1`.
- Kiểm tra đủ `deviceId`, `claimCode`, `setupSsid`, `setupPassword`.
- Percent-decode đúng một lần.
- Không log claim/setup code.
- Có test QR thiếu field, sai version, duplicate field và deviceId sai format.

## Claim flow

- Gửi Bearer token lên backend.
- Không gửi setup password lên backend.
- Xử lý riêng `DEVICE_ALREADY_CLAIMED`, `CLAIM_CODE_EXPIRED`, `INVALID_CLAIM_CODE`.
- Lưu `deviceId` sau claim; không lưu `claimCode` lâu dài.

## Local Wi-Fi flow

- Hiển thị hướng dẫn khi setup AP không có Internet.
- Xin quyền Local Network/Wi-Fi theo nền tảng.
- Gửi `X-Setup-Code` cho local API.
- Password Wi-Fi người dùng chỉ tồn tại trong memory trong lúc provisioning.
- Không đưa password vào log, analytics hoặc crash report.

## Waiting for cloud

- Chuyển điện thoại về mạng có Internet sau khi chip đóng setup AP.
- Poll mỗi 3 giây, timeout 90 giây.
- Thành công khi backend trả `connectionStatus=ONLINE`.
- Cho phép retry nếu timeout; không claim lại khi device đã claimed đúng owner.

## Test bắt buộc

- QR mẫu parse thành công.
- QR sai version bị từ chối.
- Sai setup password.
- Sai Wi-Fi password.
- Chip mất điện trong khi provisioning.
- Điện thoại đổi mạng giữa chừng.
- Backend tạm offline.
- Device online và telemetry xuất hiện sau provisioning.
- Lệnh test nhận ACK từ chip.

## Lưu ý hiện trạng

Ở bước 3, contract đã freeze nhưng API chưa chạy. App có thể làm parser/UI/mock; integration thật bắt đầu sau bước 4-5.

