package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.DeviceSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSensorRepository extends JpaRepository<DeviceSensor, Long> {
    List<DeviceSensor> findByDevice(Device device);
    List<DeviceSensor> findByDeviceDeviceId(String deviceId);
    Optional<DeviceSensor> findByDeviceDeviceIdAndSensorType(String deviceId, String sensorType);
}
