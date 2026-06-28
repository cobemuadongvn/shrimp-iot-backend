# Bước 8 - Managed MQTT Cloud TLS

## Trạng thái

Hoàn thành ngày 2026-06-28. Backend và firmware đã giao tiếp hai chiều qua
HiveMQ Cloud bằng MQTT TLS; telemetry được lưu vào Supabase và command nhận ACK
từ chip thật.

## Dịch vụ được chọn

HiveMQ Cloud Serverless Free:

- Cluster đang chạy trên HiveMQ Cloud, cổng MQTT TLS `8883`.
- Tối đa 100 kết nối và 10 GB traffic mỗi tháng theo giới hạn gói hiện tại.
- Hỗ trợ MQTT 3.1.1, TLS/SSL và permission theo topic.
- Host công khai được cấu hình qua environment/secret header, không hard-code mật khẩu.

## Kiến trúc

```text
Arduino device_01 -- MQTT TLS/8883 --> HiveMQ Cloud <-- MQTT TLS/8883 -- Backend
       |                                     |
       | publish telemetry/ack/status        | subscribe all device topics
       | subscribe commands                  | publish commands
       v                                     v
shrimp-iot/devices/device_01/#       shrimp-iot/devices/#
                                             |
                                             v
                                      Supabase PostgreSQL
```

App/web không giữ MQTT credential và không kết nối trực tiếp broker.

## Permission và credential đã tạo

### Backend

- Permission: `backend-all-devices`
- Topic filter: `shrimp-iot/devices/#`
- Activity: Publish and Subscribe
- Credential username: `shrimp-backend`

### Chip device_01

- Permission: `device-01-own-topics`
- Topic filter: `shrimp-iot/devices/device_01/#`
- Activity: Publish and Subscribe
- Credential username: `device-01`

Hai mật khẩu khác nhau và chỉ nằm trong các file local bị Git ignore. Credential
chip không truy cập được namespace của thiết bị khác.

## Thay đổi backend

- Hỗ trợ broker URL `ssl://<host>:8883`.
- Bắt buộc TLS khi `MQTT_TLS_REQUIRED=true`.
- Dùng trust store mặc định của Java để xác minh chuỗi chứng chỉ.
- Bật hostname verification.
- Bắt buộc username/password trong cấu hình cloud.
- Subscriber và command publisher dùng chung factory cấu hình TLS.
- Render Blueprint có đủ biến TLS/credential; secret dùng `sync: false`.
- File mẫu local: `.env.mqtt-cloud.example`.

## Thay đổi firmware

- MQTT transport chuyển từ `WiFiClient` sang `WiFiSSLClient`.
- Port chuyển từ `1883` sang `8883`.
- Host, username và password nằm trong `arduino_secrets.h` bị Git ignore.
- MQTT CONNECT gửi username/password và giữ Last Will `OFFLINE` retained.
- Topic telemetry, command, ACK và status không đổi.
- Compile UNO R4 WiFi PASS: 115884/262144 byte flash (44%), 9100/32768 byte RAM (27%).
- Firmware đã upload thành công lên Arduino UNO R4 WiFi tại COM3.

## Biến môi trường backend cloud

```text
MQTT_ENABLED=true
MQTT_BROKER_URL=ssl://<cluster-host>:8883
MQTT_USERNAME=<backend-username>
MQTT_PASSWORD=<backend-password>
MQTT_TLS_REQUIRED=true
MQTT_HOSTNAME_VERIFICATION_ENABLED=true
MQTT_CREDENTIALS_REQUIRED=true
MQTT_BACKEND_CLIENT_ID=shrimp-iot-backend-cloud
MQTT_QOS=1
```

## Kết quả kiểm thử

- Kiểm tra TCP/TLS tới cluster PASS.
- ACL verifier PASS: TLS, device -> backend, backend -> device và chặn subscribe
  chéo namespace thiết bị.
- Maven full test PASS, gồm 3 test riêng cho MQTT TLS options.
- Chip thật báo `MQTT: CONNECTED`, provisioning `ONLINE` và publish telemetry PASS.
- Backend nhận telemetry chip thật và lưu vào Supabase liên tục.
- Readiness `GET /api/health/ready` trả HTTP 200, database `UP`.
- Lệnh an toàn relay 1 `OFF` đi backend -> broker -> chip và quay về ACK:
  `Relay 1 turned OFF`.
- Khi kết nối chip gián đoạn, backend nhận Last Will `OFFLINE`; khi nối lại nhận
  retained/status `ONLINE`.
- Backend chạy với `AI_ENABLED=false`, nên AI local không cản luồng MQTT chính.

## Kết luận

Bước 8 hoàn thành. Luồng MQTT không còn phụ thuộc Mosquitto trong máy cá nhân.
Bước tiếp theo là bước 9: deploy backend (và AI nếu cần) lên Render.
