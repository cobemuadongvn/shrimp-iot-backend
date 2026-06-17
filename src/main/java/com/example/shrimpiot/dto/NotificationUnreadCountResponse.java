package com.example.shrimpiot.dto;

public class NotificationUnreadCountResponse {
    private String deviceId;
    private long unreadCount;

    public NotificationUnreadCountResponse(String deviceId, long unreadCount) {
        this.deviceId = deviceId;
        this.unreadCount = unreadCount;
    }

    public String getDeviceId() { return deviceId; }
    public long getUnreadCount() { return unreadCount; }
}
