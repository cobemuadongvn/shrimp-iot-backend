package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.DeviceCommand;

import java.time.LocalDateTime;

public class CommandResponse {

    private Long id;
    private String deviceId;
    private Integer relayNo;
    private String action;
    private String status;
    private String source;
    private String requestedBy;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime ackAt;
    private LocalDateTime expiresAt;

    public CommandResponse() {
    }

    public CommandResponse(DeviceCommand command) {
        this.id = command.getId();
        this.deviceId = command.getDeviceId();
        this.relayNo = command.getRelayNo();
        this.action = command.getAction().name();
        this.status = command.getStatus().name();
        this.source = command.getSource();
        this.requestedBy = command.getRequestedBy();
        this.message = command.getMessage();
        this.createdAt = command.getCreatedAt();
        this.sentAt = command.getSentAt();
        this.ackAt = command.getAckAt();
        this.expiresAt = command.getExpiresAt();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Integer getRelayNo() {
        return relayNo;
    }

    public String getAction() {
        return action;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getAckAt() {
        return ackAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
