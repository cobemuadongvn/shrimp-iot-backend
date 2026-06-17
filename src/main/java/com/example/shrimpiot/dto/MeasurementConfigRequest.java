package com.example.shrimpiot.dto;

public class MeasurementConfigRequest {
    private Integer fillDurationSeconds;
    private Integer stabilizingSeconds;
    private Integer measurementDurationSeconds;
    private Integer measurementDrainDurationSeconds;

    public Integer getFillDurationSeconds() { return fillDurationSeconds; }
    public void setFillDurationSeconds(Integer fillDurationSeconds) { this.fillDurationSeconds = fillDurationSeconds; }
    public Integer getStabilizingSeconds() { return stabilizingSeconds; }
    public void setStabilizingSeconds(Integer stabilizingSeconds) { this.stabilizingSeconds = stabilizingSeconds; }
    public Integer getMeasurementDurationSeconds() { return measurementDurationSeconds; }
    public void setMeasurementDurationSeconds(Integer measurementDurationSeconds) { this.measurementDurationSeconds = measurementDurationSeconds; }
    public Integer getMeasurementDrainDurationSeconds() { return measurementDrainDurationSeconds; }
    public void setMeasurementDrainDurationSeconds(Integer measurementDrainDurationSeconds) { this.measurementDrainDurationSeconds = measurementDrainDurationSeconds; }
}
