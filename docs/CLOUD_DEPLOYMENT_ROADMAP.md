# Shrimp IoT Cloud Deployment Roadmap

Tài liệu này là checklist chính để đưa hệ thống từ máy local lên cloud. Chỉ chuyển sang bước tiếp theo sau khi bước hiện tại đã được kiểm tra.

## Trạng thái tổng quan

| Bước | Nội dung | Trạng thái |
|---|---|---|
| 1 | Giải phóng dung lượng ổ C | Hoàn thành |
| 2 | Tách secrets khỏi source | Hoàn thành |
| 3 | Chốt đặc tả QR và provisioning API | Hoàn thành |
| 4 | Firmware chip tự phát Wi-Fi và nhận cấu hình | Hoàn thành - hardware/API core PASS |
| 5 | Backend claim device, provisioning status, docs và Postman | Hoàn thành - unit + HTTP integration PASS |
| 6 | Dockerfile, render.yaml, CI và health check | Hoàn thành cấu hình/test; Docker build chạy khi push CI |
| 7 | Flyway và chuyển PostgreSQL sang Supabase | Hoàn thành - 23 bảng/15.825 bản ghi được chuyển, backend DB UP |
| 8 | MQTT public TLS, tài khoản và ACL từng chip | Hoàn thành - HiveMQ TLS, chip thật, telemetry và command ACK PASS |
| 9 | Deploy backend và AI lên Render | Chưa làm |
| 10 | Chuyển app/web và firmware sang URL cloud | Chưa làm |
| 11 | Kiểm thử 24 giờ khi máy cá nhân tắt | Chưa làm |

## Điều kiện hoàn thành bước 2

- Firmware `.ino` không chứa trực tiếp SSID, Wi-Fi password hoặc IoT API key.
- File `arduino_secrets.h` và các file `.env.*.local` bị Git ignore.
- File mẫu chỉ chứa placeholder.
- Demo seed mặc định tắt.
- Khi bật demo seed, mật khẩu phải được truyền qua environment.
- Không đưa ZIP backup hoặc dữ liệu Mosquitto runtime lên Git.
- Backend, AI, PostgreSQL và MQTT local vẫn hoạt động sau khi restart.
- Quét tracked files và lịch sử Git không phát hiện secret thật chưa được xử lý.

## Kiến trúc đích

```text
App/Web -- HTTPS/WSS --> Backend Render --> Supabase PostgreSQL
                               |
Chip -------- MQTT TLS --------+--> Managed MQTT Broker
```

App/web không kết nối trực tiếp database và không giữ MQTT credential của chip.
