# Shrimp IoT Streamlit Dashboard

Giao diện Streamlit thân thiện cho người dùng theo dõi ao tôm từ backend hiện tại.

## Run

```powershell
python -m streamlit run streamlit-dashboard/app.py --server.port 8501 --runner.magicEnabled false
```

Default URLs:

```text
Backend API: http://127.0.0.1:8080/api
AI service:  http://127.0.0.1:8001
Device ID:   device_01
```

Đăng nhập bằng tài khoản backend có sẵn, ví dụ `admin / admin123`.

Các màn hình chính:

- Tổng quan tình trạng ao và chỉ số mới nhất.
- Biểu đồ chất lượng nước theo thời gian.
- Cảnh báo và thông báo hệ thống.
- Thông tin thiết bị, relay và lịch sử điều khiển.

Bản này chỉ đọc dữ liệu, chưa có nút bật/tắt relay để tránh thao tác nhầm lên thiết bị thật.
