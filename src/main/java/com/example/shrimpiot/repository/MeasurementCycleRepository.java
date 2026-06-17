package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.MeasurementCycle;
import com.example.shrimpiot.model.MeasurementCycleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeasurementCycleRepository extends JpaRepository<MeasurementCycle, Long> {
    List<MeasurementCycle> findByDeviceIdOrderByStartedAtDesc(String deviceId, Pageable pageable);
    Optional<MeasurementCycle> findTopByDeviceIdOrderByStartedAtDesc(String deviceId);
    boolean existsByDeviceIdAndStatusIn(String deviceId, List<MeasurementCycleStatus> statuses);
}
