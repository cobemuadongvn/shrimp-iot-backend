# Cloud Migration Worklog

## 2026-06-27 - Step 1 completed

- Xác định ổ C gần hết dung lượng và gây lỗi I/O `0xC000007F`.
- Dọn Gradle cache, VS Code cache và Temp cũ.
- Không xóa source, Docker database hoặc tài liệu cá nhân.
- Xác nhận backend, AI, PostgreSQL và MQTT vẫn hoạt động.

## 2026-06-27 - Step 2 completed

- Tách Wi-Fi password và IoT API key khỏi firmware tracked.
- Tạo file Arduino secret local và file mẫu.
- Xoay IoT API key local mà không in key.
- Demo seed production mặc định tắt; mật khẩu demo chuyển sang environment.
- Redact secret cũ khỏi docs, test và Postman.
- Mosquitto runtime database ngừng Git tracking nhưng vẫn được giữ local.
- Tạo docs secrets, roadmap và handoff app draft.
- Secret scan tracked files đạt 0 kết quả.

## 2026-06-27 - Step 3 completed

- Đối chiếu API device hiện có: register, link, latest-state có thể tái sử dụng sau claim.
- Chọn namespace mới `/api/device-provisioning` cho claim/status để không phá API cũ.
- Freeze QR URI format và field validation cho provisioning v1.
- Freeze local chip API, backend cloud API, state machine, error codes và retry policy.
- Tạo payload mẫu không chứa credential thật.
- Tạo QR PNG 570x570 và decode ngược thành công, payload khớp tuyệt đối.
- QR SHA-256: `e53f71be8b66dc728eb25e0792ce9b5c7aee7e299b74ee60966665860d7155be`.

## Quyết định kiến trúc đã chốt

- Wi-Fi password đi trực tiếp từ app sang chip, không qua backend.
- App không kết nối trực tiếp Supabase hoặc MQTT.
- QR claim code dùng một lần và backend chỉ lưu hash.
- QR mẫu trong docs không hoạt động với backend thật.
- Firmware không nhận MQTT/backend host từ app.

## Bước tiếp theo

- Bước 7: Flyway và chuyển PostgreSQL sang Supabase.

## 2026-06-27 - Step 4 firmware implementation completed

- Thêm module `ProvisioningManager` tách khỏi luồng cảm biến/relay hiện có.
- Thêm setup Access Point WPA2 và local API đúng Contract v1.
- Thêm trang test local tại `http://192.168.4.1`.
- Lưu Wi-Fi vào EEPROM chỉ sau khi kết nối mới thành công.
- Giữ cấu hình cũ nếu người dùng nhập Wi-Fi sai.
- D7 nối GND, giữ 5 giây lúc boot để xóa Wi-Fi đã lưu và vào setup mode.
- Thêm serial command `wifi setup` và `wifi reset`.
- Relay bị ép OFF và lệnh relay ON bị chặn trong provisioning mode.
- Setup AP tự timeout sau 10 phút; firmware cooldown 60 giây trước khi thử lại.
- Tạo Postman collection và hướng dẫn test hardware/local API.
- Compile PASS cho Arduino UNO R4 WiFi bằng core `arduino:renesas_uno` 1.5.3.
- Flash: 112868/262144 byte (43%).
- RAM global: 9084/32768 byte (27%).
- Đã upload thành công vào Arduino UNO R4 WiFi trên COM3.
- Hardware/API core PASS: AP WPA2, relay fail-safe, status/network API và các mã lỗi 401/400/422.
- Cấp Wi-Fi hợp lệ trả 202; chip lưu EEPROM, trở lại MQTT ONLINE và publish telemetry.
- Reset chip PASS: credential EEPROM, MQTT, telemetry và trạng thái bốn relay OFF được khôi phục.
- Bước 4 hoàn thành; bước 5 chưa bắt đầu.

## 2026-06-27 - Step 5 backend device provisioning completed

