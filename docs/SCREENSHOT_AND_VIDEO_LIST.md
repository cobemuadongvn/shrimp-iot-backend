# Danh sách ảnh/video nên chụp cho báo cáo và slide

## 1. Ảnh hệ thống phần cứng

```text
- Arduino UNO R4 WiFi gắn cảm biến.
- Cảm biến pH, DO, DS18B20, EC/độ mặn.
- Module relay và máy bơm/sục oxy.
- Toàn cảnh mô hình ao/bể demo.
```

## 2. Ảnh Serial Monitor Arduino

Cần chụp các log:

```text
- WiFi connected.
- POST /api/readings success.
- Checking pending commands.
- Execute command relay=1 action=ON.
- Sending ACK success.
```

## 3. Ảnh Postman

```text
- Login admin thành công.
- POST /api/readings thành công.
- GET /api/readings/latest có dữ liệu.
- POST /api/commands tạo lệnh ON.
- GET /api/commands/history thấy ACK.
- POST /api/chat/message trả botMessage.
```

## 4. Ảnh Web/App

```text
- Màn hình login.
- Dashboard cảm biến.
- Biểu đồ lịch sử cảm biến.
- Danh sách cảnh báo.
- Điều khiển relay.
- Lịch sử điều khiển.
- Quản lý user.
- User pending và màn hình duyệt.
- Quản lý ao.
- Quản lý thiết bị.
- Chatbot.
- Báo cáo/export nếu có.
```

## 5. Ảnh Database/PostgreSQL

```text
- Bảng sensor_readings có dữ liệu.
- Bảng alerts có cảnh báo.
- Bảng device_commands có lệnh ON/OFF.
- Bảng user_accounts có role/active/approvalStatus.
- Bảng chat_messages có lịch sử chat.
```

## 6. Video demo ngắn

Nên quay 1 video 2–3 phút:

```text
Web bấm bật máy bơm
→ Arduino Serial Monitor nhận lệnh
→ relay/máy bơm bật
→ backend command history chuyển ACK
→ web hiển thị trạng thái mới
```
