package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.ReadingStatus;
import com.example.shrimpiot.model.SensorReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    Optional<SensorReading> findTopByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<SensorReading> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    List<SensorReading> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String deviceId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<SensorReading> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            String deviceId,
            LocalDateTime from,
            LocalDateTime to
    );

    long countByDeviceId(String deviceId);

    long countByDeviceIdAndStatus(String deviceId, ReadingStatus status);
}
