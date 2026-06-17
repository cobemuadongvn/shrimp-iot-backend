package com.example.shrimpiot.dto;

public class SalinityCorrectionRequest {
    private String deviceId;
    private Double currentSalinity;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Double getCurrentSalinity() { return currentSalinity; }
    public void setCurrentSalinity(Double currentSalinity) { this.currentSalinity = currentSalinity; }
}
