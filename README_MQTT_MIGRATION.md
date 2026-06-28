# MQTT Migration Notes

Phiên bản này đã chuyển luồng thiết bị IoT sang MQTT theo kiến trúc:

```text
Arduino UNO R4 WiFi -> MQTT Broker -> Spring Boot Backend -> PostgreSQL/WebSocket -> Frontend
Frontend/Web/App -> HTTP REST API -> Spring Boot Backend
```

## IP đã cấu hình

- MQTT broker host: `175.16.16.108`
- Backend HTTP fallback host trong Arduino: `175.16.16.108`
- MQTT port: `1883`
- Backend port: `8080`

## Topic đang dùng

```text
shrimp-iot/devices/device_01/telemetry
shrimp-iot/devices/device_01/commands
shrimp-iot/devices/device_01/commands/ack
shrimp-iot/devices/device_01/status
```

Backend subscribe pattern:

```text
shrimp-iot/devices/+/telemetry
shrimp-iot/devices/+/commands/ack
shrimp-iot/devices/+/status
```

## File đã chỉnh chính

Backend:

```text
pom.xml
src/main/resources/application.yml
src/main/java/com/example/shrimpiot/config/MqttProperties.java
src/main/java/com/example/shrimpiot/config/MqttConfig.java
src/main/java/com/example/shrimpiot/service/MqttInboundService.java
src/main/java/com/example/shrimpiot/service/MqttCommandPublisher.java
src/main/java/com/example/shrimpiot/service/MqttCommandRetryService.java
src/main/java/com/example/shrimpiot/service/SensorReadingService.java
src/main/java/com/example/shrimpiot/service/CommandService.java
src/main/java/com/example/shrimpiot/repository/DeviceCommandRepository.java
docker-compose.yml
mosquitto/config/mosquitto.conf
.env.example
```

Arduino:

```text
arduino/shrimp_iot_uno_r4_complete/shrimp_iot_uno_r4_complete.ino
```

## Cách chạy local

1. Chạy database và MQTT broker:

```bash
docker compose up -d
```

2. Chạy backend:

```bash
mvn spring-boot:run
```

3. Nạp Arduino sketch:

```text
arduino/shrimp_iot_uno_r4_complete/shrimp_iot_uno_r4_complete.ino
```

Cần cài thêm thư viện Arduino:

```text
PubSubClient
```

Các thư viện cũ vẫn giữ:

```text
WiFiS3
OneWire
DallasTemperature
DFRobot_PH
DFRobot_EC10
ArduinoJson
```

## Test nhanh bằng mosquitto client

Telemetry giả lập:

```bash
mosquitto_pub -h 175.16.16.108 -p 1883 \
  -t shrimp-iot/devices/device_01/telemetry \
  -m "{\"deviceId\":\"device_01\",\"temperature\":28.5,\"ph\":7.4,\"ecValue\":1.2,\"salinity\":12.5,\"doValue\":5.8}"
```

Nghe lệnh từ backend:

```bash
mosquitto_sub -h 175.16.16.108 -p 1883 \
  -t shrimp-iot/devices/device_01/commands
```

Giả lập ACK:

```bash
mosquitto_pub -h 175.16.16.108 -p 1883 \
  -t shrimp-iot/devices/device_01/commands/ack \
  -m "{\"id\":1,\"success\":true,\"message\":\"Relay 3 turned ON\"}"
```

## Ghi chú bảo mật

Bản hiện tại để `allow_anonymous true` trong `mosquitto.conf` để dễ demo. Khi triển khai thật, cần đổi sang:

```text
allow_anonymous false
password_file /mosquitto/config/passwordfile
acl_file /mosquitto/config/aclfile
```

Sau đó tạo username/password và ACL riêng cho từng thiết bị.
