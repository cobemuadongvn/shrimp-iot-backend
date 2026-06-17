package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.SensorReading;

import java.time.LocalDateTime;

public class SensorReadingResponse {

    private Long id;
    private String deviceId;
    private Double temperature;
    private Double ph;
    private Double ecValue;
    private Double salinity;
    private Double doValue;
    private String status;
    private String message;
    private String ruleStatus;
    private String anomalyStatus;
    private String mlStatus;
    private String finalStatus;
    private String aiMessage;
    private String recommendedAction;
    private LocalDateTime createdAt;

    public SensorReadingResponse() {
    }

    public SensorReadingResponse(SensorReading reading) {
        this.id = reading.getId();
        this.deviceId = reading.getDeviceId();
        this.temperature = reading.getTemperature();
        this.ph = reading.getPh();
        this.ecValue = reading.getEcValue();
        this.salinity = reading.getSalinity();
        this.doValue = reading.getDoValue();
        this.status = reading.getStatus().name();
        this.message = reading.getMessage();
        this.ruleStatus = reading.getRuleStatus();
        this.anomalyStatus = reading.getAnomalyStatus();
        this.mlStatus = reading.getMlStatus();
        this.finalStatus = reading.getFinalStatus();
        this.aiMessage = reading.getAiMessage();
        this.recommendedAction = reading.getRecommendedAction();
        this.createdAt = reading.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getPh() {
        return ph;
    }

    public Double getEcValue() {
        return ecValue;
    }

    public Double getSalinity() {
        return salinity;
    }

    public Double getDoValue() {
        return doValue;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getRuleStatus() {
        return ruleStatus;
    }

    public String getAnomalyStatus() {
        return anomalyStatus;
    }

    public String getMlStatus() {
        return mlStatus;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
