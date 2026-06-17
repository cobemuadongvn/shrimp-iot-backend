package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.Device;

import java.time.LocalDateTime;

public class MapDeviceResponse {
    private Long id;
    private String deviceId;
    private String name;
    private String status;
    private String connectionStatus;
    private LocalDateTime lastSeenAt;
    private Long pondId;
    private String pondName;
    private Double latitude;
    private Double longitude;
    private String installationPosition;

    public MapDeviceResponse(Device device) {
        this.id = device.getId();
        this.deviceId = device.getDeviceId();
        this.name = device.getName();
        this.status = device.getStatus();
        this.connectionStatus = device.getConnectionStatus();
        this.lastSeenAt = device.getLastSeenAt();
        this.latitude = device.getLatitude();
        this.longitude = device.getLongitude();
        this.installationPosition = device.getInstallationPosition();
        if (device.getPond() != null) {
            this.pondId = device.getPond().getId();
            this.pondName = device.getPond().getName();
        }
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getConnectionStatus() { return connectionStatus; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public Long getPondId() { return pondId; }
    public String getPondName() { return pondName; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getInstallationPosition() { return installationPosition; }
}
