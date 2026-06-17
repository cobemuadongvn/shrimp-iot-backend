# Đoạn viết báo cáo — Đánh giá chức năng hệ thống

## Đánh giá các chức năng đã hoàn thiện

Hệ thống đã hoàn thiện nhóm chức năng lõi phục vụ giám sát và điều khiển môi trường ao nuôi thủy sản. Về quản trị người dùng, hệ thống hỗ trợ đăng ký tài khoản, đăng nhập, phân quyền theo ba vai trò ADMIN, USER và TECHNICIAN. Tài khoản sau khi đăng ký không được sử dụng ngay mà phải chờ quản trị viên phê duyệt, giúp tăng tính an toàn cho hệ thống có khả năng điều khiển thiết bị vật lý. Quản trị viên có thể duyệt, từ chối, khóa, mở khóa tài khoản và gán người dùng vào ao hoặc thiết bị cụ thể.

Về giám sát môi trường, hệ thống cho phép Arduino UNO R4 WiFi gửi dữ liệu cảm biến lên backend thông qua giao thức HTTP REST API. Các thông số được giám sát gồm nhiệt độ, pH, EC/độ mặn và oxy hòa tan DO. Backend lưu dữ liệu vào PostgreSQL, phân tích ngưỡng và tạo cảnh báo khi chỉ số môi trường vượt phạm vi an toàn. Web/App có thể lấy dữ liệu mới nhất, lịch sử cảm biến và danh sách cảnh báo để hiển thị cho người dùng.

Về điều khiển thiết bị, hệ thống hỗ trợ điều khiển relay theo mô hình trung gian qua backend. Người dùng gửi lệnh bật/tắt relay từ web/app, backend lưu lệnh vào hàng đợi, Arduino định kỳ lấy lệnh đang chờ, thực thi điều khiển và gửi phản hồi ACK về backend. Cách thiết kế này giúp lưu lại lịch sử điều khiển, trạng thái thực hiện và hỗ trợ truy vết thao tác vận hành.

Ngoài các chức năng giám sát và điều khiển, hệ thống còn tích hợp chatbot hỗ trợ người dùng. Chatbot có khả năng trả lời kiến thức cơ bản về pH, nhiệt độ, độ mặn, oxy hòa tan và hướng dẫn xử lý cảnh báo. Đồng thời, chatbot có thể đọc dữ liệu hệ thống để trả lời các câu hỏi như trạng thái ao hiện tại, dữ liệu cảm biến mới nhất, cảnh báo chưa xử lý, trạng thái relay và trạng thái thiết bị. Lịch sử hội thoại được lưu lại để phục vụ tra cứu.

Hệ thống cũng hỗ trợ báo cáo và truy vết dữ liệu vận hành thông qua các API lịch sử cảm biến, cảnh báo, lệnh điều khiển, thông báo và audit log. Đây là cơ sở để xây dựng các báo cáo ngày/tuần/tháng và phân tích xu hướng môi trường ao nuôi trong các phiên bản tiếp theo.

## Đánh giá mức độ đáp ứng

Nhìn chung, hệ thống đã đáp ứng được các yêu cầu chính của một hệ thống IoT giám sát môi trường ao nuôi thủy sản: thu thập dữ liệu cảm biến, truyền dữ liệu về backend, lưu trữ, cảnh báo vượt ngưỡng, điều khiển relay, phân quyền người dùng, chatbot hỗ trợ vận hành và báo cáo dữ liệu. Các chức năng này giúp hệ thống không chỉ dừng ở mức hiển thị dữ liệu cảm biến mà còn hỗ trợ quy trình vận hành thực tế gồm quản trị, giám sát, cảnh báo, can thiệp và truy vết.

## Hạn chế hiện tại

Một số hạn chế còn tồn tại gồm: hệ thống hiện sử dụng WiFi nên phù hợp với mô hình thử nghiệm hoặc khu vực có hạ tầng mạng ổn định; chưa triển khai LoRa/4G cho các ao ở xa; SMS/email thật cần cấu hình thêm nhà cung cấp dịch vụ; hệ thống cần đánh giá sai số cảm biến bằng thiết bị đối chứng để tăng độ tin cậy khoa học; và cần triển khai server public/HTTPS nếu muốn sử dụng ngoài mạng LAN.

## Hướng cải thiện tiếp theo

Các hướng cải thiện gồm: kiểm chứng sai số cảm biến bằng RMSE, triển khai WebSocket hoặc Server-Sent Events để cập nhật realtime, tích hợp email/SMS/Zalo thật, triển khai backend lên VPS/domain, bổ sung cơ chế backup dữ liệu, nâng cấp token/JWT/refresh token và mở rộng lớp truyền thông sang LoRa hoặc 4G cho triển khai thực địa trên diện rộng.
