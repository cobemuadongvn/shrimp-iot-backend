package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.NotificationLog;
import java.time.LocalDateTime;

public class NotificationResponse {
    private Long id;
    private String deviceId;
    private String eventKey;
    private String alertType;
    private String severity;
    private String channel;
    private String recipient;
    private Long recipientUserId;
    private String recipientUsername;
    private String title;
    private String message;
    private String status;
    private Boolean suppressed;
    private String suppressionReason;
    private LocalDateTime cooldownUntil;
    private Boolean read;
    private LocalDateTime readAt;
    private String readBy;
    private LocalDateTime createdAt;

    public NotificationResponse(NotificationLog log) {
        this.id = log.getId();
        this.deviceId = log.getDeviceId();
        this.eventKey = log.getEventKey();
        this.alertType = log.getAlertType();
        this.severity = log.getSeverity();
        this.channel = log.getChannel();
        this.recipient = log.getRecipient();
        this.recipientUserId = log.getRecipientUserId();
        this.recipientUsername = log.getRecipientUsername();
        this.title = buildTitle(log);
        this.message = log.getMessage();
        this.status = log.getStatus();
        this.suppressed = log.getSuppressed();
        this.suppressionReason = log.getSuppressionReason();
        this.cooldownUntil = log.getCooldownUntil();
        this.read = log.getRead();
        this.readAt = log.getReadAt();
        this.readBy = log.getReadBy();
        this.createdAt = log.getCreatedAt();
    }

    private String buildTitle(NotificationLog log) {
        String severity = log.getSeverity() == null ? "INFO" : log.getSeverity();
        String alertType = log.getAlertType() == null ? "SYSTEM" : log.getAlertType();
        if (Boolean.TRUE.equals(log.getSuppressed())) {
            return "Đã chặn gửi lặp cảnh báo " + alertType;
        }
        if ("DANGER".equalsIgnoreCase(severity)) {
            return "Cảnh báo nguy hiểm: " + alertType;
        }
        if ("WARNING".equalsIgnoreCase(severity)) {
            return "Cảnh báo: " + alertType;
        }
        return "Thông báo hệ thống";
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
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public Boolean getSuppressed() { return suppressed; }
    public String getSuppressionReason() { return suppressionReason; }
    public LocalDateTime getCooldownUntil() { return cooldownUntil; }
    public Boolean getRead() { return read; }
    public LocalDateTime getReadAt() { return readAt; }
    public String getReadBy() { return readBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
