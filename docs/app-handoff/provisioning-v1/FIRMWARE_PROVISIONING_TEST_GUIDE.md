# Firmware Provisioning v1 - Hardware Test Guide

## Trạng thái

- Firmware source: implemented.
- UNO R4 WiFi compile: PASS.
- Upload vào chip: PASS trên COM3 ngày 2026-06-27.
- Hardware/API core test: PASS.

## Kết quả kiểm thử 2026-06-27

- Compile/upload UNO R4 WiFi: PASS; flash 112868/262144 byte (43%), RAM global 9084/32768 byte (27%).
- Boot bình thường: Wi-Fi và MQTT `ONLINE`, telemetry publish PASS.
- Relay fail-safe: `pump1 on` bị chặn trong setup mode; D2-D5 giữ OFF.
- Setup AP: `ShrimpIoT-device_01` WPA2 được Windows phát hiện ở `192.168.4.1`.
- `GET /v1/provision/status`: 200, state `SETUP_AP`.
- `GET /v1/provision/networks`: 200, trả 10 mạng tại thời điểm test.
- Setup code sai: 401 `INVALID_SETUP_CODE`.
- JSON lỗi: 400 `INVALID_REQUEST`.
- Mật khẩu 1-7 ký tự: 422 `WIFI_CREDENTIALS_REJECTED`.
- Wi-Fi hợp lệ: 202 `WIFI_CONNECTING`, sau đó MQTT `ONLINE`.
- Reset chip: đọc lại credential EEPROM, MQTT và telemetry tự phục hồi; bốn relay vẫn OFF.
- Không phát hiện secret trong output kiểm thử hoặc tracked files.

Các bài test thao tác vật lý D7-GND, nhập Wi-Fi không tồn tại và chờ trọn idle timeout 10 phút vẫn được giữ trong checklist regression khi nghiệm thu thiết bị hàng loạt; chúng không chặn bước cloud tiếp theo.

## File firmware

- `shrimp_iot_uno_r4_complete.ino`: tích hợp provisioning với MQTT/sensor/relay.
- `provisioning.h`: public interface và state.
- `provisioning.cpp`: EEPROM, setup AP, HTTP API và trang test.
- `arduino_secrets.h`: secret local, không commit.

## Chuẩn bị phần cứng

- Board Arduino UNO R4 WiFi.
- D7 để trống khi chạy bình thường.
- Để reset provisioning: nối D7 với GND, cấp nguồn/reset và giữ ít nhất 5 giây.
- Relay D2-D5 sẽ bị ép OFF trong setup mode.

## Nạp firmware

1. Mở sketch `shrimp_iot_uno_r4_complete.ino` trong Arduino IDE.
2. Kiểm tra `arduino_secrets.h` có đủ bốn macro.
3. Chọn board Arduino UNO R4 WiFi và đúng COM port.
4. Verify trước, sau đó Upload.
5. Mở Serial Monitor ở 115200 baud.

Không upload trong khi hệ thống đang điều khiển bơm thật. Đưa toàn bộ relay/tải về trạng thái an toàn trước.

## Bật setup mode

Có ba trường hợp:

1. Wi-Fi đã lưu và Wi-Fi fallback đều thất bại: chip tự mở setup mode.
2. Gửi serial command `wifi setup`.
3. Giữ D7-GND 5 giây khi boot để xóa Wi-Fi đã lưu và mở setup mode.

Serial Monitor phải hiển thị setup SSID và URL, nhưng không được in setup password hoặc Wi-Fi password.

## Test bằng trình duyệt

1. Điện thoại/laptop kết nối SSID `ShrimpIoT-device_01`.
2. Password lấy từ `SECRET_SETUP_AP_PASSWORD` trong file local secret.
3. Mở `http://192.168.4.1`.
4. Nhập setup code (cùng giá trị setup AP password), Wi-Fi SSID và password muốn cấp.
5. Nhấn Connect.
6. Kỳ vọng response `202`, setup AP đóng và chip thử Wi-Fi mới.
7. Nếu thành công, Wi-Fi mới được ghi EEPROM và chip kết nối MQTT.
8. Nếu thất bại, chip giữ credential cũ và mở lại setup AP với state `WIFI_FAILED`.

## Test bằng Postman

Import `local-chip-provisioning-v1.postman_collection.json` và đặt:

- `chipBaseUrl=http://192.168.4.1`
- `setupCode`: giá trị local, không share.
- `wifiSsid`: Wi-Fi test.
- `wifiPassword`: password Wi-Fi test.

Chạy theo thứ tự:

1. Get provisioning status.
2. List nearby networks.
3. Apply Wi-Fi credentials.
4. Poll backend/MQTT sau khi điện thoại trở lại Internet.

Không chạy request Delete/Clear Wi-Fi nếu chưa chuẩn bị cấu hình lại thiết bị.

## Test regression trước khi phát hành thiết bị hàng loạt

- Trong setup mode, thử serial `pump1 on`: phải bị chặn.
- Kiểm tra D2-D5 đều OFF.
- Gửi setup code sai: API trả `401 INVALID_SETUP_CODE`.
- Gửi JSON lỗi: API trả `400 INVALID_REQUEST`.
- Password 1-7 ký tự: API trả `422 WIFI_CREDENTIALS_REJECTED`.
- Không thao tác 10 phút: AP đóng và cooldown 60 giây.
- Nhập sai Wi-Fi: AP phải mở lại, credential cũ không bị ghi đè.
- Nhập đúng Wi-Fi: power-cycle, chip phải đọc credential EEPROM và kết nối lại.

## Điều kiện nghiệm thu thiết bị hàng loạt

- Tất cả test fail-safe đạt.
- Không log secret.
- API response khớp Contract v1.
- Sau power-cycle chip tự kết nối Wi-Fi đã cấp.
- MQTT telemetry, command và ACK vẫn hoạt động.
- Không ảnh hưởng đọc sensor và watchdog/fail-safe hiện có.
