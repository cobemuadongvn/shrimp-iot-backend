package com.example.shrimpiot.dto;

import com.example.shrimpiot.model.MeasurementCycle;
import java.time.LocalDateTime;

public class MeasurementCycleResponse {
    private Long id;
    private String deviceId;
    private String sampleSource;
    private String status;
    private String message;
    private String startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public MeasurementCycleResponse() {}
    public MeasurementCycleResponse(MeasurementCycle cycle) {
        this.id = cycle.getId();
        this.deviceId = cycle.getDeviceId();
        this.sampleSource = cycle.getSampleSource();
        this.status = cycle.getStatus().name();
        this.message = cycle.getMessage();
        this.startedBy = cycle.getStartedBy();
        this.startedAt = cycle.getStartedAt();
        this.completedAt = cycle.getCompletedAt();
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getSampleSource() { return sampleSource; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getStartedBy() { return startedBy; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
