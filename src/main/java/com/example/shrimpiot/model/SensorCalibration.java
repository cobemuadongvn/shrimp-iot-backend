package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_calibrations", indexes = {
        @Index(name = "idx_sensor_calibrations_device_type", columnList = "device_id,sensor_type,active"),
        @Index(name = "idx_sensor_calibrations_created_at", columnList = "created_at")
})
public class SensorCalibration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "sensor_type", nullable = false, length = 50)
    private String sensorType;

    @Column(name = "offset_value", nullable = false)
    private Double offsetValue = 0.0;

    @Column(name = "slope_value", nullable = false)
    private Double slopeValue = 1.0;

    @Column(name = "calibration_point_1")
    private Double calibrationPoint1;

    @Column(name = "calibration_point_2")
    private Double calibrationPoint2;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (offsetValue == null) offsetValue = 0.0;
        if (slopeValue == null) slopeValue = 1.0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
