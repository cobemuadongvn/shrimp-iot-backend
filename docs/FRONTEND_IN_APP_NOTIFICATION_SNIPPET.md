# Frontend snippet: In-app notification bell

Ví dụ dưới đây giả định frontend dùng React + axios.

## 1. API client

```js
import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const api = axios.create({
  baseURL: API_BASE_URL,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

## 2. Notification service

```js
export async function getInAppNotifications(deviceId, unreadOnly = false, limit = 50) {
  const res = await api.get("/api/notifications/in-app", {
    params: { deviceId, unreadOnly, limit },
  });
  return res.data.data;
}

export async function getUnreadNotificationCount(deviceId) {
  const res = await api.get("/api/notifications/in-app/unread-count", {
    params: { deviceId },
  });
  return res.data.data.unreadCount;
}

export async function markNotificationRead(id) {
  const res = await api.patch(`/api/notifications/in-app/${id}/read`);
  return res.data.data;
}

export async function markAllNotificationsRead(deviceId) {
  const res = await api.patch("/api/notifications/in-app/read-all", null, {
    params: { deviceId },
  });
  return res.data.data;
}
```

## 3. React component đơn giản

```jsx
import { useEffect, useState } from "react";
import {
  getInAppNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from "./notificationService";

export default function NotificationBell({ deviceId }) {
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [items, setItems] = useState([]);

  async function reload() {
    const [count, notifications] = await Promise.all([
      getUnreadNotificationCount(deviceId),
      getInAppNotifications(deviceId, false, 20),
    ]);
    setUnreadCount(count);
    setItems(notifications);
  }

  useEffect(() => {
    reload();
    const timer = setInterval(reload, 15000);
    return () => clearInterval(timer);
  }, [deviceId]);

  async function handleRead(item) {
    await markNotificationRead(item.id);
    await reload();
  }

  async function handleReadAll() {
    await markAllNotificationsRead(deviceId);
    await reload();
  }

  return (
    <div style={{ position: "relative" }}>
      <button onClick={() => setOpen(!open)}>
        🔔 {unreadCount > 0 && <span>{unreadCount}</span>}
      </button>

      {open && (
        <div style={{ position: "absolute", right: 0, width: 360, background: "white", border: "1px solid #ddd", padding: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <strong>Thông báo</strong>
            <button onClick={handleReadAll}>Đánh dấu tất cả đã đọc</button>
          </div>

          {items.length === 0 ? (
            <p>Chưa có thông báo</p>
          ) : (
            items.map((item) => (
              <div key={item.id} onClick={() => handleRead(item)} style={{ padding: 10, cursor: "pointer", background: item.read ? "#fff" : "#f2f8ff" }}>
                <div><strong>{item.title}</strong></div>
                <div>{item.message}</div>
                <small>{item.createdAt}</small>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
```

## 4. Realtime bằng WebSocket, nếu muốn

Backend đã publish notification mới vào:

```text
/topic/device/{deviceId}/notifications
/topic/user/{recipientUserId}/notifications
```

Nếu chưa cần realtime, chỉ cần polling API mỗi 10-30 giây là đủ cho demo đồ án.
