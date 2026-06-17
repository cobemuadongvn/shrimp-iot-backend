package com.example.shrimpiot.dto;

import java.time.LocalDateTime;

import com.example.shrimpiot.model.RelayAction;

public class RelayStateResponse {
    private Long id;
    private String deviceId;
    private Integer relayNo;
    private String relayName;
    private RelayAction currentState;
    private Long lastCommandId;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime createdAt;

    public RelayStateResponse() {}

    public RelayStateResponse(com.example.shrimpiot.model.RelayState relayState) {
        this.id = relayState.getId();
        this.deviceId = relayState.getDeviceId();
        this.relayNo = relayState.getRelayNo();
        this.relayName = relayState.getRelayName();
        this.currentState = relayState.getCurrentState();
        this.lastCommandId = relayState.getLastCommandId();
        this.lastUpdatedAt = relayState.getLastUpdatedAt();
        this.createdAt = relayState.getCreatedAt();
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
        return "RelayStateResponse{" +
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
