# Backend đã bỏ ORP

Bản backend này đã loại bỏ ORP khỏi API, model, chatbot, report và cấu hình threshold.

## Sensor payload hiện tại

```json
{
  "deviceId": "device_01",
  "temperature": 30.5,
  "ph": 7.2,
  "ecValue": 1.1,
  "salinity": 12.5,
  "doValue": 5.8
}
```

## Các phần đã bỏ

- `orpValue` trong `SensorReadingRequest`
- `orpValue` trong `SensorReadingResponse`
- `orpValue` trong entity `SensorReading`
- Cảnh báo `ORP_LOW`, `ORP_HIGH`
- Threshold `threshold.orp`
- Chatbot trả lời về ORP
- ORP trong báo cáo summary và CSV
- Sensor ORP trong dữ liệu khởi tạo

## Lưu ý database

Nếu database PostgreSQL trước đó đã từng chạy bản có ORP, cột `orp_value` trong bảng `sensor_readings` có thể vẫn còn do `ddl-auto:update` không tự xóa cột cũ. Điều này không ảnh hưởng backend, vì backend mới không đọc/ghi cột này nữa.

Nếu muốn dọn sạch database, có thể chạy SQL sau trong pgAdmin/psql:

```sql
ALTER TABLE sensor_readings DROP COLUMN IF EXISTS orp_value;
DELETE FROM alerts WHERE alert_type IN ('ORP_LOW', 'ORP_HIGH');
DELETE FROM device_sensors WHERE sensor_type = 'ORP';
```

Chỉ chạy SQL này nếu chắc chắn không cần dữ liệu ORP cũ.
