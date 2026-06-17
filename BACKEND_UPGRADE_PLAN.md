# Kế hoạch nâng cấp backend tiếp theo

Backend hiện đã có: quản lý ao/thiết bị/relay, phân quyền, đăng ký chờ duyệt, duyệt/từ chối user, khóa/mở khóa user theo id, thu hồi token khi khóa tài khoản, cảm biến, cảnh báo, auto-control, relay state, websocket, chatbot Pha 1/Pha 2.

## Ưu tiên 1 — Hoàn thiện vận hành thực tế

0. **Đã hoàn thiện trong bản này: khóa/mở khóa tài khoản chuẩn RESTful**
   - `PATCH /api/users/{id}/deactivate`
   - `PATCH /api/users/{id}/activate`
   - Không xóa cứng user để giữ lịch sử vận hành.
   - Khi khóa user, token cũ bị thu hồi.

1. **Device API key riêng từng thiết bị**
   - Hiện tại Arduino dùng chung `MY_SECRET_KEY`.
   - Nên thêm `apiKeyHash` vào bảng `devices` để mỗi thiết bị có khóa riêng.

2. **Cấu hình relay an toàn**
   - Thêm giới hạn thời gian chạy tối đa theo relay.
   - Ví dụ máy bơm không chạy quá 10 phút nếu không có xác nhận.

3. **Audit log đầy đủ**
   - Lưu ai đăng nhập, ai bật/tắt relay, ai duyệt user, ai đổi ngưỡng.
   - Bảng đề xuất: `audit_logs`.

## Ưu tiên 2 — Chatbot nâng cao

1. **Pha 3: Chatbot điều khiển có xác nhận**
   - User: “Bật máy bơm”.
   - Bot: “Bạn xác nhận bật relay 1 - máy bơm không?”.
   - User: “Có”.
   - Backend mới tạo command.

2. **Tích hợp LLM thật**
   - OpenRouter/Gemini/OpenAI.
   - Vẫn cần guardrail: không tự bật relay nếu chưa xác nhận.

3. **RAG tài liệu kỹ thuật nuôi tôm**
   - Lưu tài liệu hướng dẫn xử lý pH, DO, nhiệt độ.
   - Bot trả lời dựa trên tài liệu nội bộ.

## Ưu tiên 3 — Báo cáo và dashboard

1. **Báo cáo ngày/tuần/tháng**
   - pH min/max/avg.
   - DO min/max/avg.
   - Số cảnh báo.
   - Thời gian thiết bị chạy.

2. **Export CSV/PDF**
   - API: `/api/reports/export-csv`.

3. **Biểu đồ realtime bằng WebSocket/SSE**
   - Web đã có thể subscribe dữ liệu realtime.

## Ưu tiên 4 — Deploy thật

1. **Dockerfile cho backend**
2. **docker-compose.prod.yml** gồm backend + PostgreSQL + Nginx
3. **HTTPS/domain**
4. **Backup database tự động**
5. **Flyway/Liquibase thay cho ddl-auto:update**

## Ưu tiên 5 — Bảo mật

1. **JWT chuẩn + refresh token**
2. **Rate limit login/register**
3. **Mã hóa/ẩn thông tin nhạy cảm**
4. **Không để device API key ở frontend**
5. **CORS theo domain deploy thật**
