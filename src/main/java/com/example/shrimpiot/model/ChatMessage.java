package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public ChatSession getSession() { return session; }
    public ChatMessageRole getRole() { return role; }
    public String getContent() { return content; }
    public String getDeviceId() { return deviceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setSession(ChatSession session) { this.session = session; }
    public void setRole(ChatMessageRole role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
