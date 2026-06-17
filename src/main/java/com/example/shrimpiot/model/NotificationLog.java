package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_notification_logs_device_time", columnList = "device_id,created_at"),
        @Index(name = "idx_notification_logs_event_key_time", columnList = "event_key,created_at"),
        @Index(name = "idx_notification_logs_status_time", columnList = "status,created_at"),
        @Index(name = "idx_notification_logs_user_read_time", columnList = "recipient_user_id,read_flag,created_at")
})
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    /**
     * Stable key used for anti-spam grouping.
     * For alert notifications this is normally: ALERT:{alertId}:{alertType}.
     * A resolved alert that becomes bad again creates a new alertId, so it can notify immediately.
     */
    @Column(name = "event_key", length = 160)
    private String eventKey;

    @Column(name = "alert_type", length = 50)
    private String alertType;

    @Column(length = 30)
    private String severity;

    @Column(length = 30)
    private String channel;

    @Column(length = 150)
    private String recipient;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_username", length = 100)
    private String recipientUsername;

    @Column(length = 500)
    private String message;

    /** CREATED/SENT/FAILED/SKIPPED_DISABLED/SKIPPED_NO_RECIPIENT/SUPPRESSED_COOLDOWN */
    @Column(length = 40)
    private String status;

    @Column(nullable = false)
    private Boolean suppressed = false;

    @Column(name = "suppression_reason", length = 500)
    private String suppressionReason;

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    @Column(name = "provider_response", length = 1000)
    private String providerResponse;

    @Column(name = "read_flag", nullable = false)
    private Boolean readFlag = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "read_by", length = 100)
    private String readBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (suppressed == null) suppressed = false;
        if (readFlag == null) readFlag = false;
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getEventKey() { return eventKey; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public String getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public Long getRecipientUserId() { return recipientUserId; }
    public String getRecipientUsername() { return recipientUsername; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public Boolean getSuppressed() { return suppressed; }
    public String getSuppressionReason() { return suppressionReason; }
    public LocalDateTime getCooldownUntil() { return cooldownUntil; }
    public String getProviderResponse() { return providerResponse; }
    public Boolean getRead() { return readFlag; }
    public LocalDateTime getReadAt() { return readAt; }
    public String getReadBy() { return readBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
    public void setMessage(String message) { this.message = message; }
    public void setStatus(String status) { this.status = status; }
    public void setSuppressed(Boolean suppressed) { this.suppressed = suppressed; }
    public void setSuppressionReason(String suppressionReason) { this.suppressionReason = suppressionReason; }
    public void setCooldownUntil(LocalDateTime cooldownUntil) { this.cooldownUntil = cooldownUntil; }
    public void setProviderResponse(String providerResponse) { this.providerResponse = providerResponse; }
    public void setRead(Boolean read) { this.readFlag = read; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public void setReadBy(String readBy) { this.readBy = readBy; }
}

