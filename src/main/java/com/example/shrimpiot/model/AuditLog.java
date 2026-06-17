package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "actor_role", length = 30)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id", length = 150)
    private String targetId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "pond_id")
    private Long pondId;

    @Column(length = 1000)
    private String message;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getActorUsername() { return actorUsername; }
    public String getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getDeviceId() { return deviceId; }
    public Long getPondId() { return pondId; }
    public String getMessage() { return message; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public void setAction(String action) { this.action = action; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setPondId(Long pondId) { this.pondId = pondId; }
    public void setMessage(String message) { this.message = message; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
