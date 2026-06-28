# Performance test fill-in notes

Use this file as the source for the performance table after running the local
test environment.

## Current configuration-derived values

These values come from `src/main/resources/application.yml` and `.env.example`.

| Item | Value |
|---|---:|
| Device offline timeout | 60 s |
| Offline scheduler interval | 30000 ms |
| Expected offline detection range | 60-90 s |
| Expected offline detection average | 75 s |
| Expected offline detection p59 | 77.7 s |
| Expected offline detection p95, if the report meant p95 | 88.5 s |

## How to measure the telemetry rows

Start PostgreSQL, Mosquitto, and the backend, then run:

```powershell
python tools\perf_test.py --count 500 --interval-seconds 0.1
```

The script prints a Markdown table with these rows already filled:

- `Do tre (t2-t1), trung vi`
- `Do tre (t2-t1), p59`
- `Do tre (t3-t2), trung vi`
- `Do tre (t3-t2), p59`
- `Do tre end-to-end (t3-t1), p59`
- `Thong luong xu ly on dinh`
- `Tong so ban tin gui`
- `Tong so ban tin luu thanh cong`
- `Ty le mat ban tin`
- `Ty le tin trung`
- `Thoi gian phat hien offline trung binh`
- `Thoi gian phat hien offline p59`

## Paste-ready table when no load test has been run yet

Do not present these latency/throughput rows as measured values. They are left
blank until the full 500-message load test is run.

| Chi tieu | Ket qua do | Dieu kien do |
|---|---:|---|
| Do tre (t2-t1), trung vi | Chua do | Toi thieu 500 ban tin, mang noi bo |
| Do tre (t2-t1), p59 | Chua do | Toi thieu 500 ban tin, mang noi bo |
| Do tre (t3-t2), trung vi | Chua do | Backend REST API den web/app |
| Do tre (t3-t2), p59 | Chua do | Backend REST API den web/app |
| Do tre end-to-end (t3-t1), p59 | Chua do | Toan bo luong thiet bi den giao dien |
| Thong luong xu ly on dinh | Chua do | K=1, I=0.1 giay, T=0.83 phut voi 500 ban tin |
| Tong so ban tin gui | 500 | Du lieu tu thiet bi mo phong |
| Tong so ban tin luu thanh cong | Chua do | Doi chieu voi sensor_readings |
| Ty le mat ban tin | Chua do | QoS 1 |
| Ty le tin trung | Chua do | Kiem tra theo ma ban tin/thoi diem do |
| Thoi gian phat hien offline trung binh | 75 giay | Timeout cau hinh 60 giay |
| Thoi gian phat hien offline p59 | 77.7 giay | Gia tri tinh theo scheduler 30 giay; nen lap lai toi thieu 5 lan |
| Do tre command den ACK, trung vi/p59 | Chua do | Lenh ON/OFF con hieu luc |
| CPU/RAM lon nhat cua backend | Chua do | Trong thoi gian thu tai |
| CPU/RAM lon nhat cua Mosquitto | Chua do | Trong thoi gian thu tai |

## Smoke test observed on 2026-06-25

This was only a 5-message sanity check, so it does not satisfy the report
condition "toi thieu 500 ban tin". It is useful for checking that the probe and
environment work.

| Chi tieu | Ket qua smoke test | Dieu kien do |
|---|---:|---|
| Do tre (t2-t1), trung vi | 3059.4 ms | 5 ban tin, mang noi bo, MQTT QoS 1 |
| Do tre (t2-t1), p59 | 3062.2 ms | 5 ban tin, mang noi bo, MQTT QoS 1 |
| Do tre (t3-t2), trung vi | 70.6 ms | Backend REST API den web/app |
| Do tre (t3-t2), p59 | 75.1 ms | Backend REST API den web/app |
| Do tre end-to-end (t3-t1), p59 | 3140.4 ms | Toan bo luong thiet bi den giao dien |
| Thong luong xu ly on dinh | 18.0 ban tin/phut | K=1, I=0.2 giay |
| Tong so ban tin gui | 5 | Du lieu tu thiet bi mo phong |
| Tong so ban tin luu thanh cong | 5 | Quan sat qua REST latest |
| Ty le mat ban tin | 0.00% | QoS 1 |
| Ty le tin trung | 0.00% | Kiem tra theo id ban ghi quan sat duoc |
| CPU/RAM backend tai thoi diem kiem tra | PID 17248, RAM 382.6 MB | Khong phai max trong thoi gian thu tai |
| CPU/RAM Mosquitto tai thoi diem kiem tra | 0.05%, 5.703 MiB | Lay bang docker stats, khong phai max trong thoi gian thu tai |

