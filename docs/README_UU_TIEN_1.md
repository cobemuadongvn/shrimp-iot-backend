# Ưu tiên 1 — Bộ hoàn thiện để bảo vệ đồ án

Bản này tập trung vào các việc cần hoàn thiện trước khi bảo vệ:

```text
1. Tài liệu API cho Web/App.
2. Kịch bản kiểm thử end-to-end.
3. Chuẩn hóa dữ liệu demo.
4. Ma trận quyền ADMIN / USER / TECHNICIAN.
5. Tắt auto-control mặc định để tránh relay tự bật trong lúc demo.
6. Danh sách ảnh/video cần chụp để đưa vào báo cáo hoặc slide.
7. Kịch bản thuyết trình demo 10 phút.
8. Postman collection để test nhanh toàn bộ backend.
```

## Trạng thái đã hoàn thiện

### 1. API documentation

File:

```text
docs/API_CONTRACT_WEB_APP.md
```

Dùng để gửi cho bạn làm Web/App. Tài liệu nêu rõ:

```text
- Method
- URL
- Header
- Body
- Response shape
- API nào dành cho Web/App
- API nào chỉ dành cho Arduino
```

### 2. End-to-end test checklist

File:

```text
docs/E2E_TEST_CHECKLIST.md
```

Luồng kiểm thử chính:

```text
Backend health
→ Login admin
→ Arduino gửi sensor
→ Web đọc sensor mới nhất
→ Web bật relay
→ Arduino lấy pending command
→ Arduino ACK
→ Web xem command history
→ Kiểm tra alert
→ Kiểm tra quyền USER / TECHNICIAN
→ Kiểm tra chatbot
```

### 3. Demo data

Backend có `DataInitializer`, tự tạo dữ liệu seed khi chạy lần đầu:

```text
admin / REPLACE_WITH_LOCAL_ADMIN_PASSWORD
user  / REPLACE_WITH_LOCAL_USER_PASSWORD
user2 / REPLACE_WITH_LOCAL_USER_PASSWORD
tech  / REPLACE_WITH_LOCAL_TECH_PASSWORD

Ao tôm thẻ 01 → device_01
Ao tôm thẻ 02 → device_02
Ao tôm thẻ 03 → device_03
```

Chi tiết nằm ở:

```text
docs/DEMO_DATA.md
```

### 4. Permission matrix

File:

```text
docs/PERMISSION_MATRIX.md
```

Quy định rõ quyền của:

```text
ADMIN
USER
TECHNICIAN
```

### 5. Auto-control safety

Trong `application.yml`, auto-control đang để mặc định:

```yaml
auto-control:
  enabled: ${AUTO_CONTROL_ENABLED:false}
```

Điều này phù hợp khi demo, tránh thiết bị tự bật/tắt ngoài ý muốn.

### 6. Postman collection

Thư mục:

```text
postman/
```

Gồm:

```text
Shrimp-IoT-Priority-1.postman_collection.json
Shrimp-IoT-Local.postman_environment.json
```

Import vào Postman, chọn environment, chạy theo thứ tự folder.

## Cách chạy backend

```bash
docker compose up -d
mvn clean compile
mvn spring-boot:run
```

Nếu máy chưa có Maven, dùng Maven wrapper nếu project có `mvnw.cmd`; nếu không thì cài Apache Maven thủ công.

## Test nhanh backend

```text
http://192.168.1.8:8080/api/health
```

Nếu máy khác cùng WiFi không mở được link này, kiểm tra:

```text
1. Backend đang chạy chưa.
2. IP máy chạy backend có đúng 192.168.1.8 không.
3. Windows Firewall đã mở port 8080 chưa.
4. application.yml có server.address=0.0.0.0 chưa.
5. Web có dùng đúng BASE_URL không.
```
