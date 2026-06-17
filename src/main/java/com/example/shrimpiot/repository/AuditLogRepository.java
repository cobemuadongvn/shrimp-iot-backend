package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AuditLog> findByActorUsernameOrderByCreatedAtDesc(String actorUsername, Pageable pageable);
    List<AuditLog> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);
}
