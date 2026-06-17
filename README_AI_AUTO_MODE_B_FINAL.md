# Backend IoT ao nuôi - bản AI_AUTO phương án B

Bản này chốt luồng 2 chế độ vận hành theo yêu cầu mới nhất:

- `MANUAL`: người dùng bấm **Đo ngay** → backend chạy bơm 1/2 để lấy mẫu và xả nước → nếu vượt ngưỡng thì chỉ cảnh báo → chủ ao tự bật/tắt bơm 3/4.
- `AI_AUTO`: người dùng bấm **Đo ngay** → backend chạy bơm 1/2 → nếu độ mặn cao thì tự bật bơm 3/4 → chờ hòa trộn → tự chạy lại bơm 1/2 để đo lại → lặp có giới hạn → nếu vẫn chưa ổn thì dừng và yêu cầu kiểm tra thủ công.

## Mapping relay

| Relay | Thiết bị | Vai trò |
|---:|---|---|
| 1 | Bơm nước vào buồng đo | Lấy mẫu nước vào vùng cảm biến |
| 2 | Bơm nước ra khỏi buồng đo | Xả nước sau khi đo |
| 3 | Bơm xả nước ao | Xả bớt nước ao khi độ mặn cao |
| 4 | Bơm nước ngọt vào ao | Bơm nước ngọt để pha loãng độ mặn |

## Cấu hình mặc định phù hợp đồ án

| Cấu hình | Giá trị mặc định | Ý nghĩa |
|---|---:|---|
| `salinityHighThreshold` | `35.0‰` | Bắt đầu xử lý khi độ mặn cao hơn giá trị này |
| `salinityStopThreshold` | `32.0‰` | Dừng xử lý khi độ mặn đã về dưới giá trị này |
| `fillDurationSeconds` | `20` | Thời gian bật bơm 1 để đưa nước vào buồng đo |
| `stabilizingSeconds` | `45` | Chờ nước/cảm biến ổn định trước khi lấy reading |
| `measurementDurationSeconds` | `30` | Khoảng thời gian chờ Arduino gửi reading mới |
| `measurementDrainDurationSeconds` | `20` | Thời gian bật bơm 2 để xả buồng đo |
| `salinityDrainDurationSeconds` | `20` | Thời gian bật bơm 3 để xả nước ao |
| `freshwaterDurationSeconds` | `25` | Thời gian bật bơm 4 để cấp nước ngọt |
| `mixingWaitSeconds` | `120` | Chờ nước hòa trộn trước khi AI tự đo lại |
| `maxRetryCount` | `2` | Số lần xử lý tối đa trong một chu kỳ AI |
| `cooldownMinutes` | `10` | Chặn việc tự xử lý liên tục quá dày |
| `readingMaxAgeSeconds` | `120` | Chỉ dùng reading mới trong khoảng thời gian này |
| `autoRemeasureEnabled` | `true` | Bật tự đo lại theo phương án B |
| `safetyLockEnabled` | `false` | Khi true, AI không được tự chạy bơm |

Lưu ý: đây là cấu hình an toàn cho mô hình đồ án. Khi triển khai ao thật phải hiệu chuẩn lại theo thể tích ao, công suất bơm, độ mặn nguồn nước và tốc độ hòa trộn.

## API chính

### 1. Đổi sang chế độ thủ công

```http
PATCH /api/devices/device_01/operation-mode
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "operationMode": "MANUAL"
}
```

### 2. Đổi sang chế độ AI tự động

```http
PATCH /api/devices/device_01/operation-mode
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "operationMode": "AI_AUTO"
}
```

Khi đổi sang `AI_AUTO`, backend tự set `salinityAutoEnabled=true`.

### 3. Cập nhật cấu hình AI xử lý độ mặn

