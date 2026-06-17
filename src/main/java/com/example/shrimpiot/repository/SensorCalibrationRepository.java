package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.SensorCalibration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorCalibrationRepository extends JpaRepository<SensorCalibration, Long> {
    List<SensorCalibration> findByDeviceIdOrderBySensorTypeAscCreatedAtDesc(String deviceId);
    Optional<SensorCalibration> findTopByDeviceIdAndSensorTypeAndActiveTrueOrderByUpdatedAtDesc(String deviceId, String sensorType);
}
