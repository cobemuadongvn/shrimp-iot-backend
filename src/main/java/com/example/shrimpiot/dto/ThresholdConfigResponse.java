package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.ThresholdConfig;
import java.time.LocalDateTime;

public class ThresholdConfigResponse {

    private Long id;
    private Long pondId;
    private String pondName;
    private String parameterName;
    private Double minValue;
    private Double maxValue;
    private String severity;
    private Boolean enabled;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public ThresholdConfigResponse(ThresholdConfig config) {
        this.id = config.getId();
        this.pondId = config.getPond().getId();
        this.pondName = config.getPond().getName();
        this.parameterName = config.getParameterName();
        this.minValue = config.getMinValue();
        this.maxValue = config.getMaxValue();
        this.severity = config.getSeverity();
        this.enabled = config.getEnabled();
        this.updatedBy = config.getUpdatedBy();
        this.updatedAt = config.getUpdatedAt();
        this.createdAt = config.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPondId() {
        return pondId;
    }

    public void setPondId(Long pondId) {
        this.pondId = pondId;
    }

    public String getPondName() {
        return pondName;
    }

    public void setPondName(String pondName) {
        this.pondName = pondName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
