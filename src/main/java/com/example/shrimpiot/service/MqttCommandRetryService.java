package com.example.shrimpiot.service;

import com.example.shrimpiot.model.CommandStatus;
import com.example.shrimpiot.model.DeviceCommand;
import com.example.shrimpiot.repository.DeviceCommandRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnBean(MqttCommandPublisher.class)
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttCommandRetryService {

    private final DeviceCommandRepository repository;
    private final MqttCommandPublisher publisher;
    private final CommandService commandService;

    public MqttCommandRetryService(
            DeviceCommandRepository repository,
            MqttCommandPublisher publisher,
            CommandService commandService
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.commandService = commandService;
    }

    @Scheduled(fixedDelayString = "${mqtt.command-retry-ms:5000}")
    public void retryPendingCommands() {
        commandService.expireStaleCommands();

        // Only retry commands that were never published successfully.
        // Do NOT resend SENT commands. In manual relay control, resending an old ON command
        // after the user already sent OFF can make the relay turn on again unexpectedly
        // if the device ACK is delayed or not processed.
        List<DeviceCommand> pending = repository.findTop20ByStatusOrderByCreatedAtAsc(CommandStatus.PENDING);
        resend(pending, "Command sent to device via MQTT retry");
    }

    private void resend(List<DeviceCommand> commands, String message) {
        for (DeviceCommand command : commands) {
            if (command.getExpiresAt() != null && command.getExpiresAt().isBefore(LocalDateTime.now())) {
                continue;
            }
            if (commandService.isRelayLocked(command.getDeviceId(), command.getRelayNo())) {
                command.setStatus(CommandStatus.EXPIRED);
                command.setMessage("Command expired because relay is locked");
                repository.save(command);
                continue;
            }
            try {
                publisher.publishCommand(command);
                command.setStatus(CommandStatus.SENT);
                command.setSentAt(LocalDateTime.now());
                command.setMessage(message);
                repository.save(command);
            } catch (Exception ignored) {
                // Keep current status so the next scheduler run can retry.
            }
        }
    }
}
