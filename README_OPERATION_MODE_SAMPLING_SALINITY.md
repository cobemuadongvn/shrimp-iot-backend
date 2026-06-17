# Backend cập nhật: 2 chế độ vận hành + lấy mẫu nước + xử lý độ mặn

Bản này bổ sung đúng luồng phần cứng 4 máy bơm:

| Relay | Thiết bị | Chức năng |
|---:|---|---|
| 1 | Bơm nước vào buồng đo | Đưa nước vào để cảm biến đo |
| 2 | Bơm nước ra khỏi buồng đo | Xả nước khỏi buồng đo sau khi đo |
| 3 | Bơm xả nước ao | Xả bớt nước ao khi độ mặn cao |
| 4 | Bơm nước ngọt vào ao | Pha loãng độ mặn khi độ mặn cao |

## 1. Hai chế độ vận hành

### MANUAL — Thủ công

- Bơm 1 và bơm 2 chỉ chạy khi người dùng bấm **Đo ngay** hoặc frontend gọi API đo.
- Nếu dữ liệu vượt ngưỡng, backend chỉ tạo cảnh báo.
- Chủ ao/kỹ thuật viên tự quyết định bật/tắt relay 3 và relay 4 từ web/app.
- Đây là chế độ an toàn mặc định.

### AI_AUTO — AI tự động / tự động thông minh

- Bơm 1 và bơm 2 chỉ chạy khi người dùng bấm **Đo ngay** hoặc frontend gọi API đo.
- Nếu độ mặn cao hơn ngưỡng, backend tự tạo chu trình xử lý:
  1. Bật relay 3 để xả bớt nước ao.
  2. Tắt relay 3.
  3. Bật relay 4 để bơm nước ngọt vào ao.
  4. Tắt relay 4.
  5. Chờ nước hòa trộn.
  6. Đọc dữ liệu mới nhất để kiểm tra lại.
  7. Nếu độ mặn vẫn cao, lặp lại trong giới hạn `maxRetryCount`.
  8. Nếu vẫn chưa ổn, chuyển trạng thái `NEED_MANUAL_CHECK`.

Lưu ý: Trong báo cáo nên ghi đây là **chế độ tự động thông minh dựa trên luật điều khiển**, chưa phải mô hình học máy.

## 2. API chế độ vận hành

### Lấy cấu hình vận hành

```http
GET /api/devices/{deviceId}/operation-mode
Authorization: Bearer <TOKEN>
```

Ví dụ:

```http
GET http://192.168.1.8:8080/api/devices/device_01/operation-mode
```

### Đổi sang chế độ thủ công

```http
PATCH /api/devices/{deviceId}/operation-mode
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "operationMode": "MANUAL"
}
```

### Đổi sang chế độ AI tự động

```http
PATCH /api/devices/{deviceId}/operation-mode
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "operationMode": "AI_AUTO"
}
```

Khi chuyển sang `AI_AUTO`, backend tự set `salinityAutoEnabled = true`.

## 3. API cấu hình xử lý độ mặn

```http
PATCH /api/devices/{deviceId}/salinity-control-config
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body mẫu:

```json
{
  "salinityAutoEnabled": true,
  "salinityHighThreshold": 35.0,
  "salinityStopThreshold": 32.0,
  "salinityDrainDurationSeconds": 30,
  "freshwaterDurationSeconds": 30,
  "mixingWaitSeconds": 180,
  "maxRetryCount": 3,
  "cooldownMinutes": 10
}
```

Ý nghĩa:

- `salinityHighThreshold`: vượt ngưỡng này thì AI_AUTO bắt đầu xử lý.
- `salinityStopThreshold`: về thấp hơn hoặc bằng ngưỡng này thì dừng xử lý.
- `salinityDrainDurationSeconds`: thời gian bật bơm 3.
- `freshwaterDurationSeconds`: thời gian bật bơm 4.
- `mixingWaitSeconds`: thời gian chờ nước hòa trộn.
- `maxRetryCount`: số lần xử lý tối đa.
- `cooldownMinutes`: khoảng nghỉ tối thiểu giữa 2 chu kỳ xử lý tự động.

## 4. API cấu hình chu kỳ đo

```http
PATCH /api/devices/{deviceId}/measurement-config
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body mẫu:

