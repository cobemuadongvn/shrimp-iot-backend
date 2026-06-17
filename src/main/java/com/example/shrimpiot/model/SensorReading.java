package com.example.shrimpiot.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_readings_device_time", columnList = "device_id,created_at"),
        @Index(name = "idx_sensor_readings_status_time", columnList = "status,created_at")
})
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    private Double temperature;

    private Double ph;

    @Column(name = "ec_value")
    private Double ecValue;

    private Double salinity;

    @Column(name = "do_value")
    private Double doValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status;

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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.status == null) {
            this.status = ReadingStatus.NORMAL;
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

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getPh() {
        return ph;
    }

    public void setPh(Double ph) {
        this.ph = ph;
    }

    public Double getEcValue() {
        return ecValue;
    }

    public void setEcValue(Double ecValue) {
        this.ecValue = ecValue;
    }

    public Double getSalinity() {
        return salinity;
    }

    public void setSalinity(Double salinity) {
        this.salinity = salinity;
    }

    public Double getDoValue() {
        return doValue;
    }

    public void setDoValue(Double doValue) {
        this.doValue = doValue;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRuleStatus() {
        return ruleStatus;
    }

    public void setRuleStatus(String ruleStatus) {
        this.ruleStatus = ruleStatus;
    }

    public String getAnomalyStatus() {
        return anomalyStatus;
    }

    public void setAnomalyStatus(String anomalyStatus) {
        this.anomalyStatus = anomalyStatus;
    }

    public String getMlStatus() {
        return mlStatus;
    }

    public void setMlStatus(String mlStatus) {
        this.mlStatus = mlStatus;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(String aiMessage) {
        this.aiMessage = aiMessage;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
