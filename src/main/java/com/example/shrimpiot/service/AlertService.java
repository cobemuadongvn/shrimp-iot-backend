package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.AlertResponse;
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {
    private final AlertRepository repository;
    private final NotificationService notificationService;
    private final WebSocketEventService webSocketEventService;

    public AlertService(AlertRepository repository, NotificationService notificationService, WebSocketEventService webSocketEventService) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.webSocketEventService = webSocketEventService;
    }

    public void openAlertIfMissing(String deviceId, AlertType type, AlertSeverity severity, String message) {
        var existingOpenAlert = repository.findTopByDeviceIdAndAlertTypeAndStatusOrderByCreatedAtDesc(deviceId, type, AlertStatus.OPEN);
        if (existingOpenAlert.isPresent()) {
            // Anti-spam: the abnormal condition is still active. Do not create a new alert.
            // NotificationService will either suppress and log the duplicate, or send a reminder
            // only after the configured cooldown window has expired.
            Alert alert = existingOpenAlert.get();
            notificationService.notifyAlert(alert);
            return;
        }

        Alert alert = new Alert();
        alert.setDeviceId(deviceId);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setStatus(AlertStatus.OPEN);
        alert.setMessage(message);
        Alert saved = repository.save(alert);
        notificationService.notifyAlert(saved);
        try { webSocketEventService.publishAlert(new com.example.shrimpiot.dto.AlertResponse(saved)); } catch (Exception ignored) {}
    }

    public void autoResolveIfOpen(String deviceId, AlertType type) {
        repository.findTopByDeviceIdAndAlertTypeAndStatusOrderByCreatedAtDesc(deviceId, type, AlertStatus.OPEN)
                .ifPresent(alert -> {
                    alert.setStatus(AlertStatus.RESOLVED);
                    alert.setResolvedAt(LocalDateTime.now());
                    alert.setResolvedBy("SYSTEM");
                    repository.save(alert);
                    try { webSocketEventService.publishAlert(new com.example.shrimpiot.dto.AlertResponse(alert)); } catch (Exception ignored) {}
                });
    }

    public List<AlertResponse> getOpenAlerts(String deviceId) {
        return repository.findByDeviceIdAndStatusOrderByCreatedAtDesc(deviceId, AlertStatus.OPEN).stream().map(AlertResponse::new).toList();
    }

    public List<AlertResponse> getHistory(String deviceId) {
        return repository.findByDeviceIdOrderByCreatedAtDesc(deviceId).stream().map(AlertResponse::new).toList();
    }

    public AlertResponse resolveAlert(Long alertId, String username) {
        Alert alert = repository.findById(alertId).orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(username);
        return new AlertResponse(repository.save(alert));
    }

    public long countOpen(String deviceId) {
        return repository.countByDeviceIdAndStatus(deviceId, AlertStatus.OPEN);
    }
}
