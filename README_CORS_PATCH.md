# CORS patch for frontend PATCH /api/users/{id}/deactivate and /activate

This version updates Spring Boot CORS configuration for browser frontend calls.

Allowed origins:
- http://localhost:3000
- http://localhost:5173
- http://192.168.1.8:3000
- http://192.168.1.8:5173

Allowed methods:
- GET
- POST
- PUT
- PATCH
- DELETE
- OPTIONS

Important APIs:

```http
PATCH http://192.168.1.8:8080/api/users/{id}/deactivate
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

{
  "reason": "Người dùng không còn quản lý ao này"
}
```

```http
PATCH http://192.168.1.8:8080/api/users/{id}/activate
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

{
  "reason": "Mở lại tài khoản cho người dùng"
}
```

If you use environment variable `CORS_ALLOWED_ORIGINS`, include all frontend origins as a comma-separated string.
