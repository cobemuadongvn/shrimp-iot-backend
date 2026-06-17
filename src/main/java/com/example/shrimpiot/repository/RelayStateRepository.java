package com.example.shrimpiot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shrimpiot.model.RelayState;

@Repository
public interface RelayStateRepository extends JpaRepository<RelayState, Long> {
    Optional<RelayState> findByDeviceIdAndRelayNo(String deviceId, Integer relayNo);
    List<RelayState> findByDeviceId(String deviceId);
    void deleteByDeviceId(String deviceId);
}
