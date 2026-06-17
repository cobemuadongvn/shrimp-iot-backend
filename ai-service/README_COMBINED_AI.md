# Shrimp IoT AI Service - Combined Models

Bản này đã kết hợp 3 mô hình:

1. Isolation Forest: phát hiện bất thường `NORMAL / ANOMALY`
2. XGBoost: phân loại trạng thái `NORMAL / WARNING / DANGER`
3. Random Forest: phân loại trạng thái `NORMAL / WARNING / DANGER`

Input chung:

```json
{
  "temperature": 28.0,
  "ph": 7.5,
  "ec_value": 28.0,
  "salinity": 19.0,
  "do_value": 5.5
}
```

Endpoint:

```http
POST /predict
```

Response có các field quan trọng:

```json
{
  "ruleStatus": "NORMAL",
  "anomalyStatus": "NORMAL",
  "xgboostStatus": "WARNING",
  "randomForestStatus": "NORMAL",
  "aiStatus": "WARNING",
  "finalStatus": "WARNING"
}
```

Ý nghĩa:

- `ruleStatus`: kết quả ngưỡng QCVN.
- `anomalyStatus`: kết quả Isolation Forest.
- `xgboostStatus`: kết quả XGBoost.
- `randomForestStatus`: kết quả Random Forest.
- `aiStatus`: kết quả tổng hợp các model AI.
- `finalStatus`: kết quả tổng hợp rule-based + AI.

## Cách chạy

```powershell
cd ai-service

python -m venv .venv
.\.venv\Scripts\activate

pip install -r requirements.txt

uvicorn app:app --host 0.0.0.0 --port 8001
```

Nếu backend của bạn đang cấu hình AI ở port 5001 thì chạy:

```powershell
uvicorn app:app --host 0.0.0.0 --port 5001
```

## Test

```powershell
curl -X POST http://127.0.0.1:8001/predict ^
  -H "Content-Type: application/json" ^
  -d "{\"temperature\":28.0,\"ph\":7.5,\"ec_value\":28.0,\"salinity\":19.0,\"do_value\":5.5}"
```

## Backend cần gọi

Backend chỉ cần gọi:

```http
POST http://127.0.0.1:8001/predict
```

và lấy các field:

```text
aiStatus
finalStatus
randomForestStatus
xgboostStatus
anomalyStatus
```
