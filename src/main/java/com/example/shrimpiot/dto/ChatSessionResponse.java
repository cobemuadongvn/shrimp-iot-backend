package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.ChatSession;
import java.time.LocalDateTime;

public class ChatSessionResponse {
    private Long id;
    private String username;
    private String deviceId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChatSessionResponse(ChatSession session) {
        this.id = session.getId();
        this.username = session.getUser().getUsername();
        this.deviceId = session.getDeviceId();
        this.title = session.getTitle();
        this.createdAt = session.getCreatedAt();
        this.updatedAt = session.getUpdatedAt();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDeviceId() { return deviceId; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