- Thêm bảng/entity `device_provisioning` liên kết 1-1 với device hiện có.
- Claim code ngẫu nhiên 256-bit, database chỉ lưu SHA-256 và xóa hash sau claim thành công.
- Dùng pessimistic write lock khi phát/claim code để tránh hai request claim đồng thời.
- Thêm `POST /api/device-provisioning/claims` và `GET /api/device-provisioning/devices/{deviceId}/status` đúng Contract v1.
- Thêm endpoint ADMIN phát claim code một lần: `POST /api/device-provisioning/devices/{deviceId}/claim-code`.
- Áp dụng quyền claim: ADMIN mọi ao; TECHNICIAN cần OWNER/READ_WRITE/CONTROL; USER cần OWNER.
- Claim lặp bởi cùng tài khoản và cùng ao là idempotent; chủ/ao khác trả 409.
- Error response có mã ổn định tại `data.code`; không log claim code.
- Timestamp `lastSeenAt` trả ISO-8601 với offset `+07:00`.
- Thêm audit log cho phát claim code và claim device, không chứa secret.
- Thêm Postman và tài liệu bàn giao app/backend.
- Maven offline unit test `DeviceProvisioningServiceTest`: 5/5 PASS.
- HTTP integration test PASS trên random port/H2: issue code, invalid code, claim, idempotent retry, status, owner conflict và unauthorized.
- Full Spring Boot context test PASS sau khi bổ sung Jackson 2 compatibility bean cho các MQTT/AI service cũ trên Spring Boot 4.
- Đã package và restart backend local bằng bản mới; `/api/health` UP, endpoint provisioning thiếu token trả đúng 401.
- Hibernate local đã tạo bảng `device_provisioning`, unique constraint theo device và ba foreign key tới device/user/pond.
- Bước 5 hoàn thành; bước 6 chưa bắt đầu.

## 2026-06-27 - Step 6 Docker, Render Blueprint, CI and health completed

- Thêm multi-stage `Dockerfile` Java 21; runtime non-root và container health check.
- Thêm `ai-service/Dockerfile` Python 3.12; runtime non-root, model health check và Uvicorn đọc `PORT`.
- Thêm `.dockerignore` cho backend/AI để loại secret, cache và artifact không cần thiết.
- Backend hỗ trợ `PORT` của Render, vẫn giữ `SERVER_PORT` fallback cho local.
- Thêm `/api/health/live` và `/api/health/ready`; readiness kiểm tra PostgreSQL bằng `SELECT 1`.
- Thêm `render.yaml` gồm backend Starter và AI Free tại Singapore.
- Backend dùng Starter vì phải giữ MQTT subscriber 24/7; AI mặc định `AI_ENABLED=false` để tránh cold-start ảnh hưởng luồng chính.
- Tất cả secret Blueprint dùng `sync: false`; `JPA_DDL_AUTO=validate`, seed và auto-control mặc định tắt.
- Thêm GitHub Actions: Maven test, AI model test, Blueprint parse và Docker Buildx cho hai image.
- Thêm `DeploymentConfigTest`: 3/3 PASS, kiểm tra Blueprint, secret placeholders, Dockerfile, CI và Render PORT.
- Maven full test PASS; AI health/model load PASS.
- Đã package/restart backend local; `/api/health/live` và `/api/health/ready` đều UP, database UP và MQTT telemetry tiếp tục vào backend.
- Docker daemon local không phản hồi và ổ C còn khoảng 7.43 GB; không restart Docker Desktop để tránh gián đoạn PostgreSQL/MQTT. Image build được giao cho CI khi push.
- Chưa push Git, chưa tạo Render service và chưa phát sinh chi phí; deploy thật thuộc bước 9.
- Bước 6 hoàn thành; bước 7 chưa bắt đầu.

## 2026-06-27 - Step 7 local database preparation completed

- Tạo snapshot nhất quán của PostgreSQL local: 23 bảng, 256 cột, 63 constraint, 46 index và 22 sequence.
- Xuất dữ liệu từng bảng thành CSV và ghi SHA-256; toàn bộ `backups/` bị Git ignore.
- Thêm `spring-boot-starter-flyway` và `flyway-database-postgresql` phù hợp Spring Boot 4.
- Tạo `V1__baseline_schema.sql` từ schema PostgreSQL thật, không dựa vào phỏng đoán.
- Chạy V1 trong schema tạm và xác nhận khớp 23 bảng/256 cột/63 constraint/46 index; rollback schema tạm sau kiểm tra.
- Baseline database local ở Flyway version 1, không thay đổi dữ liệu ứng dụng.
- Chuyển mặc định Hibernate sang `ddl-auto=validate`; Flyway baseline-on-migrate mặc định false.
- Thêm cấu hình SSL mode, Hikari pool nhỏ và các environment placeholder cho Render/Supabase.
- Full Maven test PASS: 11 test, gồm kiểm tra cấu hình/migration database; startup smoke test PostgreSQL thật PASS.
- Restart backend local bằng artifact mới; Flyway báo schema up to date, readiness database UP và MQTT device_01 ONLINE.
- Bước 7 đang chờ người dùng đăng nhập và tạo Supabase project; chưa gửi dữ liệu ra cloud.

