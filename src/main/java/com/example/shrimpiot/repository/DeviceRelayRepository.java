package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.DeviceRelay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRelayRepository extends JpaRepository<DeviceRelay, Long> {
    List<DeviceRelay> findByDevice(Device device);
    List<DeviceRelay> findByDeviceDeviceId(String deviceId);
    Optional<DeviceRelay> findByDeviceDeviceIdAndRelayNo(String deviceId, Integer relayNo);
}
