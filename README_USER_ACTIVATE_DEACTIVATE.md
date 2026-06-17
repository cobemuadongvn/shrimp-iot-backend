# API khóa / mở khóa tài khoản user

Backend đã bổ sung API chuyên nghiệp hơn thay cho API cũ `/api/users/lock?username=...&active=...`.

## 1. Khóa tài khoản

```http
PATCH http://192.168.1.8:8080/api/users/{id}/deactivate
```

Ví dụ:

```http
PATCH http://192.168.1.8:8080/api/users/5/deactivate
```

Headers:

```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "reason": "Người dùng không còn quản lý ao này"
}
```

Response mẫu:

```json
{
  "success": true,
  "message": "User deactivated",
  "data": {
    "id": 5,
    "username": "khach01",
    "fullName": "Nguyễn Văn A",
    "role": "USER",
    "active": false,
    "approvalStatus": "APPROVED"
  },
  "timestamp": "..."
}
```

Khi khóa tài khoản:

- `active = false`
- token cũ của user sẽ bị thu hồi
- user không đăng nhập / gọi API được nữa
- lịch sử chat, lịch sử cảnh báo, lịch sử điều khiển vẫn được giữ lại

## 2. Mở khóa tài khoản

```http
PATCH http://192.168.1.8:8080/api/users/{id}/activate
```

Ví dụ:

```http
PATCH http://192.168.1.8:8080/api/users/5/activate
```

Headers:

```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "reason": "Mở lại tài khoản cho người dùng"
}
```

Response mẫu:

```json
{
  "success": true,
  "message": "User activated",
  "data": {
    "id": 5,
    "username": "khach01",
    "fullName": "Nguyễn Văn A",
    "role": "USER",
    "active": true,
    "approvalStatus": "APPROVED"
  },
  "timestamp": "..."
}
```

## 3. Quy tắc an toàn

Backend đã xử lý:

1. Admin không được tự khóa chính mình.
2. Không được khóa admin cuối cùng đang hoạt động.
3. Khi khóa user, toàn bộ token cũ của user bị thu hồi.
4. API cũ `/api/users/lock` vẫn được giữ lại để web cũ không bị lỗi, nhưng web mới nên dùng API theo `id`.

## 4. Code ReactJS mẫu

```js
export const deactivateUser = async (userId, reason = "") => {
  const res = await api.patch(`/users/${userId}/deactivate`, {
    reason,
  });
  return res.data;
};

export const activateUser = async (userId, reason = "") => {
  const res = await api.patch(`/users/${userId}/activate`, {
    reason,
  });
  return res.data;
};
```
