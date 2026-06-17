package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.DashboardSummaryResponse;
import com.example.shrimpiot.dto.SensorReadingResponse;
import com.example.shrimpiot.model.ReadingStatus;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final SensorReadingService sensorReadingService;
    private final AlertService alertService;
    private final CommandService commandService;

    public DashboardService(SensorReadingService sensorReadingService, AlertService alertService, CommandService commandService) {
        this.sensorReadingService = sensorReadingService;
        this.alertService = alertService;
        this.commandService = commandService;
    }

    public DashboardSummaryResponse getSummary(String deviceId) {
        SensorReadingResponse latest = sensorReadingService.getLatest(deviceId);
        long total = sensorReadingService.countByDeviceId(deviceId);
        long normal = sensorReadingService.countByStatus(deviceId, ReadingStatus.NORMAL);
        long warning = sensorReadingService.countByStatus(deviceId, ReadingStatus.WARNING);
        long danger = sensorReadingService.countByStatus(deviceId, ReadingStatus.DANGER);
        long openAlerts = alertService.countOpen(deviceId);
        long commands = commandService.countByDeviceId(deviceId);
        return new DashboardSummaryResponse(deviceId, latest, total, normal, warning, danger, openAlerts, commands);
    }
}
