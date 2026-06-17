# Kịch bản demo 10 phút

## Mục tiêu demo

Chứng minh hệ thống có đầy đủ 4 luồng chính:

```text
1. Giám sát dữ liệu cảm biến.
2. Cảnh báo vượt ngưỡng.
3. Điều khiển relay/máy bơm.
4. Quản trị người dùng, phân quyền và chatbot.
```

## 0:00–1:00 — Giới thiệu kiến trúc

Nói:

> Hệ thống gồm Arduino UNO R4 WiFi đọc các cảm biến môi trường nước, truyền dữ liệu lên backend Spring Boot bằng HTTP REST API. Backend lưu dữ liệu vào PostgreSQL, phân tích ngưỡng, tạo cảnh báo và cung cấp API cho web/app. Người dùng có thể theo dõi dữ liệu, nhận cảnh báo, điều khiển relay và sử dụng chatbot hỗ trợ vận hành.

Mở sơ đồ kiến trúc hoặc dashboard.

## 1:00–2:00 — Login và phân quyền

Thao tác:

```text
Đăng nhập admin / admin123
Mở trang quản lý user
Hiển thị USER / TECHNICIAN / trạng thái active / approvalStatus
```

Nói:

> Hệ thống có phân quyền ADMIN, USER và TECHNICIAN. Người dùng đăng ký xong chưa dùng được ngay mà phải chờ admin duyệt, giúp tránh truy cập trái phép vào hệ thống điều khiển thiết bị.

## 2:00–3:00 — Dữ liệu cảm biến

Thao tác:

```text
Mở dashboard/device detail
Hiển thị nhiệt độ, pH, EC, độ mặn, DO
```

Nếu có Arduino đang chạy, cho Arduino gửi dữ liệu thật. Nếu không, dùng Postman gửi body demo.

Nói:

> Arduino gửi dữ liệu định kỳ lên backend. Backend lưu dữ liệu, phân tích trạng thái và cung cấp dữ liệu mới nhất cho web/app.

## 3:00–4:00 — Cảnh báo vượt ngưỡng

Thao tác:

```text
Gửi dữ liệu pH thấp + DO thấp bằng Postman hoặc Arduino
Mở tab cảnh báo
```

Nói:

> Khi pH hoặc DO vượt ngưỡng, backend tự tạo cảnh báo. Người vận hành có thể xem cảnh báo đang mở và đánh dấu đã xử lý sau khi can thiệp.

## 4:00–5:30 — Điều khiển relay/máy bơm

Thao tác:

```text
Bật relay 1 / máy bơm
Quan sát Serial Monitor Arduino nhận command
Quan sát relay/máy bơm đổi trạng thái
Tắt relay 1
Mở command history
```

Nói:

> Web/app không điều khiển trực tiếp Arduino. Người dùng gửi lệnh lên backend, Arduino định kỳ lấy lệnh pending, thực hiện bật/tắt relay và gửi ACK về backend. Nhờ đó hệ thống lưu được lịch sử điều khiển và trạng thái thực hiện.

## 5:30–6:30 — Quản lý ao và thiết bị

Thao tác:

```text
Mở danh sách ao
Mở danh sách thiết bị
Hiển thị device_01 gắn với Ao tôm thẻ 01
```

Nói:

> Admin có thể quản lý hồ nuôi, thiết bị và gán quyền cho từng người dùng theo ao hoặc thiết bị. Điều này giúp hệ thống phù hợp với mô hình nhiều ao và nhiều nhân sự vận hành.

## 6:30–7:30 — Chatbot

Thao tác:

```text
Hỏi: pH thấp thì xử lý như thế nào?
Hỏi: Ao hiện tại thế nào?
Hỏi: Máy bơm đang bật hay tắt?
```

Nói:

> Chatbot có hai pha: pha 1 trả lời kiến thức cơ bản về nuôi tôm và chỉ số môi trường; pha 2 đọc dữ liệu thật trong backend như cảm biến mới nhất, cảnh báo, relay và trạng thái thiết bị.

## 7:30–8:30 — Báo cáo và lịch sử

Thao tác:

```text
Mở history cảm biến
Mở command history
Mở report summary/export CSV nếu frontend có
```

Nói:

> Hệ thống lưu dữ liệu lịch sử để phục vụ báo cáo, phân tích và truy vết. Ngoài dữ liệu cảm biến, hệ thống còn lưu cảnh báo, lệnh điều khiển và lịch sử chat.

## 8:30–9:30 — Đăng ký và admin duyệt

Thao tác:

```text
Tạo user mới qua form register
Admin mở danh sách pending
Duyệt user thành USER hoặc TECHNICIAN
```

Nói:

> Quy trình này giúp kiểm soát người dùng trước khi họ được phép xem dữ liệu hoặc điều khiển thiết bị.

## 9:30–10:00 — Kết luận

Nói:

> Hệ thống đã hoàn thiện các chức năng lõi: giám sát, lưu trữ, cảnh báo, điều khiển, phân quyền, chatbot và báo cáo. Trong hướng phát triển, hệ thống có thể mở rộng sang LoRa/4G, SMS/email thật, WebSocket realtime và đánh giá sai số cảm biến bằng RMSE.
