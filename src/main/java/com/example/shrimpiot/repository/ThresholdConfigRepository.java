package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.ThresholdConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThresholdConfigRepository extends JpaRepository<ThresholdConfig, Long> {
    List<ThresholdConfig> findByPond(Pond pond);
    List<ThresholdConfig> findByPondId(Long pondId);
    Optional<ThresholdConfig> findByPondIdAndParameterName(Long pondId, String parameterName);
    List<ThresholdConfig> findByPondIdAndEnabledTrue(Long pondId);
}
