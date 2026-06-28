# Step 6 - Docker, Render Blueprint, CI và Health Check

## Trạng thái

- Backend Dockerfile: hoàn thành.
- AI Dockerfile: hoàn thành.
- Render Blueprint: hoàn thành.
- GitHub Actions CI: hoàn thành.
- Maven test và HTTP integration test: PASS.
- AI import/model health: PASS.
- Deployment config test: 3/3 PASS.
- Deploy thật: chưa thực hiện; thuộc bước 9.

## File chính

- `Dockerfile`: build Spring Boot bằng Java 21, runtime non-root.
- `ai-service/Dockerfile`: Python 3.12, load model và chạy Uvicorn non-root.
- `render.yaml`: hai Render Web Service tại Singapore.
- `.github/workflows/ci.yml`: test Java, test AI, parse Blueprint và build hai Docker image.
- `.dockerignore` và `ai-service/.dockerignore`: không đưa secret/cache/tài liệu thừa vào image.

## Kiến trúc Render đã chốt

### Backend

- Service: `shrimp-iot-backend`.
- Plan: `starter`.
- Health: `/api/health/ready`.
- Auto deploy: chỉ sau khi CI checks pass.
- Phải chạy liên tục để giữ MQTT subscription, xử lý telemetry/ACK và trạng thái online/offline.

Không đổi backend xuống Free nếu mục tiêu là hệ thống IoT 24/7. Render Free có thể spin down khi không có inbound HTTP trong 15 phút; lưu lượng MQTT từ chip không phải request HTTP đánh thức service.

### AI

- Service: `shrimp-iot-ai`.
- Plan: `free` trong giai đoạn đầu.
- Health: `/health`.
- Backend để `AI_ENABLED=false` cho đến khi AI URL được tạo ở bước 9.

AI Free có cold start. Khi cần dự đoán tức thời ổn định, chuyển AI sang Starter hoặc tăng timeout có kiểm soát.

## Health check

- `/api/health`: tương thích cũ, liveness.
- `/api/health/live`: JVM/web process đang chạy.
- `/api/health/ready`: chạy `SELECT 1`; trả 200 khi PostgreSQL sẵn sàng, 503 khi database lỗi.
- AI `/health`: trả 200 và trạng thái các model.

Render dùng `/api/health/ready` để không đưa backend chưa kết nối database vào nhận traffic. MQTT và AI là dependency ngoài nên không làm readiness DOWN; nếu không, một broker/AI lỗi tạm thời sẽ tạo vòng restart backend.

## Biến môi trường phải nhập trong Render Dashboard

| Biến | Nguồn ở bước sau |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Supabase - bước 7 |
| `MQTT_BROKER_URL`, `MQTT_USERNAME`, `MQTT_PASSWORD` | MQTT TLS cloud - bước 8 |
| `IOT_API_KEY` | Secret thiết bị/backend |
| `CORS_ALLOWED_ORIGINS` | URL web/app - bước 10 |
| `AI_SERVICE_URL` | URL `https://.../predict` của AI - bước 9 |
| `OPENAI_API_KEY` | Tùy chọn, chỉ khi bật chat LLM |

Các biến trên dùng `sync: false`; không ghi giá trị thật vào `render.yaml`.

## Giá trị production đã khóa

- `JPA_DDL_AUTO=validate`: chỉ chạy sau khi Flyway bước 7 tạo schema.
- `SEED_DEMO_DATA_ENABLED=false`.
- `AUTO_CONTROL_ENABLED=false` khi deploy lần đầu.
- `AI_ENABLED=false` cho đến khi health AI và URL public đã PASS.
- Backend đọc cổng từ `PORT` do Render cấp; local vẫn dùng `SERVER_PORT`/8080.

## CI

Mỗi push/PR chạy:

1. Maven test toàn backend.
2. Cài dependency AI, compile Python và load tất cả model.
3. Parse `render.yaml`.
4. Build backend image và AI image bằng Docker Buildx, không push registry.

Blueprint đặt `autoDeployTrigger: checksPass`, nên Render không auto deploy commit có CI thất bại.

## Kiểm tra local

```powershell
mvn test
docker build -t shrimp-iot-backend:local .
docker build -t shrimp-iot-ai:local -f ai-service/Dockerfile ai-service
```

Docker daemon trên máy hiện không phản hồi, nên không restart Docker Desktop để tránh gián đoạn PostgreSQL/MQTT đang chạy. Hai image sẽ được Buildx kiểm tra trong CI sau khi push Git. Bước 6 không tự push và không tạo tài nguyên tính phí.

Trước bước 9, dùng Render CLI chính thức để validate lần cuối:

```text
render blueprints validate render.yaml
```

Tham khảo chính thức:

- https://render.com/docs/blueprint-spec
- https://render.com/docs/web-services
- https://render.com/docs/health-checks
- https://render.com/docs/configure-environment-variables
