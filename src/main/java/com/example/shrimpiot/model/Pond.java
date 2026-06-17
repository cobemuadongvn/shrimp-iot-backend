package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ponds")
public class Pond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "area_square_meters")
    private Double areaSquareMeters;

    @Column(name = "species_type", length = 100)
    private String speciesType;

    @Column(name = "pond_type", length = 100)
    private String pondType;

    @Column(name = "water_volume_cubic_meters")
    private Double waterVolumeCubicMeters;

    @Column(length = 100)
    private String region;

    @Column(length = 30)
    private String status;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getAreaSquareMeters() {
        return areaSquareMeters;
    }

    public void setAreaSquareMeters(Double areaSquareMeters) {
        this.areaSquareMeters = areaSquareMeters;
    }

    public String getSpeciesType() { return speciesType; }

    public void setSpeciesType(String speciesType) { this.speciesType = speciesType; }

    public String getPondType() { return pondType; }

    public void setPondType(String pondType) { this.pondType = pondType; }

    public Double getWaterVolumeCubicMeters() { return waterVolumeCubicMeters; }

    public void setWaterVolumeCubicMeters(Double waterVolumeCubicMeters) { this.waterVolumeCubicMeters = waterVolumeCubicMeters; }

    public String getRegion() { return region; }

    public void setRegion(String region) { this.region = region; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }

    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
