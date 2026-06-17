# Shrimp IoT backend - clean fixed build

Bản này đã được dọn lại theo một kiến trúc duy nhất `com.example.shrimpiot.*`.

## Đã sửa

1. Xóa các package module trùng gây lỗi bean name conflict:
   - `com.example.shrimpiot.aquaculture.*`
   - `com.example.shrimpiot.automation.*`
   - `com.example.shrimpiot.device.*`
   - `com.example.shrimpiot.monitoring.*`

2. Giữ lại bộ controller/service/repository/model chính ở:
   - `src/main/java/com/example/shrimpiot/controller`
   - `src/main/java/com/example/shrimpiot/service`
   - `src/main/java/com/example/shrimpiot/repository`
   - `src/main/java/com/example/shrimpiot/model`

3. Sửa `application.yml`:
   - `server.address=0.0.0.0`
   - PostgreSQL password mặc định `123456`
   - `spring.jpa.open-in-view=false`
   - MQTT broker cho backend: `tcp://localhost:1883`

4. Sửa `docker-compose.yml` để PostgreSQL password mặc định khớp backend: `123456`.

5. Sửa `AuthTokenRepository` bằng `@EntityGraph(attributePaths = "user")` để tránh lỗi Hibernate lazy proxy `UserAccount#1 - no session`.

6. Cập nhật file Arduino trong thư mục `arduino/...` sang relay active HIGH:
   - `HIGH = ON`
   - `LOW = OFF`

## Cách chạy khi chưa có Maven CLI

Không copy đè vào thư mục cũ. Hãy giải nén ZIP này vào thư mục mới hoàn toàn, ví dụ:

`D:\shrimp-iot-complete-work-clean-fixed`

Sau đó mở thư mục đó bằng VS Code.

Trong VS Code:

1. `Ctrl + Shift + P`
2. Chọn `Java: Clean Java Language Server Workspace`
3. Chọn `Restart and delete`
4. Mở file `src/main/java/com/example/shrimpiot/ShrimpIotApplication.java`
5. Bấm `Run`

## Docker

Trước khi chạy backend:

```powershell
docker compose up -d
```

Nếu cần kiểm tra MQTT:

```powershell
Test-NetConnection localhost -Port 1883
Test-NetConnection 192.168.1.89 -Port 1883
```

## API base URL

Dùng trong Postman/Web/App:

`http://192.168.1.89:8080`

Backend tự kết nối MQTT broker bằng:

`tcp://localhost:1883`

Arduino vẫn dùng MQTT host:

`192.168.1.89`
