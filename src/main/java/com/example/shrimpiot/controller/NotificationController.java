package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.NotificationResponse;
import com.example.shrimpiot.dto.NotificationTestRequest;
import com.example.shrimpiot.dto.NotificationUnreadCountResponse;
import com.example.shrimpiot.model.NotificationLog;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    /**
     * Technical log endpoint. It returns all notification logs for a device, including APP/SMS/EMAIL/SYSTEM logs.
     * Frontend should use /in-app for the user notification bell/list.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getNotificationLogs(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Notification logs", notificationService.getByDeviceId(deviceId)));
    }

    /**
     * In-app notification list for the current logged-in user.
     * Use unreadOnly=true for the notification bell dropdown.
     */
    @GetMapping("/in-app")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getInAppNotifications(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        authService.validateAccessToDevice(authorization, deviceId);
        List<NotificationResponse> data = notificationService.getInAppNotifications(user, deviceId, unreadOnly, limit);
        return ResponseEntity.ok(ApiResponse.ok("In-app notifications", data));
    }

    @GetMapping("/in-app/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        authService.validateAccessToDevice(authorization, deviceId);
        long count = notificationService.countUnreadInApp(user, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Unread in-app notification count", new NotificationUnreadCountResponse(deviceId, count)));
    }

    @PatchMapping("/in-app/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markOneRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        NotificationResponse response = notificationService.markInAppAsRead(user, id);
        authService.validateAccessToDevice(authorization, response.getDeviceId());
        return ResponseEntity.ok(ApiResponse.ok("In-app notification marked as read", response));
    }

    @PatchMapping("/in-app/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        authService.validateAccessToDevice(authorization, deviceId);
        int updated = notificationService.markAllInAppAsRead(user, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("All in-app notifications marked as read", Map.of(
                "deviceId", deviceId,
                "updated", updated
        )));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Object>> testNotification(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody NotificationTestRequest request
    ) {
        authService.validateAccessToDevice(authorization, request.getDeviceId());
        notificationService.notifyAlert(request.getDeviceId(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.ok("Notification test created", null));
    }
}
