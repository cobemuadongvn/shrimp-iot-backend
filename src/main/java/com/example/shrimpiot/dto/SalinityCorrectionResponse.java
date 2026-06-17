package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.SalinityCorrectionCycle;
import java.time.LocalDateTime;

public class SalinityCorrectionResponse {
    private Long id;
    private String deviceId;
    private Double startSalinity;
    private Double latestSalinity;
    private Double targetSalinity;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String status;
    private String message;
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public SalinityCorrectionResponse() {}
    public SalinityCorrectionResponse(SalinityCorrectionCycle cycle) {
        this.id = cycle.getId();
        this.deviceId = cycle.getDeviceId();
        this.startSalinity = cycle.getStartSalinity();
        this.latestSalinity = cycle.getLatestSalinity();
        this.targetSalinity = cycle.getTargetSalinity();
        this.retryCount = cycle.getRetryCount();
        this.maxRetryCount = cycle.getMaxRetryCount();
        this.status = cycle.getStatus().name();
        this.message = cycle.getMessage();
        this.triggeredBy = cycle.getTriggeredBy();
        this.startedAt = cycle.getStartedAt();
        this.completedAt = cycle.getCompletedAt();
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public Double getStartSalinity() { return startSalinity; }
    public Double getLatestSalinity() { return latestSalinity; }
    public Double getTargetSalinity() { return targetSalinity; }
    public Integer getRetryCount() { return retryCount; }
    public Integer getMaxRetryCount() { return maxRetryCount; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getTriggeredBy() { return triggeredBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
