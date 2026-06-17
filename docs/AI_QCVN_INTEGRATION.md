# Tích hợp Rule-based QCVN và AI cho Shrimp IoT Backend

## 1. Rule-based threshold

Backend sử dụng QCVN 02-19:2014/BNNPTNT, Phụ lục 1, Bảng 1 làm ngưỡng mặc định cho môi trường ao nuôi tôm nước lợ:

| Thông số | Ngưỡng mặc định |
|---|---:|
| Nhiệt độ | 18 - 33 °C |
| pH | 7.0 - 9.0 |
| Độ mặn | 5 - 35 ‰ |
| Oxy hòa tan DO | >= 3.5 mg/L |

EC không được đưa vào rule-based threshold vì QCVN này không quy định ngưỡng EC trong Bảng 1. EC vẫn được lưu, hiển thị và dùng làm feature cho mô hình AI.

## 2. Mapping severity trong backend

QCVN chỉ quy định giá trị cho phép, không quy định trực tiếp NORMAL/WARNING/DANGER. Backend mã hóa vận hành như sau:

- Không có thông số vượt chuẩn: `NORMAL`
- Có 1 thông số vượt chuẩn, trừ DO: `WARNING`
- DO < 3.5 mg/L hoặc có từ 2 thông số vượt chuẩn: `DANGER`

## 3. AI service

AI service nằm trong thư mục:

```text
ai-service/
```

Service chạy bằng Python FastAPI, load các file:

```text
ai-service/models/isolation_forest_scaler.joblib
ai-service/models/isolation_forest_model.joblib
ai-service/models/xgboost_scaler.joblib
ai-service/models/xgboost_status_model.joblib
ai-service/models/status_label_encoder.joblib
```

Input model bắt buộc theo thứ tự:

```text
temperature, ph, ec_value, salinity, do_value
```

## 4. Cách chạy AI service

```bash
cd ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8001
```

Trên Linux/macOS:

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8001
```

Kiểm tra:

```bash
curl http://127.0.0.1:8001/health
```

Test predict:

```bash
curl -X POST http://127.0.0.1:8001/predict \
  -H "Content-Type: application/json" \
  -d '{"temperature":29.5,"ph":7.5,"ec_value":28.0,"salinity":18.0,"do_value":5.5}'
```

## 5. Bật AI trong Spring Boot

Mặc định AI tắt để backend vẫn chạy độc lập bằng rule-based QCVN.

Bật AI bằng biến môi trường:

```env
AI_ENABLED=true
AI_SERVICE_URL=http://127.0.0.1:8001/predict
AI_TIMEOUT_MS=1500
```

Nếu AI service lỗi hoặc timeout, backend tự fallback về `ruleStatus` và không làm hỏng luồng nhận dữ liệu cảm biến.

## 6. Response trả về frontend

`SensorReadingResponse` có thêm các field:

```json
{
  "ruleStatus": "NORMAL",
  "anomalyStatus": "NORMAL",
  "mlStatus": "NORMAL",
  "finalStatus": "NORMAL",
  "aiMessage": "Isolation Forest: NORMAL; XGBoost: NORMAL",
  "recommendedAction": "Tiếp tục giám sát định kỳ"
}
```

Trong đó:

- `ruleStatus`: kết quả rule-based theo QCVN.
- `anomalyStatus`: kết quả Isolation Forest, `NORMAL` hoặc `ANOMALY`.
- `mlStatus`: kết quả XGBoost, `NORMAL`, `WARNING`, hoặc `DANGER`.
- `finalStatus`: kết quả tổng hợp cuối cùng, cũng được gán vào field cũ `status` để giữ tương thích frontend.

## 7. Logic tổng hợp cuối cùng

```text
Nếu ruleStatus = DANGER hoặc mlStatus = DANGER -> finalStatus = DANGER
Nếu anomalyStatus = ANOMALY -> finalStatus = WARNING
Nếu ruleStatus = WARNING hoặc mlStatus = WARNING -> finalStatus = WARNING
Ngược lại -> finalStatus = NORMAL
```

Rule-based QCVN là lớp an toàn chính. AI là lớp hỗ trợ phát hiện bất thường và phân loại trạng thái, không thay thế hoàn toàn rule.
