package com.example.shrimpiot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportSummaryResponse {
    private String deviceId;
    private LocalDateTime from;
    private LocalDateTime to;
    private long readingCount;
    private long alertCount;
    private long openAlertCount;
    private long resolvedAlertCount;
    private long commandCount;
    private long manualCommandCount;
    private long autoCommandCount;
    private List<MetricStatsResponse> metrics;
    private Map<String, Long> alertsByType;
    private Map<String, Long> commandsByRelay;

    public ReportSummaryResponse(String deviceId, LocalDateTime from, LocalDateTime to, long readingCount,
                                 long alertCount, long openAlertCount, long resolvedAlertCount,
                                 long commandCount, long manualCommandCount, long autoCommandCount,
                                 List<MetricStatsResponse> metrics,
                                 Map<String, Long> alertsByType,
                                 Map<String, Long> commandsByRelay) {
        this.deviceId = deviceId;
        this.from = from;
        this.to = to;
        this.readingCount = readingCount;
        this.alertCount = alertCount;
        this.openAlertCount = openAlertCount;
        this.resolvedAlertCount = resolvedAlertCount;
        this.commandCount = commandCount;
        this.manualCommandCount = manualCommandCount;
        this.autoCommandCount = autoCommandCount;
        this.metrics = metrics;
        this.alertsByType = alertsByType;
        this.commandsByRelay = commandsByRelay;
    }

    public String getDeviceId() { return deviceId; }
    public LocalDateTime getFrom() { return from; }
    public LocalDateTime getTo() { return to; }
    public long getReadingCount() { return readingCount; }
    public long getAlertCount() { return alertCount; }
    public long getOpenAlertCount() { return openAlertCount; }
    public long getResolvedAlertCount() { return resolvedAlertCount; }
    public long getCommandCount() { return commandCount; }
    public long getManualCommandCount() { return manualCommandCount; }
    public long getAutoCommandCount() { return autoCommandCount; }
    public List<MetricStatsResponse> getMetrics() { return metrics; }
    public Map<String, Long> getAlertsByType() { return alertsByType; }
    public Map<String, Long> getCommandsByRelay() { return commandsByRelay; }
}
