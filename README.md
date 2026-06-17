# Shrimp IoT Backend - Spring Boot + PostgreSQL

Backend cho đề tài:

**IOT – Hệ thống giám sát môi trường ao nuôi thủy hải sản**

## Kiến trúc

```text
Arduino UNO R4 WiFi
→ HTTP POST JSON
→ Spring Boot REST API
→ PostgreSQL Docker
→ ReactJS Dashboard
```

## Công nghệ

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL
- Docker Compose
- ReactJS frontend gọi API

---

## 1. Chạy PostgreSQL bằng Docker

Trong thư mục project, chạy:

```bash
docker compose up -d
```

Kiểm tra container:

```bash
docker ps
```

Thông tin PostgreSQL mặc định:

```text
Host: localhost
Port: 5432
Database: shrimp_iot
User: shrimp_user
Password: 123456
```

PgAdmin:

```text
URL: http://localhost:5050
Email: admin@shrimp-iot.local
Password: 123456
```

Khi vào PgAdmin, thêm server:

```text
Host name/address: postgres
Port: 5432
Maintenance database: shrimp_iot
Username: shrimp_user
Password: 123456
```

Nếu dùng công cụ ngoài Docker như DBeaver/TablePlus, kết nối bằng:

```text
Host: localhost
Port: 5432
Database: shrimp_iot
Username: shrimp_user
Password: 123456
```

---

## 2. Chạy backend

```bash
mvn spring-boot:run
```

Hoặc Windows:

```bash
mvnw.cmd spring-boot:run
```

Backend mặc định chạy ở:

```text
http://localhost:8080
```

Health check:

```bash
curl http://localhost:8080/api/health
```

---

## 3. API Arduino gửi dữ liệu

```http
POST /api/readings
Content-Type: application/json
X-API-Key: MY_SECRET_KEY
```

Body:

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

Test bằng curl:

```bash
curl -X POST http://localhost:8080/api/readings \
  -H "Content-Type: application/json" \
  -H "X-API-Key: MY_SECRET_KEY" \
  -d '{
    "deviceId": "device_01",
    "temperature": 28.5,
    "ph": 7.2,
    "ecValue": 1.8,
    "salinity": 12.8,
    "doValue": 5.6
  }'
```

---

## 4. API ReactJS gọi dữ liệu

### Lấy dữ liệu mới nhất

```http
GET /api/readings/latest?deviceId=device_01
```

### Lấy lịch sử dữ liệu

```http
GET /api/readings/history?deviceId=device_01&limit=50
```

### Lấy dữ liệu theo khoảng thời gian

```http
GET /api/readings/range?deviceId=device_01&from=2026-05-16T00:00:00&to=2026-05-16T23:59:59
```

### Dashboard summary

```http
GET /api/dashboard/summary?deviceId=device_01
```

---

## 5. API điều khiển relay, phần mở rộng

### Tạo lệnh điều khiển

```http
POST /api/commands
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

### Arduino lấy lệnh đang chờ

```http
GET /api/commands/pending?deviceId=device_01
X-API-Key: MY_SECRET_KEY
```

### Arduino xác nhận đã thực hiện lệnh

```http
POST /api/commands/{id}/ack
X-API-Key: MY_SECRET_KEY
Content-Type: application/json
```

Body:

```json
{
  "success": true,
  "message": "Relay updated"
}
```

---

## 6. Kiểm tra dữ liệu trong PostgreSQL

Vào container:

```bash
docker exec -it shrimp-postgres psql -U shrimp_user -d shrimp_iot
```

Xem bảng:

```sql
\dt
```

Xem dữ liệu cảm biến:

```sql
SELECT * FROM sensor_readings ORDER BY created_at DESC;
```

---

## 7. Cấu hình Arduino

Trong code Arduino, nếu backend chạy local qua ngrok hoặc server online, chỉnh:

```cpp
const char SERVER_HOST[] = "your-domain.com";
const int SERVER_PORT = 80;
const char API_PATH[] = "/api/readings";
const char API_KEY[] = "MY_SECRET_KEY";
```

Nếu gọi HTTPS thì dùng `WiFiSSLClient` và port `443`.

## Bản complete đã bổ sung

- Phân quyền: ADMIN / USER / TECHNICIAN
- Bảng `alerts` để lưu lịch sử cảnh báo
- Bảng `notification_logs` để mô phỏng thông báo APP/SMS/EMAIL
- Backend tự động tạo lệnh relay khi vượt ngưỡng:
  - DO thấp -> bật relay máy sục oxy
  - Nhiệt độ cao -> bật relay quạt nước
- Arduino cần dùng file `shrimp_iot_uno_r4_complete.ino` đi kèm để lấy lệnh và ACK.

## Bổ sung mới: đăng ký chờ duyệt + chatbot

Xem chi tiết trong file `README_REGISTER_CHATBOT.md`.

### API đăng ký

```http
POST /api/auth/register
```

### Admin duyệt user

```http
GET /api/users/pending
POST /api/users/{id}/approve
POST /api/users/{id}/reject
```

### Chatbot

```http
POST /api/chat/message
GET /api/chat/sessions
GET /api/chat/sessions/{sessionId}/messages
```

Chatbot hiện có Pha 1 hỏi đáp kiến thức cơ bản và Pha 2 đọc dữ liệu hệ thống.
