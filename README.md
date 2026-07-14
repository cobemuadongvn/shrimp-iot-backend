# Shrimp IoT Backend

Backend Spring Boot cho hệ thống giám sát và điều khiển môi trường ao nuôi tôm. Dự án nhận dữ liệu cảm biến từ thiết bị IoT, lưu PostgreSQL, phát sự kiện realtime cho dashboard, cảnh báo khi vượt ngưỡng và hỗ trợ điều khiển relay qua MQTT hoặc HTTP fallback.

## Tổng quan

```text
Arduino UNO R4 WiFi
  -> MQTT Broker / HTTP fallback
  -> Spring Boot REST API
  -> PostgreSQL + Flyway
  -> Web/App Dashboard + WebSocket
  -> AI service tùy chọn
```

Các nhóm chức năng chính:

- Thu thập chỉ số môi trường: nhiệt độ, pH, EC, độ mặn, DO.
- Quản lý ao, thiết bị, cảm biến, relay và quyền truy cập theo vai trò.
- Xác thực tài khoản bằng token `Bearer`, phê duyệt người dùng mới.
- Cảnh báo, thông báo in-app, lịch sử lệnh điều khiển và báo cáo CSV.
- Điều khiển relay thủ công hoặc theo kịch bản; hỗ trợ MQTT command retry.
- Phát trạng thái mới qua WebSocket/STOMP cho dashboard realtime.
- Tích hợp AI service và chatbot tùy chọn.

## Công nghệ

- Java 21
- Spring Boot 4
- Spring Web, Spring Data JPA, Validation
- PostgreSQL 16, Flyway
- Docker Compose
- MQTT với Eclipse Mosquitto và Spring Integration MQTT
- WebSocket/STOMP
- Python AI service và Streamlit dashboard tùy chọn

## Cấu trúc thư mục

```text
src/main/java/com/example/shrimpiot  Spring Boot source code
src/main/resources                  application.yml, Flyway migrations
ai-service                          FastAPI AI prediction service
streamlit-dashboard                 Dashboard demo bằng Streamlit
arduino                             Sketch Arduino/device
docs                                Tài liệu handoff và ghi chú kỹ thuật
mosquitto                           Cấu hình MQTT broker local
postman                             Collection/test request nếu có
```

## Yêu cầu môi trường

- JDK 21
- Maven 3.9+
- Docker Desktop hoặc Docker Engine có Compose
- Git
- Python 3.10+ nếu chạy `ai-service` hoặc `streamlit-dashboard`

Kiểm tra nhanh:

```bash
java -version
mvn -version
docker compose version
```

## Cấu hình local

Ứng dụng đọc cấu hình từ biến môi trường và tự import `.env.local` nếu file tồn tại.

Tạo file cấu hình local:

```powershell
Copy-Item .env.example .env.local
```

Trên Linux/macOS:

```bash
cp .env.example .env.local
```

Các biến quan trọng cần kiểm tra trong `.env.local`:

```properties
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shrimp_iot
DB_USER=shrimp_user
DB_PASSWORD=change_me_dev_password
IOT_API_KEY=replace_with_a_long_random_device_api_key
MQTT_ENABLED=true
MQTT_BROKER_URL=tcp://127.0.0.1:1883
```

Không commit `.env.local` hoặc bất kỳ file nào chứa mật khẩu/API key thật.

## Chạy local

Khởi động PostgreSQL, pgAdmin và Mosquitto:

```bash
docker compose up -d
```

Chạy backend:

```bash
mvn spring-boot:run
```

Backend mặc định chạy tại:

```text
http://localhost:8080
```

