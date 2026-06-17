package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_latest_states", indexes = {
        @Index(name = "idx_device_latest_states_updated_at", columnList = "updated_at")
})
public class DeviceLatestState {

    @Id
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "latest_reading_id")
    private Long latestReadingId;

    private Double temperature;
    private Double ph;

    @Column(name = "ec_value")
    private Double ecValue;

    private Double salinity;

    @Column(name = "do_value")
    private Double doValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status = ReadingStatus.NORMAL;

    @Column(length = 500)
    private String message;

    @Column(name = "rule_status", length = 20)
    private String ruleStatus;

    @Column(name = "anomaly_status", length = 20)
    private String anomalyStatus;

    @Column(name = "ml_status", length = 20)
    private String mlStatus;

    @Column(name = "final_status", length = 20)
    private String finalStatus;

    @Column(name = "ai_message", length = 500)
    private String aiMessage;

    @Column(name = "recommended_action", length = 500)
    private String recommendedAction;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null) status = ReadingStatus.NORMAL;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static DeviceLatestState fromReading(SensorReading reading) {
        DeviceLatestState state = new DeviceLatestState();
        state.applyReading(reading);
        return state;
    }

    public void applyReading(SensorReading reading) {
        this.deviceId = reading.getDeviceId();
        this.latestReadingId = reading.getId();
        this.temperature = reading.getTemperature();
        this.ph = reading.getPh();
        this.ecValue = reading.getEcValue();
        this.salinity = reading.getSalinity();
        this.doValue = reading.getDoValue();
        this.status = reading.getStatus();
        this.message = reading.getMessage();
        this.ruleStatus = reading.getRuleStatus();
        this.anomalyStatus = reading.getAnomalyStatus();
        this.mlStatus = reading.getMlStatus();
        this.finalStatus = reading.getFinalStatus();
        this.aiMessage = reading.getAiMessage();
        this.recommendedAction = reading.getRecommendedAction();
        this.updatedAt = LocalDateTime.now();
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Long getLatestReadingId() { return latestReadingId; }
    public Double getTemperature() { return temperature; }
    public Double getPh() { return ph; }
    public Double getEcValue() { return ecValue; }
    public Double getSalinity() { return salinity; }
    public Double getDoValue() { return doValue; }
    public ReadingStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public String getRuleStatus() { return ruleStatus; }
    public String getAnomalyStatus() { return anomalyStatus; }
    public String getMlStatus() { return mlStatus; }
    public String getFinalStatus() { return finalStatus; }
    public String getAiMessage() { return aiMessage; }
    public String getRecommendedAction() { return recommendedAction; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
