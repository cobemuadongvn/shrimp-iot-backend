package com.example.shrimpiot.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_commands", indexes = {
        @Index(name = "idx_device_commands_device_status", columnList = "device_id,status"),
        @Index(name = "idx_device_commands_device_created", columnList = "device_id,created_at"),
        @Index(name = "idx_device_commands_status_expires", columnList = "status,expires_at")
})
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "relay_no", nullable = false)
    private Integer relayNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RelayAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommandStatus status;

    @Column(length = 30)
    private String source; // MANUAL / AUTO

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "ack_at")
    private LocalDateTime ackAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = CommandStatus.PENDING;
        }

        if (this.source == null) {
            this.source = "MANUAL";
        }
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getRelayNo() {
        return relayNo;
    }

    public void setRelayNo(Integer relayNo) {
        this.relayNo = relayNo;
    }

    public RelayAction getAction() {
        return action;
    }

    public void setAction(RelayAction action) {
        this.action = action;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public void setStatus(CommandStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getAckAt() {
        return ackAt;
    }

    public void setAckAt(LocalDateTime ackAt) {
        this.ackAt = ackAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
