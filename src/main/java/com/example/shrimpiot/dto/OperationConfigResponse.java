package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.DeviceOperationConfig;
import java.time.LocalDateTime;

public class OperationConfigResponse {
    private String deviceId;
    private String operationMode;
    private Boolean salinityAutoEnabled;
    private Double salinityHighThreshold;
    private Double salinityStopThreshold;
    private Integer fillDurationSeconds;
    private Integer stabilizingSeconds;
    private Integer measurementDurationSeconds;
    private Integer measurementDrainDurationSeconds;
    private Integer salinityDrainDurationSeconds;
    private Integer freshwaterDurationSeconds;
    private Integer mixingWaitSeconds;
    private Integer maxRetryCount;
    private Integer cooldownMinutes;
    private Integer readingMaxAgeSeconds;
    private Boolean autoRemeasureEnabled;
    private Boolean safetyLockEnabled;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public OperationConfigResponse() {}

    public OperationConfigResponse(DeviceOperationConfig config) {
        this.deviceId = config.getDeviceId();
        this.operationMode = config.getOperationMode().name();
        this.salinityAutoEnabled = config.getSalinityAutoEnabled();
        this.salinityHighThreshold = config.getSalinityHighThreshold();
        this.salinityStopThreshold = config.getSalinityStopThreshold();
        this.fillDurationSeconds = config.getFillDurationSeconds();
        this.stabilizingSeconds = config.getStabilizingSeconds();
        this.measurementDurationSeconds = config.getMeasurementDurationSeconds();
        this.measurementDrainDurationSeconds = config.getMeasurementDrainDurationSeconds();
        this.salinityDrainDurationSeconds = config.getSalinityDrainDurationSeconds();
        this.freshwaterDurationSeconds = config.getFreshwaterDurationSeconds();
        this.mixingWaitSeconds = config.getMixingWaitSeconds();
        this.maxRetryCount = config.getMaxRetryCount();
        this.cooldownMinutes = config.getCooldownMinutes();
        this.readingMaxAgeSeconds = config.getReadingMaxAgeSeconds();
        this.autoRemeasureEnabled = config.getAutoRemeasureEnabled();
        this.safetyLockEnabled = config.getSafetyLockEnabled();
        this.updatedBy = config.getUpdatedBy();
        this.updatedAt = config.getUpdatedAt();
    }

    public String getDeviceId() { return deviceId; }
    public String getOperationMode() { return operationMode; }
    public Boolean getSalinityAutoEnabled() { return salinityAutoEnabled; }
    public Double getSalinityHighThreshold() { return salinityHighThreshold; }
    public Double getSalinityStopThreshold() { return salinityStopThreshold; }
    public Integer getFillDurationSeconds() { return fillDurationSeconds; }
    public Integer getStabilizingSeconds() { return stabilizingSeconds; }
    public Integer getMeasurementDurationSeconds() { return measurementDurationSeconds; }
    public Integer getMeasurementDrainDurationSeconds() { return measurementDrainDurationSeconds; }
    public Integer getSalinityDrainDurationSeconds() { return salinityDrainDurationSeconds; }
    public Integer getFreshwaterDurationSeconds() { return freshwaterDurationSeconds; }
    public Integer getMixingWaitSeconds() { return mixingWaitSeconds; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public Integer getReadingMaxAgeSeconds() { return readingMaxAgeSeconds; }
    public Boolean getAutoRemeasureEnabled() { return autoRemeasureEnabled; }
    public Boolean getSafetyLockEnabled() { return safetyLockEnabled; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