```http
PATCH /api/devices/device_01/salinity-control-config
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "salinityAutoEnabled": true,
  "salinityHighThreshold": 35.0,
  "salinityStopThreshold": 32.0,
  "salinityDrainDurationSeconds": 20,
  "freshwaterDurationSeconds": 25,
  "mixingWaitSeconds": 120,
  "maxRetryCount": 2,
  "cooldownMinutes": 10,
  "readingMaxAgeSeconds": 120,
  "autoRemeasureEnabled": true,
  "safetyLockEnabled": false
}
```

### 4. Cập nhật cấu hình chu kỳ đo

```http
PATCH /api/devices/device_01/measurement-config
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "fillDurationSeconds": 20,
  "stabilizingSeconds": 45,
  "measurementDurationSeconds": 30,
  "measurementDrainDurationSeconds": 20
}
```

### 5. Người dùng bấm Đo ngay

```http
POST /api/sampling/measurement/measure-now
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "deviceId": "device_01",
  "sampleSource": "Buồng đo trung tâm"
}
```

## Luồng MANUAL

```text
Người dùng bấm Đo ngay
→ Relay 1 ON
→ Relay 1 OFF
→ Chờ ổn định
→ Chờ Arduino gửi reading mới
→ Relay 2 ON
→ Relay 2 OFF
→ Backend phân tích ngưỡng
→ Nếu độ mặn cao: chỉ cảnh báo, không bật relay 3/4
→ Chủ ao tự bật/tắt relay 3/4 nếu muốn
```

## Luồng AI_AUTO phương án B

```text
Người dùng bấm Đo ngay
→ Relay 1 ON
→ Relay 1 OFF
→ Chờ ổn định
→ Chờ Arduino gửi reading mới
→ Relay 2 ON
→ Relay 2 OFF
→ Backend phân tích độ mặn
→ Nếu độ mặn > salinityHighThreshold:
   → Tạo salinity correction cycle
   → Relay 3 ON để xả bớt nước ao
   → Relay 3 OFF
   → Relay 4 ON để bơm nước ngọt vào
   → Relay 4 OFF
   → Chờ mixingWaitSeconds
   → Relay 1 ON để tự đo lại
   → Relay 1 OFF
   → Chờ ổn định
   → Chờ Arduino gửi reading mới
   → Relay 2 ON để xả buồng đo
   → Relay 2 OFF
   → Nếu salinity <= salinityStopThreshold: COMPLETED
   → Nếu vẫn cao: lặp lại nếu retry < maxRetryCount
   → Nếu quá số lần: NEED_MANUAL_CHECK
```

## Điểm an toàn đã chỉnh

- Không còn tự chạy xử lý độ mặn chỉ vì Arduino gửi reading liên tục.
- AI_AUTO chỉ bắt đầu sau nút **Đo ngay** hoặc sau lần tự đo lại trong cycle AI.
- Có hysteresis: ngưỡng bật `35‰`, ngưỡng dừng `32‰`.
- Có cooldown `10 phút` để tránh tự xử lý liên tục.
- Có `maxRetryCount=2` để không bơm quá nhiều lần.
- Có `readingMaxAgeSeconds=120` để không dùng dữ liệu cảm biến quá cũ.
- Có `safetyLockEnabled` để khóa AI khi cần bảo trì/demo.
- Khi lỗi hoặc vượt retry, backend tạo lệnh OFF cho các relay liên quan.

## Lưu ý test nhanh khi demo

Nếu cần demo nhanh, có thể giảm thời gian tạm thời:

```json
{
  "fillDurationSeconds": 5,
  "stabilizingSeconds": 5,
  "measurementDurationSeconds": 5,
  "measurementDrainDurationSeconds": 5,
  "salinityDrainDurationSeconds": 5,
  "freshwaterDurationSeconds": 5,
  "mixingWaitSeconds": 10,
  "maxRetryCount": 1,
  "cooldownMinutes": 1,
  "readingMaxAgeSeconds": 60,
  "autoRemeasureEnabled": true,
  "safetyLockEnabled": false
}
```

Sau demo, nên đưa lại cấu hình mặc định ở trên.
