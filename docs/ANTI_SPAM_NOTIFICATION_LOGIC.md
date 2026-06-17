# Anti-Spam Notification Logic

## Mục tiêu

Khi cảm biến gửi dữ liệu mỗi 1 phút và một thông số vẫn xấu, hệ thống không được gửi tin nhắn liên tục làm spam điện thoại người dùng.

## Bảng quản lý nhật ký gửi cảnh báo

Backend sử dụng bảng `notification_logs` để lưu mọi lần gửi hoặc bị chặn gửi thông báo.

Các cột chính:

| Cột | Ý nghĩa |
|---|---|
| `device_id` | Thiết bị/ao phát sinh cảnh báo |
| `event_key` | Khóa gom nhóm chống spam, dạng `ALERT:{alertId}:{alertType}` |
| `alert_type` | Loại cảnh báo: `PH_LOW`, `DO_LOW`, `TEMP_HIGH`, ... |
| `severity` | Mức độ: `WARNING`, `DANGER` |
| `channel` | Kênh thông báo: `APP`, `SMS`, `EMAIL`, `SYSTEM` |
| `recipient` | Người nhận hoặc `assigned-users` |
| `message` | Nội dung cảnh báo |
| `status` | `CREATED`, `SENT`, `FAILED`, `SUPPRESSED_COOLDOWN`, ... |
| `suppressed` | `true` nếu bị chặn bởi anti-spam |
| `suppression_reason` | Lý do bị chặn |
| `cooldown_until` | Thời điểm hết chặn gửi lại |
| `created_at` | Thời điểm tạo log |

## Thuật toán

1. Khi một chỉ số vượt ngưỡng QCVN, `AlertService.openAlertIfMissing()` kiểm tra xem alert cùng `deviceId + alertType` đã đang `OPEN` chưa.
2. Nếu chưa có alert `OPEN`:
   - Tạo alert mới.
   - Gửi thông báo qua `NotificationService`.
   - Ghi log `APP/SMS/EMAIL` vào `notification_logs`.
3. Nếu đã có alert `OPEN`:
   - Không tạo alert mới.
   - Kiểm tra log gửi gần nhất theo `event_key`.
   - Nếu chưa qua `notification.cooldown-minutes`, chỉ ghi log `SUPPRESSED_COOLDOWN`, không gửi lại.
   - Nếu đã qua cooldown, gửi lại một thông báo nhắc và ghi log mới.
4. Khi thông số trở lại bình thường, `AlertService.autoResolveIfOpen()` chuyển alert sang `RESOLVED`.
5. Nếu sau đó thông số xấu trở lại, hệ thống tạo alert mới với `alertId` mới, nên được gửi thông báo ngay, không bị chặn bởi cooldown cũ.

## Cấu hình

```yaml
notification:
  anti-spam-enabled: ${NOTIFICATION_ANTI_SPAM_ENABLED:true}
  cooldown-minutes: ${NOTIFICATION_COOLDOWN_MINUTES:30}
```

`.env`:

```env
NOTIFICATION_ANTI_SPAM_ENABLED=true
NOTIFICATION_COOLDOWN_MINUTES=30
```

## Cách trả lời hội đồng

Nếu hội đồng hỏi: “Nếu cảm biến gửi dữ liệu 1 phút/lần và pH vẫn thấp, hệ thống có nhắn tin 1 phút/lần không?”

Trả lời:

> Không. Backend có cơ chế anti-spam dựa trên trạng thái alert và bảng `notification_logs`. Khi pH thấp lần đầu, hệ thống tạo một alert `PH_LOW`, gửi thông báo và ghi log. Các lần đo tiếp theo nếu pH vẫn thấp, alert đó vẫn đang `OPEN`, nên hệ thống không gửi lại ngay mà chỉ ghi một log `SUPPRESSED_COOLDOWN`. Sau 30 phút mới có thể gửi nhắc lại. Nếu pH trở lại bình thường, alert được chuyển sang `RESOLVED`; nếu sau đó pH thấp lại, hệ thống xem đây là một sự kiện mới và gửi thông báo ngay.
