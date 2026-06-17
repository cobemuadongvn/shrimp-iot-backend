package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.DeviceOperationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceOperationConfigRepository extends JpaRepository<DeviceOperationConfig, Long> {
    Optional<DeviceOperationConfig> findByDeviceId(String deviceId);
}
