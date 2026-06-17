package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_operation_configs")
public class DeviceOperationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", unique = true, nullable = false, length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_mode", nullable = false, length = 20)
    private OperationMode operationMode;

    @Column(name = "salinity_auto_enabled", nullable = false)
    private Boolean salinityAutoEnabled;

    @Column(name = "salinity_high_threshold", nullable = false)
    private Double salinityHighThreshold;

    @Column(name = "salinity_stop_threshold", nullable = false)
    private Double salinityStopThreshold;

    @Column(name = "fill_duration_seconds", nullable = false)
    private Integer fillDurationSeconds;

    @Column(name = "stabilizing_seconds", nullable = false)
    private Integer stabilizingSeconds;

    @Column(name = "measurement_duration_seconds", nullable = false)
    private Integer measurementDurationSeconds;

    @Column(name = "measurement_drain_duration_seconds", nullable = false)
    private Integer measurementDrainDurationSeconds;

    @Column(name = "salinity_drain_duration_seconds", nullable = false)
    private Integer salinityDrainDurationSeconds;

    @Column(name = "freshwater_duration_seconds", nullable = false)
    private Integer freshwaterDurationSeconds;

    @Column(name = "mixing_wait_seconds", nullable = false)
    private Integer mixingWaitSeconds;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount;

    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes;

    @Column(name = "reading_max_age_seconds")
    private Integer readingMaxAgeSeconds;

    @Column(name = "auto_remeasure_enabled")
    private Boolean autoRemeasureEnabled;

    @Column(name = "safety_lock_enabled")
    private Boolean safetyLockEnabled;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (operationMode == null) operationMode = OperationMode.MANUAL;
        if (salinityAutoEnabled == null) salinityAutoEnabled = false;
        // Default cấu hình theo hướng an toàn cho mô hình đồ án:
        // - Có hysteresis 3‰ để tránh bật/tắt liên tục quanh ngưỡng.
        // - Thời gian bơm ngắn, có giới hạn retry, có cooldown.
        // - Có tự đo lại trong AI_AUTO theo phương án B.
        if (salinityHighThreshold == null) salinityHighThreshold = 35.0;
        if (salinityStopThreshold == null) salinityStopThreshold = 32.0;
        if (fillDurationSeconds == null) fillDurationSeconds = 20;
        if (stabilizingSeconds == null) stabilizingSeconds = 45;
        if (measurementDurationSeconds == null) measurementDurationSeconds = 30;
        if (measurementDrainDurationSeconds == null) measurementDrainDurationSeconds = 20;
        if (salinityDrainDurationSeconds == null) salinityDrainDurationSeconds = 20;
        if (freshwaterDurationSeconds == null) freshwaterDurationSeconds = 25;
        if (mixingWaitSeconds == null) mixingWaitSeconds = 120;
        if (maxRetryCount == null) maxRetryCount = 2;
        if (cooldownMinutes == null) cooldownMinutes = 10;
        if (readingMaxAgeSeconds == null) readingMaxAgeSeconds = 120;
        if (autoRemeasureEnabled == null) autoRemeasureEnabled = true;
        if (safetyLockEnabled == null) safetyLockEnabled = false;
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public OperationMode getOperationMode() { return operationMode; }
    public void setOperationMode(OperationMode operationMode) { this.operationMode = operationMode; }
    public Boolean getSalinityAutoEnabled() { return salinityAutoEnabled; }
    public void setSalinityAutoEnabled(Boolean salinityAutoEnabled) { this.salinityAutoEnabled = salinityAutoEnabled; }
    public Double getSalinityHighThreshold() { return salinityHighThreshold; }
    public void setSalinityHighThreshold(Double salinityHighThreshold) { this.salinityHighThreshold = salinityHighThreshold; }
    public Double getSalinityStopThreshold() { return salinityStopThreshold; }
    public void setSalinityStopThreshold(Double salinityStopThreshold) { this.salinityStopThreshold = salinityStopThreshold; }
    public Integer getFillDurationSeconds() { return fillDurationSeconds; }
    public void setFillDurationSeconds(Integer fillDurationSeconds) { this.fillDurationSeconds = fillDurationSeconds; }
    public Integer getStabilizingSeconds() { return stabilizingSeconds; }
    public void setStabilizingSeconds(Integer stabilizingSeconds) { this.stabilizingSeconds = stabilizingSeconds; }
    public Integer getMeasurementDurationSeconds() { return measurementDurationSeconds; }
    public void setMeasurementDurationSeconds(Integer measurementDurationSeconds) { this.measurementDurationSeconds = measurementDurationSeconds; }
    public Integer getMeasurementDrainDurationSeconds() { return measurementDrainDurationSeconds; }
    public void setMeasurementDrainDurationSeconds(Integer measurementDrainDurationSeconds) { this.measurementDrainDurationSeconds = measurementDrainDurationSeconds; }
    public Integer getSalinityDrainDurationSeconds() { return salinityDrainDurationSeconds; }
    public void setSalinityDrainDurationSeconds(Integer salinityDrainDurationSeconds) { this.salinityDrainDurationSeconds = salinityDrainDurationSeconds; }
    public Integer getFreshwaterDurationSeconds() { return freshwaterDurationSeconds; }
    public void setFreshwaterDurationSeconds(Integer freshwaterDurationSeconds) { this.freshwaterDurationSeconds = freshwaterDurationSeconds; }
    public Integer getMixingWaitSeconds() { return mixingWaitSeconds; }
    public void setMixingWaitSeconds(Integer mixingWaitSeconds) { this.mixingWaitSeconds = mixingWaitSeconds; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    public Integer getReadingMaxAgeSeconds() { return readingMaxAgeSeconds; }
    public void setReadingMaxAgeSeconds(Integer readingMaxAgeSeconds) { this.readingMaxAgeSeconds = readingMaxAgeSeconds; }
    public Boolean getAutoRemeasureEnabled() { return autoRemeasureEnabled; }
    public void setAutoRemeasureEnabled(Boolean autoRemeasureEnabled) { this.autoRemeasureEnabled = autoRemeasureEnabled; }
    public Boolean getSafetyLockEnabled() { return safetyLockEnabled; }
    public void setSafetyLockEnabled(Boolean safetyLockEnabled) { this.safetyLockEnabled = safetyLockEnabled; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
