package com.example.shrimpiot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SensorCalibrationRequest {
    @NotBlank(message = "sensorType is required")
    private String sensorType;

    @NotNull(message = "offsetValue is required")
    private Double offsetValue;

    @NotNull(message = "slopeValue is required")
    private Double slopeValue;

    private Double calibrationPoint1;
    private Double calibrationPoint2;
    private String note;
    private Boolean active;

    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public Double getOffsetValue() { return offsetValue; }
    public void setOffsetValue(Double offsetValue) { this.offsetValue = offsetValue; }
    public Double getSlopeValue() { return slopeValue; }
    public void setSlopeValue(Double slopeValue) { this.slopeValue = slopeValue; }
    public Double getCalibrationPoint1() { return calibrationPoint1; }
    public void setCalibrationPoint1(Double calibrationPoint1) { this.calibrationPoint1 = calibrationPoint1; }
    public Double getCalibrationPoint2() { return calibrationPoint2; }
    public void setCalibrationPoint2(Double calibrationPoint2) { this.calibrationPoint2 = calibrationPoint2; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
