package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Alert;
import com.example.shrimpiot.model.AlertStatus;
import com.example.shrimpiot.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    boolean existsByDeviceIdAndAlertTypeAndStatus(String deviceId, AlertType alertType, AlertStatus status);
    Optional<Alert> findTopByDeviceIdAndAlertTypeAndStatusOrderByCreatedAtDesc(String deviceId, AlertType alertType, AlertStatus status);
    List<Alert> findByDeviceIdAndStatusOrderByCreatedAtDesc(String deviceId, AlertStatus status);
    List<Alert> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    List<Alert> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(String deviceId, LocalDateTime from, LocalDateTime to);
    long countByDeviceIdAndStatus(String deviceId, AlertStatus status);
}
