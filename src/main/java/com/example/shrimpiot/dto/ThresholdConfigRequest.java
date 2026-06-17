package com.example.shrimpiot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class ThresholdConfigRequest {

    @NotNull(message = "pondId is required")
    private Long pondId;

    @NotBlank(message = "parameterName is required")
    private String parameterName; // TEMPERATURE, PH, EC, SALINITY, DO

    @NotNull(message = "minValue is required")
    private Double minValue;

    @NotNull(message = "maxValue is required")
    private Double maxValue;

    private String severity; // WARNING, DANGER, CRITICAL (default: WARNING)

    private Boolean enabled; // default: true

    // Getters and Setters
    public Long getPondId() {
        return pondId;
    }

    public void setPondId(Long pondId) {
        this.pondId = pondId;
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
}
