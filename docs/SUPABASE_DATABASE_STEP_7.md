# Bước 7 - Flyway và Supabase PostgreSQL

## Trạng thái hiện tại

Bước 7 đã hoàn tất ngày 2026-06-28. Supabase project tại Singapore đã có schema Flyway,
dữ liệu local đã được chuyển và backend local đang sử dụng Session Pooler qua SSL.

Đã hoàn thành:

- Sao lưu nhất quán database local bằng transaction `REPEATABLE READ READ ONLY`.
- Snapshot chứa 23 bảng, 256 cột, 63 constraint, 46 index và 22 identity sequence.
- Mỗi file dữ liệu CSV có SHA-256 trong `manifest.txt`.
- Thư mục `backups/` bị Git ignore vì chứa token, password hash và dữ liệu người dùng.
- Thêm Spring Boot Flyway starter và module PostgreSQL.
- Tạo `db/migration/V1__baseline_schema.sql` từ schema PostgreSQL thật.
- Chạy thử V1 trong schema tạm: 23 bảng, 256 cột, 63 constraint và 46 index khớp.
- Baseline database local tại version 1; không chạy lại V1 lên 23 bảng đang có.
- Đổi Hibernate từ tự sửa schema (`update`) sang chỉ kiểm tra (`validate`).
- Backend mới khởi động thành công, Flyway báo schema up to date, readiness database UP và MQTT nhận chip ONLINE.
- Tạo Supabase project `shrimp-iot` tại Southeast Asia (Singapore).
- Chạy Flyway V1 trên Supabase PostgreSQL 17.6: 23 bảng, 256 cột, 63 constraint và 46 index.
- Tạo snapshot cuối `shrimp_iot-20260627-182555` rồi import 23 bảng/15.825 bản ghi trong một transaction.
- Reset identity sequence sau import và xác minh row count từng bảng.
- Kiểm tra lại ngày 2026-06-28: Supabase có 15.849 bản ghi, không bảng nào ít hơn snapshot local.
- Tạo cloud snapshot kiểm chứng tại `backups/supabase/postgres-20260628-123911/`.
- Backend PID 3788 khởi động bằng Supabase, Flyway up to date và `/api/health/ready` báo database UP.

## File quan trọng

- Migration: `src/main/resources/db/migration/V1__baseline_schema.sql`
- Cấu hình: `src/main/resources/application.yml`
- Snapshot tool: `tools/database/PostgresSnapshot.java`
- Migration verifier: `tools/database/PostgresMigrationVerifier.java`
- Baseline tool: `tools/database/ExistingDatabaseFlywayBaseline.java`
- Snapshot local hiện tại: `backups/postgres/shrimp_iot-20260627-173959/`

Không gửi thư mục snapshot lên Git, email hoặc chat.

## Việc người dùng đã thực hiện

1. Tạo organization `Shrimp IoT` trên gói Free.
2. Tạo project `shrimp-iot` tại Singapore.
3. Tắt Data API, tự động expose table và automatic RLS vì app/web chỉ gọi backend.
4. Chọn Shared Session Pooler IPv4 cổng 5432.
5. Lưu database credential trong `.env.supabase.local` bị Git ignore.

Codex có thể hỗ trợ thao tác dashboard sau khi người dùng tự hoàn tất đăng nhập, OTP hoặc CAPTCHA.

## Quy trình chuyển dữ liệu đã thực hiện

1. Kiểm tra kết nối Session Pooler và SSL.
2. Chạy V1 trong schema tạm rồi rollback để kiểm tra cú pháp trên PostgreSQL Supabase.
3. Chạy Flyway V1 trên public schema trống.
4. Dừng backend local và tạo snapshot cuối để có mốc dữ liệu nhất quán.
5. Kiểm tra SHA-256, import theo thứ tự foreign key và rollback toàn bộ nếu có lỗi.
6. Đồng bộ 22 identity sequence và đối chiếu số dòng từng bảng.
7. Khởi động backend bằng `tools/start-backend-supabase.ps1`.
8. Giữ PostgreSQL local và hai snapshot làm rollback.

## Cấu hình kết nối dự kiến

Runtime backend dùng các biến:

```text
DB_HOST=<session-pooler-host>
DB_PORT=5432
DB_NAME=postgres
DB_USER=<pooler-user>
DB_PASSWORD=<stored-outside-git>
DB_SSLMODE=require
DB_POOL_MAX_SIZE=5
DB_POOL_MIN_IDLE=1
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false
JPA_DDL_AUTO=validate
```

Migration và backup/restore ưu tiên Direct connection. Backend chạy lâu dài dùng Direct connection nếu môi trường hỗ trợ IPv6; nếu không thì dùng Session pooler IPv4.

## Quy tắc an toàn

- Không bật lại `ddl-auto=update` trên cloud.
- Không bật `FLYWAY_BASELINE_ON_MIGRATE=true` cho database Supabase trống.
- Không xóa PostgreSQL local sau khi chuyển.
- Không để app/web truy cập trực tiếp database bằng password backend.
- Chỉ dùng transaction pooler cổng 6543 cho workload serverless; backend Render là tiến trình lâu dài nên ưu tiên session mode cổng 5432.
