# OpenAI Chat Assistant Integration

## Muc dich

`/api/chat/message` van la endpoint duy nhat cho AI Assistant. Backend lay du lieu ao nuoi tu he thong noi bo, loc thanh context gon, sau do moi goi OpenAI neu cau hinh duoc bat.

Neu OpenAI bi tat, thieu key, timeout hoac loi API, backend tu dong tra ve cau tra loi rule-based cu. Luong canh bao, dieu khien relay va AI service phan loai chat luong nuoc khong bi thay doi.

## Cau hinh backend

Dat trong `.env.local` hoac bien moi truong:

```env
OPENAI_API_KEY=...
OPENAI_CHAT_ENABLED=true
OPENAI_CHAT_MODEL=gpt-5.4-mini
OPENAI_CHAT_TIMEOUT_MS=8000
OPENAI_CHAT_MAX_OUTPUT_TOKENS=700
```

Trong source, `OPENAI_CHAT_ENABLED` mac dinh la `false` de giu hanh vi cu neu chua chu dong bat.

## Web/app goi endpoint

Frontend dang nhap de lay JWT, sau do goi:

```http
POST /api/chat/message
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "sessionId": null,
  "deviceId": "device_01",
  "message": "Ao hien tai co on khong?"
}
```

Response nam trong `data`:

```json
{
  "data": {
    "sessionId": 1,
    "intent": "LATEST_READING",
    "userMessage": {
      "role": "USER",
      "content": "Ao hien tai co on khong?"
    },
    "botMessage": {
      "role": "ASSISTANT",
      "content": "..."
    }
  }
}
```

De tiep tuc cung mot cuoc tro chuyen, gui lai `sessionId` da nhan duoc. De bat dau moi, gui `sessionId: null`.

## Du lieu gui sang OpenAI

Backend chi gui du lieu van hanh can thiet:

- chi so moi nhat: nhiet do, pH, EC, do man, DO, finalStatus, recommendedAction
- canh bao dang mo: loai, muc do, noi dung, thoi gian
- relay: so relay, ten, loai, trang thai, khoa hay khong
- thiet bi: deviceId, ten, trang thai ket noi, ao, vi tri lap dat

Backend khong gui mat khau, API key, token dang nhap, email hay so dien thoai nguoi dung.