## 2026-06-28 - Step 7 Supabase migration completed

- Tạo Supabase project `shrimp-iot` tại Southeast Asia (Singapore), gói Free.
- Dùng Shared Session Pooler IPv4 cổng 5432 với SSL; credential chỉ nằm trong `.env.supabase.local` bị Git ignore.
- Flyway V1 chạy thành công trên PostgreSQL 17.6: 23 bảng, 256 cột, 63 constraint và 46 index.
- Dừng backend ngắn hạn, tạo snapshot cuối `shrimp_iot-20260627-182555` và kiểm tra SHA-256.
- Import 23 bảng/15.825 bản ghi trong một transaction, theo thứ tự foreign key.
- Lần import đầu rollback an toàn khi tool gặp bảng không có cột `id`; sửa tool chỉ reset bảng có identity rồi import lại PASS.
- Đồng bộ identity sequence và đối chiếu row count từng bảng sau import.
- Cloud verification snapshot ngày 2026-06-28 có 15.849 bản ghi, nhiều hơn snapshot local 24 bản ghi và không có regression.
- Thêm `tools/start-backend-supabase.ps1` để chạy backend local bằng cấu hình Supabase mà không lộ secret.
- Backend PID 3788: Flyway schema up to date, `/api/health/ready` database UP.
- PostgreSQL local và snapshot vẫn được giữ để rollback; không xóa dữ liệu local.
- Bước 7 hoàn thành; bước 8 tiếp theo là managed MQTT public TLS và credential/ACL cho thiết bị.

## 2026-06-28 - Step 8 MQTT TLS implementation started

- Chọn HiveMQ Cloud Serverless Free cho giai đoạn hiện tại: TLS, 100 connections, 10 GB/tháng, không cần thẻ.
- Thiết kế hai permission: backend `shrimp-iot/devices/#` và chip `shrimp-iot/devices/device_01/#`.
- Thêm MQTT TLS/hostname verification/credential validation dùng chung cho backend inbound và outbound.
- Thêm biến môi trường TLS vào application, `.env.example` và Render Blueprint.
- Firmware chuyển sang `WiFiSSLClient`, port 8883 và credential trong secret header bị Git ignore.
- Maven full test PASS, gồm 3 test riêng cho MQTT TLS options.
- Firmware UNO R4 WiFi compile PASS: flash 44%, RAM 27%.
- Chưa upload firmware và chưa đổi backend đang chạy; chờ cluster host/credential thật để tránh gián đoạn.

## 2026-06-28 - Step 8 HiveMQ Cloud TLS completed

- Tạo HiveMQ Cloud Serverless cluster và hai permission giới hạn topic cho backend/device_01.
- Tạo credential riêng cho backend và chip; mật khẩu chỉ lưu trong file local bị Git ignore.
- ACL verifier PASS: TLS, hai chiều publish/subscribe và từ chối truy cập namespace thiết bị khác.
- Chuyển backend local sang HiveMQ TLS đồng thời tiếp tục dùng Supabase; readiness HTTP 200, database UP.
- Upload firmware TLS lên Arduino UNO R4 WiFi tại COM3 thành công.
- Chip thật báo MQTT CONNECTED, provisioning ONLINE và publish telemetry định kỳ qua TLS/8883.
- Backend nhận telemetry thật, xử lý và lưu liên tục vào Supabase.
- Last Will/status được xác minh: backend nhận OFFLINE khi kết nối gián đoạn và ONLINE khi chip nối lại.
- Gửi lệnh an toàn relay 1 OFF qua backend; chip thực thi và command chuyển từ SENT sang ACK với thông báo `Relay 1 turned OFF`.
- Tắt AI local bằng `AI_ENABLED=false`; luồng MQTT chính không còn chờ AI local.
- Bước 8 hoàn thành; bước 9 tiếp theo là deploy backend và AI (nếu bật) lên Render.
