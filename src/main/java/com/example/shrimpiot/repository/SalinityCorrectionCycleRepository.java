package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.SalinityCorrectionCycle;
import com.example.shrimpiot.model.SalinityCorrectionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalinityCorrectionCycleRepository extends JpaRepository<SalinityCorrectionCycle, Long> {
    List<SalinityCorrectionCycle> findByDeviceIdOrderByStartedAtDesc(String deviceId, Pageable pageable);
    Optional<SalinityCorrectionCycle> findTopByDeviceIdOrderByStartedAtDesc(String deviceId);
    boolean existsByDeviceIdAndStatusIn(String deviceId, List<SalinityCorrectionStatus> statuses);
    boolean existsByDeviceIdAndStartedAtAfter(String deviceId, LocalDateTime after);
}
