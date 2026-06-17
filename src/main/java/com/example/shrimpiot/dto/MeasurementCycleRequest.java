package com.example.shrimpiot.dto;

public class MeasurementCycleRequest {
    private String deviceId;
    private String sampleSource;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getSampleSource() { return sampleSource; }
    public void setSampleSource(String sampleSource) { this.sampleSource = sampleSource; }
}