Health check:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/ready
```

pgAdmin local:

```text
URL: http://localhost:5050
Email: giá trị PGADMIN_DEFAULT_EMAIL trong .env.local
Password: giá trị PGADMIN_DEFAULT_PASSWORD trong .env.local
```

Khi thêm server PostgreSQL trong pgAdmin:

```text
Host name/address: postgres
Port: 5432
Maintenance database: shrimp_iot
Username: shrimp_user
Password: giá trị DB_PASSWORD trong .env.local
```

Nếu dùng DBeaver/TablePlus từ máy host:

```text
Host: localhost
Port: 5432
Database: shrimp_iot
Username: shrimp_user
Password: giá trị DB_PASSWORD trong .env.local
```

## Seed dữ liệu demo

Mặc định repo không tự tạo tài khoản demo để tránh lộ mật khẩu. Nếu cần dữ liệu mẫu khi phát triển local, bật trong `.env.local`:

```properties
SEED_DEMO_DATA_ENABLED=true
SEED_ADMIN_PASSWORD=your_admin_password
SEED_USER_PASSWORD=your_user_password
SEED_TECH_PASSWORD=your_technician_password
```

Sau đó restart backend. Các username demo được tạo theo cấu hình `DataInitializer`: `admin`, `user`, `tech`.

## Xác thực

Thiết bị IoT dùng header `X-API-Key` với giá trị `IOT_API_KEY`.

Web/App dùng token từ API đăng nhập:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "your_admin_password"
}
```

Các API dành cho Web/App cần gửi:

```http
Authorization: Bearer <token>
```

Đăng ký tài khoản mới:

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "pond01",
  "password": "strong_password",
  "fullName": "Chủ ao 01",
  "phone": "0987654321",
  "email": "pond01@example.com"
}
```

Admin duyệt hoặc từ chối người dùng:

```http
GET /api/users/pending
POST /api/users/{id}/approve
POST /api/users/{id}/reject
```

## API thiết bị

Gửi dữ liệu cảm biến qua HTTP fallback:

```http
POST /api/readings
Content-Type: application/json
X-API-Key: <IOT_API_KEY>
```

```json
{
  "deviceId": "device_01",
  "temperature": 28.5,
  "ph": 7.2,
  "ecValue": 1.8,
  "salinity": 12.8,
  "doValue": 5.6
}
```

Thiết bị lấy lệnh đang chờ:

```http
GET /api/commands/pending?deviceId=device_01
X-API-Key: <IOT_API_KEY>
```

Thiết bị xác nhận lệnh:

```http
POST /api/commands/{id}/ack
Content-Type: application/json
X-API-Key: <IOT_API_KEY>
```

```json
{
  "success": true,
  "message": "Relay updated"
}
```

## API Web/App chính

Các endpoint sau cần `Authorization: Bearer <token>`:

```http
GET    /api/readings/latest?deviceId=device_01
GET    /api/readings/history?deviceId=device_01&limit=50
GET    /api/readings/range?deviceId=device_01&from=2026-05-16T00:00:00&to=2026-05-16T23:59:59
GET    /api/dashboard/summary?deviceId=device_01

POST   /api/commands
GET    /api/commands/history?deviceId=device_01

GET    /api/alerts/open?deviceId=device_01
GET    /api/alerts/history?deviceId=device_01
POST   /api/alerts/{id}/resolve

GET    /api/devices
GET    /api/devices/{deviceId}
GET    /api/devices/{deviceId}/latest-state

GET    /api/notifications/in-app
PATCH  /api/notifications/in-app/{id}/read
PATCH  /api/notifications/in-app/read-all

POST   /api/chat/message
GET    /api/chat/sessions
GET    /api/chat/sessions/{sessionId}/messages

