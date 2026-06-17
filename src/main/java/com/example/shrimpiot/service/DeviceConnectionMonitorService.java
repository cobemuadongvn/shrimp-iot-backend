package com.example.shrimpiot.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.shrimpiot.model.AlertSeverity;
import com.example.shrimpiot.model.AlertType;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.repository.DeviceRepository;

@Service
public class DeviceConnectionMonitorService {
    private final DeviceRepository deviceRepository;
    private final AlertService alertService;

    @Value("${device.offline.seconds:60}")
    private long offlineSeconds;

    public DeviceConnectionMonitorService(DeviceRepository deviceRepository, AlertService alertService) {
        this.deviceRepository = deviceRepository;
        this.alertService = alertService;
    }

    @Scheduled(fixedDelayString = "${device.offline.check.ms:30000}")
    public void checkDeviceConnections() {
        LocalDateTime now = LocalDateTime.now();
        List<Device> devices = deviceRepository.findByStatus("ACTIVE")
                .stream()
                .filter(this::isRealDevice)
                .toList();
        for (Device device : devices) {
            LocalDateTime lastSeen = device.getLastSeenAt();
            boolean consideredOffline = (lastSeen == null) || lastSeen.isBefore(now.minusSeconds(offlineSeconds));
            if (consideredOffline && !"OFFLINE".equals(device.getConnectionStatus())) {
                device.setConnectionStatus("OFFLINE");
                deviceRepository.save(device);
                alertService.openAlertIfMissing(device.getDeviceId(), AlertType.DEVICE_OFFLINE, AlertSeverity.WARNING, "Thiết bị mất kết nối");
            }
        }
    }

    private boolean isRealDevice(Device device) {
        if (device == null || device.getDeviceId() == null) {
            return false;
        }
        return !device.getDeviceId().trim().toLowerCase().startsWith("pond_");
    }
}
