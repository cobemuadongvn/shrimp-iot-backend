# Permission Matrix — ADMIN / USER / TECHNICIAN

## Vai trò

```text
ADMIN      Quản trị hệ thống.
USER       Chủ ao/khách hàng.
TECHNICIAN Kỹ thuật viên/vận hành.
```

## Ma trận quyền

| Nhóm chức năng | ADMIN | USER | TECHNICIAN |
|---|---:|---:|---:|
| Đăng nhập | Có | Có nếu đã duyệt/active | Có nếu đã duyệt/active |
| Đăng ký tài khoản | Không cần | Có | Có |
| Xem danh sách user | Có | Không | Không |
| Xem user chờ duyệt | Có | Không | Không |
| Duyệt user | Có | Không | Không |
| Từ chối user | Có | Không | Không |
| Khóa/mở khóa user | Có | Không | Không |
| Đổi role | Có | Không | Không |
| Tạo/sửa/vô hiệu hóa ao | Có | Không | Không |
| Xem danh sách ao | Tất cả | Ao được gán | Ao được gán |
| Xem chi tiết ao | Tất cả | Ao được gán | Ao được gán |
| Cấp/gỡ quyền ao | Có | Không | Không |
| Đăng ký/sửa/vô hiệu hóa/kích hoạt thiết bị | Có | Không | Có nếu thuộc ao được gán OWNER/READ_WRITE/CONTROL |
| Xem thiết bị | Tất cả | Thiết bị thuộc ao được gán | Thiết bị thuộc ao được gán |
| Gán thiết bị vào ao | Có | Không | Có nếu có quyền OWNER/READ_WRITE/CONTROL ở ao liên quan |
| Xem cảm biến mới nhất | Tất cả | Thiết bị được gán | Thiết bị được gán |
| Xem lịch sử cảm biến | Tất cả | Thiết bị được gán | Thiết bị được gán |
| Xem cảnh báo | Tất cả | Thiết bị được gán | Thiết bị được gán |
| Xử lý cảnh báo | Có | Có nếu được gán | Có nếu được gán |
| Bật/tắt relay | Có | Có nếu OWNER/READ_WRITE/CONTROL | Có nếu READ_WRITE/CONTROL |
| Xem lịch sử điều khiển | Tất cả | Thiết bị được gán | Thiết bị được gán |
| Điều khiển tự động / scenario | Có | Không nên | Không nên |
| Chatbot | Có | Có | Có |
| Báo cáo | Toàn bộ | Phạm vi được gán | Phạm vi được gán |
| Audit log | Có | Không | Không |

## Quy tắc bảo mật cần giữ

```text
1. User chưa được duyệt không được login.
2. User bị khóa không được login và token cũ phải bị thu hồi.
3. User/TECHNICIAN không được gọi API quản trị tài khoản.
4. USER/TECHNICIAN chỉ xem dữ liệu thiết bị thuộc ao được gán.
5. Frontend không gửi X-API-Key.
6. Arduino không dùng Bearer token user.
7. Không cho admin tự khóa chính mình.
8. Không cho khóa admin cuối cùng trong hệ thống.
```

## Test phân quyền nhanh

### ADMIN

```text
GET /api/users → 200
GET /api/users/pending → 200
POST /api/users/{id}/approve → 200
PATCH /api/users/{id}/deactivate → 200
POST /api/commands → 200
```

### USER

```text
GET /api/users → 403 hoặc lỗi quyền
GET /api/users/pending → 403 hoặc lỗi quyền
GET /api/readings/latest?deviceId=device_01 → 200 nếu được gán
POST /api/commands → 200 nếu OWNER/READ_WRITE; lỗi quyền nếu READ_ONLY/không được gán
```

### TECHNICIAN

```text
GET /api/users → 403 hoặc lỗi quyền
GET /api/readings/latest?deviceId=device_01 → 200 nếu được gán
POST /api/commands → 200 nếu READ_WRITE/CONTROL
POST /api/devices?pondId=1 → 200 nếu có OWNER/READ_WRITE/CONTROL ở ao 1
PUT /api/devices/device_01 → 200 nếu thiết bị thuộc ao được gán OWNER/READ_WRITE/CONTROL
PATCH /api/devices/device_01/deactivate → 200 nếu thiết bị thuộc ao được gán OWNER/READ_WRITE/CONTROL
PATCH /api/devices/device_01/activate → 200 nếu thiết bị thuộc ao được gán OWNER/READ_WRITE/CONTROL
POST /api/users/{id}/approve → 403 hoặc lỗi quyền
```
