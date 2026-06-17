package com.example.shrimpiot.service;

import com.example.shrimpiot.model.AuditLog;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UserAccount actor, String action, String targetType, String targetId, String deviceId, Long pondId, String message, String ipAddress) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setActorUsername(actor.getUsername());
            log.setActorRole(actor.getRole() == null ? null : actor.getRole().name());
        }
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDeviceId(deviceId);
        log.setPondId(pondId);
        log.setMessage(message);
        log.setIpAddress(ipAddress);
        repository.save(log);
    }

    public List<AuditLog> getRecent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit(limit)));
    }

    public List<AuditLog> getByActor(String username, int limit) {
        return repository.findByActorUsernameOrderByCreatedAtDesc(username, PageRequest.of(0, safeLimit(limit)));
    }

    public List<AuditLog> getByDevice(String deviceId, int limit) {
        return repository.findByDeviceIdOrderByCreatedAtDesc(deviceId, PageRequest.of(0, safeLimit(limit)));
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
