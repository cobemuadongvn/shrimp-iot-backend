# API cho web/app: 2 chế độ vận hành + 4 máy bơm

Base URL:

```text
http://192.168.1.8:8080/api
```

Header các API web/app:

```http
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

## 1. Relay mapping

| Relay | Chức năng |
|---:|---|
| 1 | Bơm nước vào buồng đo |
| 2 | Bơm nước ra khỏi buồng đo |
| 3 | Bơm xả nước ao khi độ mặn cao |
| 4 | Bơm nước ngọt vào ao |

## 2. Chế độ vận hành

### Lấy chế độ hiện tại

```http
GET /devices/device_01/operation-mode
```

### Chọn thủ công

```http
PATCH /devices/device_01/operation-mode
```

```json
{
  "operationMode": "MANUAL"
}
```

### Chọn AI tự động

```http
PATCH /devices/device_01/operation-mode
```

```json
{
  "operationMode": "AI_AUTO"
}
```

## 3. Chu kỳ đo nước

### Đo ngay

Bơm 1 chỉ bắt đầu chạy khi người dùng bấm nút **Đo ngay** trên web/app và frontend gọi API này. Backend không tự chạy bơm 1 theo lịch.

```http
POST /sampling/measurement/measure-now
```

```json
{
  "deviceId": "device_01",
  "sampleSource": "Buồng đo trung tâm"
}
```

### Current

```http
GET /sampling/measurement/current?deviceId=device_01
```

### History

```http
GET /sampling/measurement/history?deviceId=device_01&limit=50
```

## 4. Xử lý độ mặn

### Start thủ công một chu trình xử lý

```http
POST /sampling/salinity/start
```

```json
{
  "deviceId": "device_01",
  "currentSalinity": 38.5
}
```

### Current

```http
GET /sampling/salinity/current?deviceId=device_01
```

### History

```http
GET /sampling/salinity/history?deviceId=device_01&limit=50
```

## 5. Logic web/app

- Bơm 1/2 chỉ chạy khi người dùng bấm **Đo ngay**.
- Nếu `operationMode = MANUAL`: web/app hiển thị cảnh báo, người dùng tự bấm relay 3/4 bằng `/commands`.
- Nếu `operationMode = AI_AUTO`: khi backend nhận reading có salinity > threshold, backend tự tạo chu trình relay 3/4.
- Web/app nên có nút xác nhận trước khi bật `AI_AUTO`.
