# Test API bằng curl

## Health check

```bash
curl http://localhost:8080/api/health
```

## Gửi dữ liệu cảm biến

```bash
curl -X POST http://localhost:8080/api/readings \
  -H "Content-Type: application/json" \
  -H "X-API-Key: MY_SECRET_KEY" \
  -d '{
    "deviceId": "device_01",
    "temperature": 28.5,
    "ph": 7.2,
    "ecValue": 1.8,
    "salinity": 12.8,
    "doValue": 5.6
  }'
```

## Lấy dữ liệu mới nhất

```bash
curl "http://localhost:8080/api/readings/latest?deviceId=device_01"
```

## Lấy lịch sử

```bash
curl "http://localhost:8080/api/readings/history?deviceId=device_01&limit=20"
```

## Tạo lệnh relay

```bash
curl -X POST http://localhost:8080/api/commands \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "device_01",
    "relayNo": 1,
    "action": "ON"
  }'
```

## Arduino lấy lệnh đang chờ

```bash
curl "http://localhost:8080/api/commands/pending?deviceId=device_01" \
  -H "X-API-Key: MY_SECRET_KEY"
```
