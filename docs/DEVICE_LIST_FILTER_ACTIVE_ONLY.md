# Device list filter — chỉ trả thiết bị thật đang ACTIVE

## Mục tiêu

Frontend chỉ nên nhận các thiết bị điều khiển thật, ví dụ `device_01`. Các dòng dữ liệu cũ bị lưu nhầm trong bảng `devices`, ví dụ `pond_01`, không được trả về từ API danh sách thiết bị hoặc bản đồ.

## Các API đã được chỉnh

### `GET /api/devices`

- `ADMIN`: chỉ nhận thiết bị có `status = ACTIVE`.
- `TECHNICIAN` / `USER`: chỉ nhận thiết bị `ACTIVE` thuộc các ao được phân quyền.
- Backend lọc bổ sung các `deviceId` bắt đầu bằng `pond_` để tránh dữ liệu ao cũ xuất hiện như thiết bị.

### `GET /api/map/devices`

Áp dụng cùng quy tắc như `GET /api/devices`.

### `GET /api/map/ponds`

Trong mỗi ao, danh sách `devices` lồng bên trong chỉ chứa thiết bị `ACTIVE` và không chứa các mã bắt đầu bằng `pond_`.

### `DeviceConnectionMonitorService`

Scheduler kiểm tra offline chỉ xử lý thiết bị `ACTIVE` thật, không tạo cảnh báo offline cho dữ liệu rác như `pond_01`.

## Repository methods đã thêm

```java
Optional<Device> findByDeviceIdAndStatus(String deviceId, String status);
List<Device> findByPondAndStatus(Pond pond, String status);
List<Device> findByStatus(String status);
List<Device> findByPondInAndStatus(List<Pond> ponds, String status);
```

## SQL xử lý dữ liệu cũ trong database

Khuyến nghị deactivate trước, không xóa cứng:

```sql
UPDATE devices
SET status = 'INACTIVE'
WHERE device_id = 'pond_01';
```

Lệnh Docker Windows:

```bat
docker exec -it shrimp-postgres psql -U shrimp_user -d shrimp_iot -c "UPDATE devices SET status = 'INACTIVE' WHERE device_id = 'pond_01';"
```

Kiểm tra lại:

```bat
docker exec -it shrimp-postgres psql -U shrimp_user -d shrimp_iot -c "SELECT id, device_id, name, status FROM devices ORDER BY id;"
```

## Kết quả mong muốn

Khi gọi:

```http
GET /api/devices
Authorization: Bearer <TOKEN>
```

Frontend chỉ còn nhận thiết bị thật, ví dụ:

```json
[
  {
    "deviceId": "device_01",
    "name": "Bộ điều khiển Ao 1",
    "status": "ACTIVE"
  }
]
```
