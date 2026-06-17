package com.example.shrimpiot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shrimpiot.model.CommandStatus;
import com.example.shrimpiot.model.DeviceCommand;
import com.example.shrimpiot.model.RelayAction;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {
    List<DeviceCommand> findByDeviceIdAndStatusOrderByCreatedAtAsc(String deviceId, CommandStatus status);
    List<DeviceCommand> findByDeviceIdAndRelayNoAndStatusInOrderByCreatedAtAsc(String deviceId, Integer relayNo, List<CommandStatus> statuses);
    List<DeviceCommand> findTop20ByStatusOrderByCreatedAtAsc(CommandStatus status);
    List<DeviceCommand> findTop20ByStatusAndSentAtBeforeOrderByCreatedAtAsc(CommandStatus status, LocalDateTime sentAtBefore);
    List<DeviceCommand> findTop50ByStatusInAndExpiresAtBeforeOrderByCreatedAtAsc(List<CommandStatus> statuses, LocalDateTime expiresAtBefore);
    List<DeviceCommand> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    List<DeviceCommand> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(String deviceId, LocalDateTime from, LocalDateTime to);
    Optional<DeviceCommand> findTopByDeviceIdAndRelayNoAndActionOrderByCreatedAtDesc(String deviceId, Integer relayNo, RelayAction action);
    Optional<DeviceCommand> findTopByDeviceIdAndRelayNoOrderByCreatedAtDesc(String deviceId, Integer relayNo);
    long countByDeviceId(String deviceId);
}
