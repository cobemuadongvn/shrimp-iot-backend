# Backend fix notes

## Các lỗi đã xử lý

1. `PondController.java` báo `Pond cannot be resolved to a type`, `UserAccount cannot be resolved to a type`, `RoleName cannot be resolved to a variable`, `UserPondAccess cannot be resolved`.
   - Nguyên nhân: file controller và một số model/repository liên quan có import wildcard quá rộng, lẫn nhiều package không cần thiết do script tự động sinh import. Điều này làm Java Language Server dễ nhận sai hoặc không cập nhật dependency nội bộ.
   - Cách sửa: thay import wildcard bằng import tường minh cho đúng class: `Pond`, `UserPondAccess`, `UserAccount`, `RoleName`, repository và `AuthService`.

2. `application.yml` yêu cầu biến môi trường `DB_PASSWORD` và `IOT_API_KEY` nhưng không có giá trị mặc định.
   - Nguyên nhân: khi chạy local nếu chưa set biến môi trường, Spring Boot có thể fail ở bước resolve placeholder.
   - Cách sửa: thêm default local: `DB_PASSWORD=123456`, `IOT_API_KEY=MY_SECRET_KEY`.

3. `spring.jpa.hibernate.ddl-auto` mặc định đang là `validate`.
   - Nguyên nhân: nếu database local chưa có đủ bảng, backend sẽ fail khi khởi động.
   - Cách sửa: đổi default local thành `update`. Khi deploy production có thể set `JPA_DDL_AUTO=validate`.

4. `docker-compose.yml`, `.env.example`, `README.md` chưa thống nhất mật khẩu database.
   - Cách sửa: thống nhất local default về `123456`.

5. VS Code Java setting để `java.configuration.updateBuildConfiguration` ở `interactive`.
   - Cách sửa: đổi thành `automatic` để VS Code tự cập nhật lại classpath/Maven project sau khi sửa source.

## File đã chỉnh

- `src/main/java/com/example/shrimpiot/aquaculture/controller/PondController.java`
- `src/main/java/com/example/shrimpiot/aquaculture/model/Pond.java`
- `src/main/java/com/example/shrimpiot/aquaculture/model/UserPondAccess.java`
- `src/main/java/com/example/shrimpiot/aquaculture/repository/PondRepository.java`
- `src/main/java/com/example/shrimpiot/aquaculture/repository/UserPondAccessRepository.java`
- `src/main/java/com/example/shrimpiot/auth/model/UserAccount.java`
- `src/main/java/com/example/shrimpiot/auth/model/RoleName.java`
- `src/main/resources/application.yml`
- `.env.example`
- `docker-compose.yml`
- `.vscode/settings.json`

## Cách chạy lại

```bash
docker compose up -d
mvn clean spring-boot:run
```

Nếu dùng VS Code: mở đúng thư mục gốc chứa `pom.xml`, sau đó chạy `Java: Clean Java Language Server Workspace` nếu vẫn còn lỗi đỏ cũ.
