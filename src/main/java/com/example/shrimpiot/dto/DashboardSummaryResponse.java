package com.example.shrimpiot.dto;

public class DashboardSummaryResponse {
    private String deviceId;
    private SensorReadingResponse latest;
    private long totalReadings;
    private long normalCount;
    private long warningCount;
    private long dangerCount;
    private long openAlertCount;
    private long commandCount;

    public DashboardSummaryResponse(String deviceId, SensorReadingResponse latest, long totalReadings, long normalCount, long warningCount, long dangerCount, long openAlertCount, long commandCount) {
        this.deviceId = deviceId;
        this.latest = latest;
        this.totalReadings = totalReadings;
        this.normalCount = normalCount;
        this.warningCount = warningCount;
        this.dangerCount = dangerCount;
        this.openAlertCount = openAlertCount;
        this.commandCount = commandCount;
    }

    public String getDeviceId() { return deviceId; }
    public SensorReadingResponse getLatest() { return latest; }
    public long getTotalReadings() { return totalReadings; }
    public long getNormalCount() { return normalCount; }
    public long getWarningCount() { return warningCount; }
    public long getDangerCount() { return dangerCount; }
    public long getOpenAlertCount() { return openAlertCount; }
    public long getCommandCount() { return commandCount; }
}
