# App Handoff: Device Claim And Wi-Fi Provisioning

## Trạng thái tài liệu

- Phiên bản tài liệu handoff: v0.3.
- Contract kỹ thuật: Provisioning Contract v1 đã được đóng băng ở bước 3.
- Mục tiêu: chốt ranh giới trách nhiệm và contract dự kiến cho đội mobile app.
- Local provisioning API đã được implement, nạp và kiểm thử trên Arduino UNO R4 WiFi ở bước 4.
- Backend claim/status API đã được triển khai và unit test ở bước 5.

Tài liệu chính thức của v1:

- `docs/app-handoff/provisioning-v1/PROVISIONING_CONTRACT_V1.md`
- `docs/app-handoff/provisioning-v1/APP_IMPLEMENTATION_CHECKLIST.md`
- `docs/app-handoff/provisioning-v1/qr-payload.sample.txt`
- `docs/app-handoff/provisioning-v1/qr-payload.sample.json`
- `docs/app-handoff/provisioning-v1/sample-provisioning-qr.png`
- `docs/app-handoff/provisioning-v1/FIRMWARE_PROVISIONING_TEST_GUIDE.md`
- `docs/app-handoff/provisioning-v1/local-chip-provisioning-v1.postman_collection.json`
- `docs/app-handoff/provisioning-v1/BACKEND_PROVISIONING_HANDOFF.md`
- `docs/app-handoff/provisioning-v1/backend-device-provisioning-v1.postman_collection.json`

## Ranh giới trách nhiệm

### Firmware chip

- Tạo Access Point tạm khi chưa có Wi-Fi hoặc khi người dùng yêu cầu reset provisioning.
- Cung cấp local provisioning API.
- Nhận Wi-Fi credential trực tiếp từ app trên mạng local.
- Lưu credential vào bộ nhớ không mất dữ liệu khi tắt nguồn.
- Kết nối MQTT cloud và phát trạng thái online/offline.

### Mobile app

- Quét QR trên thiết bị.
- Claim thiết bị vào tài khoản/ao qua backend cloud.
- Hướng dẫn người dùng kết nối Access Point của chip.
- Gửi Wi-Fi credential trực tiếp cho chip.
- Không gửi Wi-Fi password lên backend.
- Theo dõi backend cho tới khi thiết bị online.

### Backend

- Xác thực claim code và quyền người dùng.
- Gắn device với tài khoản/ao.
- Không nhận và không lưu Wi-Fi password.
- Nhận trạng thái thiết bị từ MQTT.
- Cung cấp API/WebSocket trạng thái online cho app.

## QR payload dự kiến

QR chỉ nên chứa dữ liệu bootstrap:

| Trường | Mô tả |
|---|---|
| `version` | Phiên bản provisioning contract |
| `deviceId` | Định danh duy nhất của chip |
| `claimCode` | Mã claim một lần hoặc có hạn sử dụng |
| `setupSsid` | Tên Access Point tạm của chip |
| `setupCode` | Mật khẩu/setup code tạm |

QR không chứa Wi-Fi password của người dùng, database password, IoT API key hoặc MQTT password lâu dài.

## Local chip API dự kiến

Base URL khi điện thoại kết nối Access Point của chip:

`http://192.168.4.1`

| Method | Path | Mục đích |
|---|---|---|
| `GET` | `/provision/status` | Trạng thái provisioning hiện tại |
| `GET` | `/provision/networks` | Danh sách Wi-Fi chip quét được |
| `POST` | `/provision/wifi` | Gửi SSID/password trực tiếp cho chip |
| `POST` | `/provision/apply` | Lưu và thử kết nối |
| `POST` | `/provision/reset` | Xóa cấu hình Wi-Fi đã lưu |

Request/response chính thức, timeout và error code đã được đóng băng trong Provisioning Contract v1.

## Cloud API dự kiến cho app

Backend hiện đã có auth, device link, latest state và các API vận hành. Bước 5 sẽ kiểm tra khả năng tái sử dụng và bổ sung tối thiểu:

- Claim device bằng `deviceId` và `claimCode`.
- Lấy provisioning/connection status.
- Nhận realtime event khi device online.
- Test measure/command sau provisioning.

App chỉ dùng HTTPS/WSS tới backend. App không kết nối trực tiếp Supabase và không giữ MQTT credential.

## Luồng màn hình app

1. Người dùng đăng nhập.
2. Chọn `Thêm thiết bị`.
3. Quét QR.
4. Backend xác nhận claim code.
5. App hướng dẫn kết nối Wi-Fi setup của chip.
6. Người dùng chọn/nhập Wi-Fi tại nơi lắp đặt.
7. App gửi credential cho local chip API.
8. Chip đóng Access Point và kết nối Internet/MQTT.
9. App chuyển lại mạng Internet và chờ backend báo `ONLINE`.
10. App chạy phép đo hoặc lệnh kiểm tra.

## Yêu cầu bảo mật cho app

- Không log hoặc lưu plaintext Wi-Fi password lâu dài.
- Xóa password khỏi state sau khi provisioning kết thúc.
- Không đưa claim code vào analytics/crash report.
- Yêu cầu quyền Local Network/Wi-Fi phù hợp trên Android và iOS.
- Hiển thị rõ khi điện thoại đang kết nối mạng setup không có Internet.
- Cho phép retry và reset nếu nhập sai password.

## Cách test trước khi app hoàn thành

- Dùng trình duyệt điện thoại hoặc Postman gọi local chip API.
- Dùng QR mẫu để kiểm tra parser.
- Dùng backend staging và tài khoản test để claim thiết bị.
- Kiểm tra telemetry, command, ACK và trạng thái offline/online.

## Tiêu chí bàn giao cho đội app

- Contract được đánh dấu `READY` và có version cố định.
- Có QR mẫu hợp lệ và QR lỗi.
- Có Postman collection cho backend và local chip API.
- Có backend staging URL và tài khoản test.
- Có bảng error code và timeout/retry policy.
- Test thành công trên ít nhất một điện thoại Android thật; iOS kiểm tra riêng nếu app hỗ trợ.
