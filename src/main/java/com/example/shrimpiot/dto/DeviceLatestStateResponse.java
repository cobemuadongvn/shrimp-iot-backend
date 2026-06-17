package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.DeviceLatestState;
import java.time.LocalDateTime;

public class DeviceLatestStateResponse {
    private String deviceId;
    private Long latestReadingId;
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
    private LocalDateTime updatedAt;

    public DeviceLatestStateResponse(DeviceLatestState state) {
        this.deviceId = state.getDeviceId();
        this.latestReadingId = state.getLatestReadingId();
        this.temperature = state.getTemperature();
        this.ph = state.getPh();
        this.ecValue = state.getEcValue();
        this.salinity = state.getSalinity();
        this.doValue = state.getDoValue();
        this.status = state.getStatus() == null ? null : state.getStatus().name();
        this.message = state.getMessage();
        this.ruleStatus = state.getRuleStatus();
        this.anomalyStatus = state.getAnomalyStatus();
        this.mlStatus = state.getMlStatus();
        this.finalStatus = state.getFinalStatus();
        this.aiMessage = state.getAiMessage();
        this.recommendedAction = state.getRecommendedAction();
        this.updatedAt = state.getUpdatedAt();
    }

    public String getDeviceId() { return deviceId; }
    public Long getLatestReadingId() { return latestReadingId; }
    public Double getTemperature() { return temperature; }
    public Double getPh() { return ph; }
    public Double getEcValue() { return ecValue; }
    public Double getSalinity() { return salinity; }
    public Double getDoValue() { return doValue; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getRuleStatus() { return ruleStatus; }
    public String getAnomalyStatus() { return anomalyStatus; }
    public String getMlStatus() { return mlStatus; }
    public String getFinalStatus() { return finalStatus; }
    public String getAiMessage() { return aiMessage; }
    public String getRecommendedAction() { return recommendedAction; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
