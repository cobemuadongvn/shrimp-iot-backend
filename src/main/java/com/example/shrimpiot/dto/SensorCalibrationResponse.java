package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.SensorCalibration;
import java.time.LocalDateTime;

public class SensorCalibrationResponse {
    private Long id;
    private String deviceId;
    private String sensorType;
    private Double offsetValue;
    private Double slopeValue;
    private Double calibrationPoint1;
    private Double calibrationPoint2;
    private String note;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SensorCalibrationResponse(SensorCalibration calibration) {
        this.id = calibration.getId();
        this.deviceId = calibration.getDeviceId();
        this.sensorType = calibration.getSensorType();
        this.offsetValue = calibration.getOffsetValue();
        this.slopeValue = calibration.getSlopeValue();
        this.calibrationPoint1 = calibration.getCalibrationPoint1();
        this.calibrationPoint2 = calibration.getCalibrationPoint2();
        this.note = calibration.getNote();
        this.active = calibration.isActive();
        this.createdAt = calibration.getCreatedAt();
        this.updatedAt = calibration.getUpdatedAt();
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getSensorType() { return sensorType; }
    public Double getOffsetValue() { return offsetValue; }
    public Double getSlopeValue() { return slopeValue; }
    public Double getCalibrationPoint1() { return calibrationPoint1; }
    public Double getCalibrationPoint2() { return calibrationPoint2; }
    public String getNote() { return note; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
