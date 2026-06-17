package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.Pond;

import java.util.List;

public class MapPondResponse {
    private Long id;
    private String name;
    private String location;
    private String region;
    private String speciesType;
    private String pondType;
    private String status;
    private Double areaSquareMeters;
    private Double waterVolumeCubicMeters;
    private Double latitude;
    private Double longitude;
    private String description;
    private List<MapDeviceResponse> devices;

    public MapPondResponse(Pond pond, List<MapDeviceResponse> devices) {
        this.id = pond.getId();
        this.name = pond.getName();
        this.location = pond.getLocation();
        this.region = pond.getRegion();
        this.speciesType = pond.getSpeciesType();
        this.pondType = pond.getPondType();
        this.status = pond.getStatus();
        this.areaSquareMeters = pond.getAreaSquareMeters();
        this.waterVolumeCubicMeters = pond.getWaterVolumeCubicMeters();
        this.latitude = pond.getLatitude();
        this.longitude = pond.getLongitude();
        this.description = pond.getDescription();
        this.devices = devices;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getRegion() { return region; }
    public String getSpeciesType() { return speciesType; }
    public String getPondType() { return pondType; }
    public String getStatus() { return status; }
    public Double getAreaSquareMeters() { return areaSquareMeters; }
    public Double getWaterVolumeCubicMeters() { return waterVolumeCubicMeters; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getDescription() { return description; }
    public List<MapDeviceResponse> getDevices() { return devices; }
}
