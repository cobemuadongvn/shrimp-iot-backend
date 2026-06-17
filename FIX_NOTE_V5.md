# Fixed v5

Sửa hiện tượng relay tự bật lại sau khi đã tắt:

1. Backend: `MqttCommandRetryService` không resend các command đã ở trạng thái `SENT`.
   - Trước đó nếu ACK bị trễ/không xử lý, lệnh ON cũ có thể được gửi lại sau lệnh OFF.

2. Arduino: tắt failsafe trong giai đoạn test/manual control bằng `FAILSAFE_ENABLED = false`.
   - Trước đó failsafe có thể tự bật relay 2/3 khi server offline + DO thấp hoặc nhiệt độ cao.

Sau khi ổn định demo, nếu muốn bật failsafe thật thì cần cấu hình lại theo đúng thiết bị đấu vào từng relay.
