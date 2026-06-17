package com.example.shrimpiot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_relays", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"device_id", "relay_no"})
})
public class DeviceRelay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "relay_no", nullable = false)
    private Integer relayNo; // 1, 2, 3, 4

    @Column(nullable = false, length = 150)
    private String name; // e.g. Máy bơm, Quạt nước, Sục oxy

    @Column(name = "relay_type", length = 50)
    private String relayType; // PUMP, FAN, OXYGEN, SPARE

    @Column
    private Integer pin; // e.g. 2, 3, 4, 5

    @Column(nullable = false, length = 30)
    private String status; // ON, OFF, UNKNOWN

    @Column(name = "locked")
    private Boolean locked; // true = không cho gửi command ON/OFF tới relay này

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "OFF";
        }
        if (this.locked == null) {
            this.locked = false;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Integer getRelayNo() {
        return relayNo;
    }

    public void setRelayNo(Integer relayNo) {
        this.relayNo = relayNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelayType() {
        return relayType;
    }

    public void setRelayType(String relayType) {
        this.relayType = relayType;
    }

    public Integer getPin() {
        return pin;
    }

    public void setPin(Integer pin) {
        this.pin = pin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getLocked() {
        return locked;
    }

    public boolean isLocked() {
        return Boolean.TRUE.equals(locked);
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
