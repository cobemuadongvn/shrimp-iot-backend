package com.example.shrimpiot.dto;

import jakarta.validation.constraints.NotBlank;

public class NotificationTestRequest {
    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotBlank(message = "message is required")
    private String message;

    public String getDeviceId() { return deviceId; }
    public String getMessage() { return message; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setMessage(String message) { this.message = message; }
}
