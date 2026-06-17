package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.Alert;
import java.time.LocalDateTime;

public class AlertResponse {
    private Long id;
    private String deviceId;
    private String alertType;
    private String severity;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    public AlertResponse(Alert alert) {
        this.id = alert.getId();
        this.deviceId = alert.getDeviceId();
        this.alertType = alert.getAlertType().name();
        this.severity = alert.getSeverity().name();
        this.status = alert.getStatus().name();
        this.message = alert.getMessage();
        this.createdAt = alert.getCreatedAt();
        this.resolvedAt = alert.getResolvedAt();
        this.resolvedBy = alert.getResolvedBy();
    }
    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
}
