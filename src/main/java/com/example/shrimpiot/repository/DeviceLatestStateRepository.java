package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.DeviceLatestState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLatestStateRepository extends JpaRepository<DeviceLatestState, String> {
}
