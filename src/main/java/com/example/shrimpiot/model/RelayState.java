package com.example.shrimpiot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "relay_states", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"device_id", "relay_no"})
})
public class RelayState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "relay_no", nullable = false)
    private Integer relayNo;

    @Column(name = "relay_name")
    private String relayName;

    @Column(name = "current_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private RelayAction currentState = RelayAction.OFF;

    @Column(name = "last_command_id")
    private Long lastCommandId;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RelayState() {}

    public RelayState(String deviceId, Integer relayNo, String relayName) {
        this.deviceId = deviceId;
        this.relayNo = relayNo;
        this.relayName = relayName;
        this.currentState = RelayAction.OFF;
        this.lastUpdatedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.lastUpdatedAt == null) this.lastUpdatedAt = LocalDateTime.now();
        if (this.currentState == null) this.currentState = RelayAction.OFF;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Integer getRelayNo() { return relayNo; }
    public void setRelayNo(Integer relayNo) { this.relayNo = relayNo; }

    public String getRelayName() { return relayName; }
    public void setRelayName(String relayName) { this.relayName = relayName; }

    public RelayAction getCurrentState() { return currentState; }
    public void setCurrentState(RelayAction currentState) { this.currentState = currentState; }

    public Long getLastCommandId() { return lastCommandId; }
    public void setLastCommandId(Long lastCommandId) { this.lastCommandId = lastCommandId; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "RelayState{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", relayNo=" + relayNo +
                ", relayName='" + relayName + '\'' +
                ", currentState=" + currentState +
                ", lastCommandId=" + lastCommandId +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
