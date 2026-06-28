package com.example.shrimpiot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_provisioning")
public class DeviceProvisioning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_status", nullable = false, length = 20)
    private DeviceClaimStatus claimStatus;

    @Column(name = "claim_code_hash", length = 64)
    private String claimCodeHash;

    @Column(name = "claim_code_expires_at")
    private LocalDateTime claimCodeExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_user_id")
    private UserAccount claimedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_pond_id")
    private Pond claimedPond;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (claimStatus == null) claimStatus = DeviceClaimStatus.UNCLAIMED;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Device getDevice() { return device; }
    public DeviceClaimStatus getClaimStatus() { return claimStatus; }
    public String getClaimCodeHash() { return claimCodeHash; }
    public LocalDateTime getClaimCodeExpiresAt() { return claimCodeExpiresAt; }
    public UserAccount getClaimedBy() { return claimedBy; }
    public Pond getClaimedPond() { return claimedPond; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setDevice(Device device) { this.device = device; }
    public void setClaimStatus(DeviceClaimStatus claimStatus) { this.claimStatus = claimStatus; }
    public void setClaimCodeHash(String claimCodeHash) { this.claimCodeHash = claimCodeHash; }
    public void setClaimCodeExpiresAt(LocalDateTime claimCodeExpiresAt) { this.claimCodeExpiresAt = claimCodeExpiresAt; }
    public void setClaimedBy(UserAccount claimedBy) { this.claimedBy = claimedBy; }
    public void setClaimedPond(Pond claimedPond) { this.claimedPond = claimedPond; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
