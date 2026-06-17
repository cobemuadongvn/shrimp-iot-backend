# Cập nhật: Bơm 1 chỉ chạy khi người dùng bấm “Đo ngay”

## Ý nghĩa thay đổi

Backend không tự chạy bơm 1 theo lịch. Chu kỳ đo chỉ bắt đầu khi web/app gọi API đo ngay.

API chính cho nút **Đo ngay**:

```http
POST /api/sampling/measurement/measure-now
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "deviceId": "device_01",
  "sampleSource": "Buồng đo trung tâm"
}
```

Endpoint cũ vẫn được giữ để tương thích:

```http
POST /api/sampling/measurement/start
```

Logic của `/measurement/start` giống `/measurement/measure-now`.

## Luồng relay khi bấm “Đo ngay”

```text
Người dùng bấm Đo ngay
→ Frontend gọi POST /api/sampling/measurement/measure-now
→ Backend tạo command Relay 1 ON
→ Chờ fillDurationSeconds
→ Backend tạo command Relay 1 OFF
→ Chờ stabilizingSeconds
→ Chuyển trạng thái MEASURING
→ Chờ measurementDurationSeconds
→ Backend tạo command Relay 2 ON
→ Chờ measurementDrainDurationSeconds
→ Backend tạo command Relay 2 OFF
→ Hoàn thành chu kỳ đo
```

## Phân biệt 2 chế độ

### MANUAL

- Bơm 1/2 chỉ chạy khi bấm **Đo ngay**.
- Nếu dữ liệu vượt ngưỡng, backend chỉ tạo cảnh báo.
- Bơm 3/4 không tự chạy; chủ ao tự bật/tắt nếu muốn xử lý.

### AI_AUTO

- Bơm 1/2 vẫn chỉ chạy khi bấm **Đo ngay**.
- Sau khi Arduino gửi dữ liệu đo mới lên backend, nếu độ mặn cao và đủ điều kiện an toàn, backend tự chạy chu trình xử lý độ mặn bằng relay 3/4.

## API cho frontend

```js
export const measureNow = async (deviceId = "device_01") => {
  const res = await api.post("/sampling/measurement/measure-now", {
    deviceId,
    sampleSource: "Buồng đo trung tâm",
  });
  return res.data;
};
```

## Cách test Postman

1. Login admin lấy token.
2. Gọi:

```http
POST http://192.168.1.8:8080/api/sampling/measurement/measure-now
```

Headers:

```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "deviceId": "device_01",
  "sampleSource": "Buồng đo trung tâm"
}
```

3. Mở Arduino Serial Monitor, kiểm tra Arduino lấy pending command và relay 1 bắt đầu chạy.
