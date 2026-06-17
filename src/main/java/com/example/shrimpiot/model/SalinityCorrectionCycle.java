package com.example.shrimpiot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "salinity_correction_cycles")
public class SalinityCorrectionCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "start_salinity")
    private Double startSalinity;

    @Column(name = "latest_salinity")
    private Double latestSalinity;

    @Column(name = "target_salinity")
    private Double targetSalinity;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SalinityCorrectionStatus status;

    @Column(length = 500)
    private String message;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (retryCount == null) retryCount = 0;
        if (maxRetryCount == null) maxRetryCount = 3;
        if (status == null) status = SalinityCorrectionStatus.SALINITY_HIGH_DETECTED;
        if (startedAt == null) startedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Double getStartSalinity() { return startSalinity; }
    public void setStartSalinity(Double startSalinity) { this.startSalinity = startSalinity; }
    public Double getLatestSalinity() { return latestSalinity; }
    public void setLatestSalinity(Double latestSalinity) { this.latestSalinity = latestSalinity; }
    public Double getTargetSalinity() { return targetSalinity; }
    public void setTargetSalinity(Double targetSalinity) { this.targetSalinity = targetSalinity; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(Integer maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public SalinityCorrectionStatus getStatus() { return status; }
    public void setStatus(SalinityCorrectionStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
