package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.Pond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceId(String deviceId);

    Optional<Device> findByDeviceIdAndStatus(String deviceId, String status);

    List<Device> findByPond(Pond pond);

    List<Device> findByPondAndStatus(Pond pond, String status);

    List<Device> findByStatus(String status);

    List<Device> findByPondInAndStatus(List<Pond> ponds, String status);

    boolean existsByDeviceId(String deviceId);
}
