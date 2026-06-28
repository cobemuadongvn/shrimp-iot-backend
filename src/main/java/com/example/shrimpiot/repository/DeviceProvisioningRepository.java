package com.example.shrimpiot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.shrimpiot.model.DeviceProvisioning;

import jakarta.persistence.LockModeType;

public interface DeviceProvisioningRepository extends JpaRepository<DeviceProvisioning, Long> {
    Optional<DeviceProvisioning> findByDeviceDeviceId(String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DeviceProvisioning p where p.device.deviceId = :deviceId")
    Optional<DeviceProvisioning> findByDeviceIdForUpdate(@Param("deviceId") String deviceId);
}