```json
{
  "fillDurationSeconds": 20,
  "stabilizingSeconds": 30,
  "measurementDurationSeconds": 20,
  "measurementDrainDurationSeconds": 20
}
```

## 5. API chu kỳ lấy mẫu và đo nước

### Bắt đầu đo ngay

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

Khi người dùng bấm nút **Đo ngay**, frontend gọi API này. Khi đó backend mới tạo command:

```text
Relay 1 ON
→ chờ fillDurationSeconds
→ Relay 1 OFF
→ chờ stabilizingSeconds
→ MEASURING
→ chờ measurementDurationSeconds
→ Relay 2 ON
→ chờ measurementDrainDurationSeconds
→ Relay 2 OFF
→ COMPLETED
```

### Xem trạng thái hiện tại

```http
GET /api/sampling/measurement/current?deviceId=device_01
Authorization: Bearer <TOKEN>
```

### Xem lịch sử

```http
GET /api/sampling/measurement/history?deviceId=device_01&limit=50
Authorization: Bearer <TOKEN>
```

## 6. API chu kỳ xử lý độ mặn

### Chạy xử lý độ mặn thủ công 1 lần

Dùng khi người dùng đang ở chế độ thủ công nhưng vẫn muốn kích hoạt chu trình tự động một lần.

```http
POST /api/sampling/salinity/start
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Body:

```json
{
  "deviceId": "device_01",
  "currentSalinity": 38.5
}
```

Nếu không gửi `currentSalinity`, backend sẽ lấy độ mặn mới nhất trong bảng `sensor_readings`.

### Xem trạng thái xử lý độ mặn

```http
GET /api/sampling/salinity/current?deviceId=device_01
Authorization: Bearer <TOKEN>
```

### Xem lịch sử xử lý độ mặn

```http
GET /api/sampling/salinity/history?deviceId=device_01&limit=50
Authorization: Bearer <TOKEN>
```

## 7. API điều khiển thủ công relay 3/4

Ở chế độ MANUAL, backend chỉ cảnh báo; web/app gọi API này để chủ ao tự bật/tắt bơm 3/4.

```http
POST /api/commands
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

Bật bơm xả nước ao:

```json
{
  "deviceId": "device_01",
  "relayNo": 3,
  "action": "ON"
}
```

Tắt bơm xả nước ao:

```json
{
  "deviceId": "device_01",
  "relayNo": 3,
  "action": "OFF"
}
```

Bật bơm nước ngọt:

```json
{
  "deviceId": "device_01",
  "relayNo": 4,
  "action": "ON"
}
```

Tắt bơm nước ngọt:

```json
{
  "deviceId": "device_01",
  "relayNo": 4,
  "action": "OFF"
}
```

## 8. Arduino có cần sửa không?

Không cần sửa API Arduino nếu code hiện tại vẫn dùng:

```http
POST /api/readings
GET  /api/commands/pending?deviceId=device_01
POST /api/commands/{id}/ack
```

Backend chỉ tạo thêm command PENDING cho relay 1/2/3/4. Arduino vẫn lấy lệnh pending và thực hiện như trước.

## 9. Câu mô tả trong báo cáo

Hệ thống được thiết kế với hai chế độ vận hành: chế độ thủ công và chế độ tự động thông minh. Ở chế độ thủ công, hệ thống thực hiện chu kỳ lấy mẫu nước, đo thông số môi trường và tạo cảnh báo khi vượt ngưỡng; người dùng tự quyết định bật/tắt các máy bơm xử lý. Ở chế độ tự động thông minh, khi phát hiện độ mặn cao, backend tự động kích hoạt chu trình xả bớt nước ao và bơm nước ngọt vào nhằm pha loãng độ mặn, sau đó chờ hòa trộn và kiểm tra lại dữ liệu cảm biến. Nếu độ mặn chưa trở về ngưỡng an toàn sau số lần xử lý tối đa, hệ thống dừng tự động và yêu cầu người vận hành kiểm tra thủ công.
