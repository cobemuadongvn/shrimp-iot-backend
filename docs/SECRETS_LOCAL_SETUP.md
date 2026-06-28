# Secrets And Local Setup

## Nguyên tắc

- Giá trị thật chỉ nằm trong file local bị Git ignore hoặc secret manager của cloud.
- File `*.example.*` chỉ chứa placeholder.
- Không gửi `.env.local`, `.env.device.local`, `arduino_secrets.h` hoặc ZIP backup vào chat công khai hay repository.
- Trước production phải đổi toàn bộ khóa đang dùng cho môi trường demo.

## Arduino UNO R4 WiFi

Thư mục firmware có hai file cấu hình:

- `arduino_secrets.example.h`: file mẫu được commit.
- `arduino_secrets.h`: giá trị thật trên từng máy, không được commit.

Khi clone project sang máy mới:

1. Sao chép `arduino_secrets.example.h` thành `arduino_secrets.h`.
2. Điền SSID, Wi-Fi password, IoT API key và setup AP password.
3. Không đổi tên các macro trong file mẫu.
4. Compile và nạp firmware.

Việc tách file chỉ ngăn lộ secret qua Git. Ở bước 4, firmware sẽ được bổ sung provisioning để đổi Wi-Fi mà không phải nạp lại chương trình.

## Backend local

Backend đọc hai file tùy chọn:

- `.env.local`: cấu hình dịch vụ cá nhân, ví dụ OpenAI.
- `.env.device.local`: IoT API key và mật khẩu demo seed local.

Cả hai file đều bị Git ignore. Môi trường production không dùng các file này; Render sẽ truyền giá trị qua Environment Variables.

Các biến quan trọng:

| Biến | Mục đích |
|---|---|
| `IOT_API_KEY` | Xác thực HTTP fallback từ thiết bị |
| `DB_PASSWORD` | Mật khẩu PostgreSQL/Supabase |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | Xác thực MQTT |
| `OPENAI_API_KEY` | OpenAI assistant, nếu bật |
| `SEED_DEMO_DATA_ENABLED` | Cho phép tạo dữ liệu demo |
| `SEED_ADMIN_PASSWORD` | Mật khẩu admin demo khi seed bật |
| `SEED_USER_PASSWORD` | Mật khẩu user demo khi seed bật |
| `SEED_TECH_PASSWORD` | Mật khẩu technician demo khi seed bật |

Production phải đặt `SEED_DEMO_DATA_ENABLED=false`.

`SECRET_SETUP_AP_PASSWORD` là mật khẩu WPA2 của mạng setup do chip phát. Mỗi chip phải có một giá trị riêng dài 12-63 ký tự; không đưa giá trị thật vào QR mẫu hoặc docs tracked.

## Rotation trước cloud

Trước khi mở hệ thống ra Internet:

1. Tạo IoT API key ngẫu nhiên mới.
2. Tạo MQTT credential riêng cho backend và từng device.
3. Đổi mật khẩu demo hoặc không tạo tài khoản demo.
4. Đổi Wi-Fi password nếu mật khẩu cũ từng bị chia sẻ ngoài nhóm.
5. Cập nhật backend và chip trong cùng một đợt để tránh mất kết nối.

## Backup

ZIP backup đầy đủ có thể chứa file local secrets. Chỉ giữ trong ổ riêng an toàn; không upload vào Git hoặc dịch vụ chia sẻ công khai.