GET    /api/reports/summary
GET    /api/reports/sensors.csv
GET    /api/reports/alerts.csv
GET    /api/reports/commands.csv
```

Tạo lệnh relay:

```http
POST /api/commands
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "deviceId": "device_01",
  "relayNo": 1,
  "action": "ON"
}
```

## MQTT

MQTT là luồng chính cho giao tiếp thiết bị khi chạy local hoặc production.

Broker local từ Docker Compose:

```text
tcp://127.0.0.1:1883
```

Topic mặc định:

```text
Telemetry: shrimp-iot/devices/{deviceId}/telemetry
Command:   shrimp-iot/devices/{deviceId}/commands
ACK:       shrimp-iot/devices/{deviceId}/commands/ack
Status:    shrimp-iot/devices/{deviceId}/status
```

Backend subscribe:

```text
shrimp-iot/devices/+/telemetry
shrimp-iot/devices/+/commands/ack
shrimp-iot/devices/+/status
```

Payload telemetry:

```json
{
  "deviceId": "device_01",
  "temperature": 28.5,
  "ph": 7.4,
  "ecValue": 1.2,
  "salinity": 12.5,
  "doValue": 5.8
}
```

Xem thêm: `README_MQTT_MIGRATION.md`.

## WebSocket realtime

Backend mở STOMP endpoint:

```text
/ws
```

Simple broker prefix:

```text
/topic
```

Topic realtime:

```text
/topic/device/{deviceId}/readings
/topic/device/{deviceId}/relays
/topic/device/{deviceId}/alerts
/topic/device/{deviceId}/notifications
/topic/user/{userId}/notifications
```

## AI service

AI service là thành phần tùy chọn, nằm trong thư mục `ai-service`.

Chạy service:

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8001
```

Backend gọi service qua:

```properties
AI_ENABLED=true
AI_SERVICE_URL=http://127.0.0.1:8001/predict
```

Xem thêm: `ai-service/README_COMBINED_AI.md`.

## Streamlit dashboard

Dashboard demo nằm trong `streamlit-dashboard`.

```powershell
pip install -r streamlit-dashboard/requirements.txt
python -m streamlit run streamlit-dashboard/app.py --server.port 8501 --runner.magicEnabled false
```

Xem thêm: `streamlit-dashboard/README.md`.

## Build, test và package

Chạy test:

```bash
mvn test
```

Build jar:

```bash
mvn clean package
```

Build Docker image:

```bash
docker build -t shrimp-iot-backend .
```

## Deploy

Repo có sẵn `Dockerfile` và `render.yaml` cho Render.

Khi deploy production cần cấu hình tối thiểu:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
DB_SSLMODE
IOT_API_KEY
CORS_ALLOWED_ORIGINS
MQTT_BROKER_URL
MQTT_USERNAME
MQTT_PASSWORD
AI_SERVICE_URL
```

Lưu ý:

- `DB_SSLMODE=require` thường cần cho database cloud.
- MQTT production nên bật TLS và credential riêng cho broker.
- Không bật seed demo data trên production.
- Render Free có thể sleep, không phù hợp nếu cần nhận telemetry 24/7.

## Tài liệu liên quan

- `README_AUTH.md`: phân quyền và luồng tài khoản.
- `README_MQTT_MIGRATION.md`: MQTT topics và migration notes.
- `README_OPERATION_MODE_SAMPLING_SALINITY.md`: chế độ vận hành, sampling, salinity control.
- `README_AI_AUTO_MODE_B_FINAL.md`: chế độ AI auto.
- `docs/APP_DEVICE_PROVISIONING_HANDOFF.md`: bàn giao provisioning thiết bị.
- `docs/TECHNICIAN_DEVICE_MANAGEMENT.md`: quản lý thiết bị cho kỹ thuật viên.
- `docs/BACKEND_ADJUSTMENT_SECURITY_RUNTIME.md`: ghi chú bảo mật/runtime.

## Ghi chú phát triển

- Schema database được quản lý bằng Flyway trong `src/main/resources/db/migration`.
- `spring.jpa.hibernate.ddl-auto` mặc định là `validate`, nên migration phải khớp entity.
- API key, database password, OpenAI key và MQTT credentials chỉ đặt trong `.env.local` hoặc biến môi trường runtime.
- Nếu dùng database cũ chưa có Flyway history, cần xử lý baseline một lần rồi đưa `FLYWAY_BASELINE_ON_MIGRATE=false` trở lại.
