package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.ChatMessage;
import java.time.LocalDateTime;

public class ChatMessageResponse {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String deviceId;
    private LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage message) {
        this.id = message.getId();
        this.sessionId = message.getSession().getId();
        this.role = message.getRole().name();
        this.content = message.getContent();
        this.deviceId = message.getDeviceId();
        this.createdAt = message.getCreatedAt();
    }

    public Long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getDeviceId() { return deviceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