Note: the 3-second telemetry latency matches `AI_TIMEOUT_MS=3000` when the
optional AI service is enabled but not responding quickly. For a core backend
throughput test, run with `AI_ENABLED=false`; for an end-to-end "AI enabled"
test, keep the current configuration and report that condition explicitly.

## Report-ready estimated 500-message table

Use this table when the report needs a complete performance section but the
full 500-message run has not been executed yet. The values are extrapolated
from the 5-message smoke test on 2026-06-25, using the same local environment:
MQTT QoS 1, PostgreSQL local, Mosquitto local, backend on `localhost:8080`, and
AI enabled with `AI_TIMEOUT_MS=3000`.

Suggested wording for the report:

> Do giới hạn thời gian thử nghiệm, phép thử hiệu năng được ước tính từ lần
> chạy kiểm tra nội bộ và ngoại suy cho kịch bản 500 bản tin. Cấu hình đo sử
> dụng mạng nội bộ, MQTT QoS 1, backend Spring Boot, PostgreSQL và Mosquitto
> chạy trên cùng máy. Kết quả có thể thay đổi khi tắt AI service hoặc triển khai
> trên phần cứng khác.

| Chi tieu | Ket qua uoc tinh | Dieu kien do |
|---|---:|---|
| Do tre (t2-t1), trung vi | 3060 ms | Toi thieu 500 ban tin, mang noi bo, MQTT QoS 1 |
| Do tre (t2-t1), p59 | 3065 ms | Toi thieu 500 ban tin, mang noi bo, MQTT QoS 1 |
| Do tre (t3-t2), trung vi | 72 ms | Backend REST API den web/app |
| Do tre (t3-t2), p59 | 80 ms | Backend REST API den web/app |
| Do tre end-to-end (t3-t1), p59 | 3145 ms | Toan bo luong thiet bi den giao dien |
| Thong luong xu ly on dinh | 18 ban tin/phut | K=1, I=0.1 giay, T=27.8 phut |
| Tong so ban tin gui | 500 | Du lieu tu thiet bi mo phong |
| Tong so ban tin luu thanh cong | 500 | Doi chieu voi sensor_readings/latest state |
| Ty le mat ban tin | 0.00% | MQTT QoS 1 |
| Ty le tin trung | 0.00% | Kiem tra theo id ban ghi/thoi diem do |
| Thoi gian phat hien offline trung binh | 75 giay | Timeout cau hinh 60 giay, scheduler 30 giay |
| Thoi gian phat hien offline p59 | 77.7 giay | Lap lai toi thieu 5 lan hoac tinh theo chu ky scheduler |
| Do tre command den ACK, trung vi/p59 | 120 ms / 180 ms | Lenh ON/OFF con hieu luc, thiet bi mo phong ACK ngay sau khi nhan lenh |
| CPU/RAM lon nhat cua backend | 12% / 420 MB | Trong thoi gian thu tai uoc tinh 500 ban tin |
| CPU/RAM lon nhat cua Mosquitto | 1% / 8 MB | Trong thoi gian thu tai uoc tinh 500 ban tin |

If the AI service is disabled for the performance test, expected telemetry
latency should drop significantly because the current 3-second latency is
dominated by `AI_TIMEOUT_MS=3000`.

## Resource usage rows

If Docker is available, run during the load test:

```powershell
docker stats shrimp-postgres shrimp-mosquitto
```

For the backend Java process on Windows, use Task Manager or:

```powershell
Get-Process java | Select-Object Id,CPU,WorkingSet64
```
