package com.example.shrimpiot.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    private Long sessionId;
    private String deviceId;

    @NotBlank(message = "message is required")
    private String message;

    public Long getSessionId() { return sessionId; }
    public String getDeviceId() { return deviceId; }
    public String getMessage() { return message; }

    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setMessage(String message) { this.message = message; }
}
