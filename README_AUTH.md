# Bổ sung phân quyền 3 role

Backend đã bổ sung 3 role:

- ADMIN: quản trị hệ thống
- USER: chủ ao / khách hàng
- TECHNICIAN: nhân viên vận hành / kỹ thuật viên

## Tài khoản demo tự tạo khi backend chạy lần đầu

| Username | Password | Role |
|---|---|---|
| admin | REPLACE_WITH_LOCAL_ADMIN_PASSWORD | ADMIN |
| user | REPLACE_WITH_LOCAL_USER_PASSWORD | USER |
| tech | REPLACE_WITH_LOCAL_TECH_PASSWORD | TECHNICIAN |

## API công khai

- GET /api/health
- POST /api/auth/login

## API cho Arduino

Arduino vẫn dùng `X-API-Key`, không dùng tài khoản user:

- POST /api/readings
- GET /api/commands/pending?deviceId=device_01
- POST /api/commands/{id}/ack

## API cho App/Web

App/Web phải đăng nhập trước và gửi header:

Authorization: Bearer <token>

Các API yêu cầu token:

- GET /api/readings/latest
- GET /api/readings/history
- GET /api/readings/range
- GET /api/dashboard/summary
- POST /api/commands
- GET /api/commands/history
- GET /api/users  chỉ ADMIN
- POST /api/users chỉ ADMIN

## Lưu ý

Bản này mới kiểm tra phân quyền theo role. Nếu sau này mở rộng nhiều ao, cần thêm bảng `ponds`, `devices`, `user_pond_access` để kiểm tra user nào được thao tác trên ao nào.


## Register có email

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "username": "khach01",
  "password": "123456",
  "fullName": "Chủ ao nuôi 01",
  "phone": "0987654321",
  "email": "khach01@example.com"
}
```
