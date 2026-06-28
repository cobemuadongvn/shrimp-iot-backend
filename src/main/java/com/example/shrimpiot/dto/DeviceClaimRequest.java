package com.example.shrimpiot.dto;

public class DeviceClaimRequest {
    private Integer version;
    private String deviceId;
    private String claimCode;
    private Long pondId;

    public Integer getVersion() { return version; }
    public String getDeviceId() { return deviceId; }
    public String getClaimCode() { return claimCode; }
    public Long getPondId() { return pondId; }
    public void setVersion(Integer version) { this.version = version; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setClaimCode(String claimCode) { this.claimCode = claimCode; }
    public void setPondId(Long pondId) { this.pondId = pondId; }
}
