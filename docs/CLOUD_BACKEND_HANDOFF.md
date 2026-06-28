# Cloud Backend Handoff

Tai lieu nay dung de ban giao nhanh cho nguoi lam web/app khi backend da chay tren cloud.

## Trang thai hien tai

- Backend cloud: da deploy thanh cong tren Render.
- Database cloud: Supabase PostgreSQL da ket noi OK.
- MQTT cloud: backend Render dang nhan telemetry that tu `device_01`.
- API login: OK.
- API latest reading: OK.
- AI service: da deploy, nhung backend dang de `AI_ENABLED=false`.

## Base URL

```text
https://shrimp-iot-backend.onrender.com/api
```

## Luu y quan trong

- Render backend hien dang o plan `Free`.
- Service co the sleep khi khong co request trong mot thoi gian.
- Lan goi dau sau khi sleep co the cham hoac can cho wake up.
- Neu muc tieu la IoT 24/7 on dinh, nen doi sang plan tra phi sau.
- Da them GitHub Actions keep-alive workflow ping `/api/health/ready` moi 10 phut de giam kha nang backend bi ngu trong giai doan demo.
- Keep-alive nay chi la workaround cho demo, khong nen xem la giai phap production.

## Auth

Tat ca API danh cho web/app deu dung:

```http
Authorization: Bearer <token>
```

Web/app khong duoc dung:

```http
X-API-Key
```

`X-API-Key` chi dung cho device / Arduino.

## API can dung ngay

### 1. Login

```http
POST /auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "<username>",
  "password": "<password>"
}
```

Response thanh cong se co:

- `data.token`
- `data.tokenType`
- `data.user`

### 2. Kiem tra user hien tai

```http
GET /auth/me
Authorization: Bearer <token>
```

### 3. Lay du lieu moi nhat cua device

```http
GET /readings/latest?deviceId=device_01
Authorization: Bearer <token>
```

Da test thanh cong tren cloud voi:

- `deviceId = device_01`

### 4. Lay lich su du lieu

```http
GET /readings/history?deviceId=device_01&limit=50
Authorization: Bearer <token>
```

### 5. Dashboard summary

```http
GET /dashboard/summary?deviceId=device_01
Authorization: Bearer <token>
```

### 6. Trang thai moi nhat cua device

```http
GET /devices/device_01/latest-state
Authorization: Bearer <token>
```

## Health check

Dung cho backend/devops, khong phai man hinh nguoi dung:

```http
GET /health
GET /health/live
GET /health/ready
```

Da test thanh cong:

- `/health` -> `UP`
- `/health/ready` -> `UP`
- `database` -> `UP`

## Trang thai AI

Hien tai backend cloud dang de:

```text
AI_ENABLED=false
OPENAI_CHAT_ENABLED=false
```

Nen:

- truong `aiMessage` co the bao AI disabled
- `finalStatus` hien dang dua tren rule-based layer

Dieu nay la chu dong, khong phai loi.

## Luong du lieu da xac nhan

```text
Chip -> HiveMQ Cloud -> Backend Render -> Supabase -> API cloud
```

Da xac nhan bang 3 dau hieu:

- Render logs co dong `MQTT telemetry saved and published`
- `/api/health/ready` tra `database: UP`
- `GET /api/readings/latest?deviceId=device_01` tra du lieu that

## Khong nen lam o web/app

Khong goi cac API danh rieng cho device:

```text
POST /readings
GET /commands/pending
POST /commands/{id}/ack
```

Web/app chi nen di qua flow auth Bearer token.

## Can ban giao cho doi web/app

- Base URL cloud
- Mot tai khoan test hop le
- Device test hien tai: `device_01`
- Bearer auth flow
- Luu y Render Free co the wake up cham

## Ket luan

Neu pham vi la backend + database + MQTT cloud + API cloud, thi phan cloud backend da san sang de doi web/app tich hop.
