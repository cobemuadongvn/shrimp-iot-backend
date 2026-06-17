# API user có email

Backend đã bổ sung trường `email` cho tài khoản người dùng.

## 1. Field user hiện có

Response user có các trường chính:

```json
{
  "id": 5,
  "username": "khach01",
  "fullName": "Chủ ao nuôi 01",
  "phone": "0987654321",
  "email": "khach01@example.com",
  "role": "USER",
  "active": false,
  "approvalStatus": "PENDING",
  "approvedBy": null,
  "approvedAt": null,
  "createdAt": "..."
}
```

## 2. Đăng ký user mới

```http
POST http://192.168.1.8:8080/api/auth/register
Content-Type: application/json
```

Body:

```json
{
  "username": "khach01",
  "password": "123456",
  "fullName": "Chủ ao nuôi 01",
  "phone": "0987654321",
  "email": "khach01@example.com"
}
```

Sau khi đăng ký:

- `role = USER`
- `active = false`
- `approvalStatus = PENDING`
- user chưa đăng nhập được cho tới khi Admin duyệt

## 3. Admin tạo user trực tiếp

```http
POST http://192.168.1.8:8080/api/users
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "username": "khach02",
  "password": "123456",
  "fullName": "Chủ ao nuôi 02",
  "phone": "0987654322",
  "email": "khach02@example.com",
  "role": "USER"
}
```

## 4. Quy tắc email

- `email` là optional, user có thể không nhập email.
- Nếu có email thì backend kiểm tra đúng định dạng email.
- Backend lưu email dạng chữ thường, ví dụ `ABC@GMAIL.COM` sẽ lưu thành `abc@gmail.com`.
- Email không được trùng với user khác.
- Nếu chạy với `spring.jpa.hibernate.ddl-auto=update`, database sẽ tự thêm cột `email` vào bảng `users`.

## 5. Gợi ý frontend

Form đăng ký nên có:

```text
Họ và tên
Tên đăng nhập
Mật khẩu
Số điện thoại
Email
```

Khi gọi API, gửi đủ các trường:

```js
await api.post('/auth/register', {
  username,
  password,
  fullName,
  phone,
  email,
});
```
