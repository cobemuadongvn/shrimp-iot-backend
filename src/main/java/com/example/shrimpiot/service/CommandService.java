package com.example.shrimpiot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.shrimpiot.dto.CommandAckRequest;
import com.example.shrimpiot.dto.CommandRequest;
import com.example.shrimpiot.dto.CommandResponse;
import com.example.shrimpiot.model.AlertType;
import com.example.shrimpiot.model.AlertSeverity;
import com.example.shrimpiot.model.CommandStatus;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.DeviceCommand;
import com.example.shrimpiot.model.DeviceRelay;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.repository.DeviceCommandRepository;
import com.example.shrimpiot.repository.DeviceRelayRepository;
import com.example.shrimpiot.repository.DeviceRepository;

@Service
public class CommandService {
    private final DeviceCommandRepository repository;
    private final ApiKeyService apiKeyService;
    private final RelayStateService relayStateService;
    private final DeviceRepository deviceRepository;
    private final DeviceRelayRepository deviceRelayRepository;
    private final AlertService alertService;
    private final ObjectProvider<MqttCommandPublisher> mqttCommandPublisherProvider;

    @Value("${command.default-expiration-seconds:60}")
    private long defaultExpirationSeconds;

    public CommandService(
            DeviceCommandRepository repository,
            ApiKeyService apiKeyService,
            RelayStateService relayStateService,
            DeviceRepository deviceRepository,
            DeviceRelayRepository deviceRelayRepository,
            AlertService alertService,
            ObjectProvider<MqttCommandPublisher> mqttCommandPublisherProvider
    ) {
        this.repository = repository;
        this.apiKeyService = apiKeyService;
        this.relayStateService = relayStateService;
        this.deviceRepository = deviceRepository;
        this.deviceRelayRepository = deviceRelayRepository;
        this.alertService = alertService;
        this.mqttCommandPublisherProvider = mqttCommandPublisherProvider;
    }

    public CommandResponse createCommand(CommandRequest request) {
        return createCommand(request, "APP");
    }

    public CommandResponse createCommand(CommandRequest request, String requestedBy) {
        RelayAction action = parseAction(request.getAction());
        ensureRelayCanReceiveCommand(request.getDeviceId(), request.getRelayNo());
        DeviceCommand command = buildCommand(
                request.getDeviceId(),
                request.getRelayNo(),
                action,
                "MANUAL",
                requestedBy == null ? "APP" : requestedBy,
                "Manual command created"
        );

        DeviceCommand saved = repository.save(command);
        dispatchCommand(saved);
        return new CommandResponse(saved);
    }

    public DeviceCommand createAutoCommand(String deviceId, int relayNo, RelayAction action, String reason) {
        ensureRelayCanReceiveCommand(deviceId, relayNo);
        DeviceCommand cmd = repository.save(buildCommand(deviceId, relayNo, action, "AUTO", "SYSTEM", reason));

        // Associate command id with relay state if exists
        try {
            com.example.shrimpiot.model.RelayState state = relayStateService.getOrCreateRelayState(deviceId, relayNo, null);
            state.setLastCommandId(cmd.getId());
            relayStateService.updateRelayState(deviceId, relayNo, state.getCurrentState(), cmd.getId());
        } catch (Exception ignored) {}

        dispatchCommand(cmd);
        return cmd;
    }

    private DeviceCommand buildCommand(String deviceId, int relayNo, RelayAction action, String source, String requestedBy, String message) {
        DeviceCommand command = new DeviceCommand();
        command.setDeviceId(deviceId);
        command.setRelayNo(relayNo);
        command.setAction(action);
        command.setStatus(CommandStatus.PENDING);
        command.setSource(source);
        command.setRequestedBy(requestedBy);
        command.setMessage(message);
        command.setExpiresAt(LocalDateTime.now().plusSeconds(Math.max(5, defaultExpirationSeconds)));
        return command;
    }

    private void dispatchCommand(DeviceCommand command) {
        MqttCommandPublisher publisher = mqttCommandPublisherProvider.getIfAvailable();

        if (publisher == null) {
            command.setStatus(CommandStatus.PENDING);
            command.setMessage("MQTT disabled or publisher unavailable. Command remains pending for HTTP fallback.");
            repository.save(command);
            return;
        }

        try {
            publisher.publishCommand(command);
            command.setStatus(CommandStatus.SENT);
            command.setSentAt(LocalDateTime.now());
            command.setMessage("Command sent to device via MQTT");
            repository.save(command);
        } catch (Exception e) {
            command.setStatus(CommandStatus.PENDING);
            command.setMessage("MQTT publish failed. Command remains pending: " + e.getMessage());
            repository.save(command);
        }
    }

    /**
     * HTTP fallback: device can still poll pending commands with X-API-Key if MQTT is not available.
     */
    public List<CommandResponse> getPendingCommands(String deviceId, String apiKey) {
        apiKeyService.validate(apiKey);
        markDeviceSeenFromTrustedChannel(deviceId);

        expireStaleCommands();
        List<DeviceCommand> pendingCommands = repository.findByDeviceIdAndStatusOrderByCreatedAtAsc(deviceId, CommandStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        for (DeviceCommand command : pendingCommands) {
            command.setStatus(CommandStatus.SENT);
            command.setSentAt(now);
            command.setMessage("Command sent to device via HTTP fallback");
        }
        return repository.saveAll(pendingCommands).stream().map(CommandResponse::new).toList();
    }

    public CommandResponse acknowledge(Long id, CommandAckRequest request, String apiKey) {
        apiKeyService.validate(apiKey);
        return acknowledgeInternal(id, request, null);
    }

    public CommandResponse acknowledgeFromTrustedDevice(String expectedDeviceId, Long id, CommandAckRequest request) {
        return acknowledgeInternal(id, request, expectedDeviceId);
    }

    private CommandResponse acknowledgeInternal(Long id, CommandAckRequest request, String expectedDeviceId) {
        DeviceCommand command = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Command not found: " + id));

        if (expectedDeviceId != null && !expectedDeviceId.equals(command.getDeviceId())) {
            throw new IllegalArgumentException("ACK deviceId does not match command deviceId");
        }

        LocalDateTime now = LocalDateTime.now();
        if (command.getExpiresAt() != null && command.getExpiresAt().isBefore(now) && command.getStatus() != CommandStatus.ACK) {
            command.setStatus(CommandStatus.EXPIRED);
            command.setMessage("ACK ignored because command expired before device confirmation");
            repository.save(command);
            throw new IllegalArgumentException("Command expired before ACK: " + id);
        }

        command.setAckAt(now);
        markDeviceSeenFromTrustedChannel(command.getDeviceId());

        if (request.isSuccess()) {
            command.setStatus(CommandStatus.ACK);
            command.setMessage(request.getMessage() == null ? "Command executed successfully" : request.getMessage());
            relayStateService.updateFromCommand(command);
        } else {
            command.setStatus(CommandStatus.FAILED);
            command.setMessage(request.getMessage() == null ? "Command execution failed" : request.getMessage());
        }

        return new CommandResponse(repository.save(command));
    }

    public void markDeviceSeenFromTrustedChannel(String deviceId) {
        deviceRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE")
                .filter(this::isRealDevice)
                .ifPresent(device -> {
                    device.setLastSeenAt(LocalDateTime.now());
                    device.setConnectionStatus("ONLINE");
                    deviceRepository.save(device);
                    alertService.autoResolveIfOpen(device.getDeviceId(), AlertType.DEVICE_OFFLINE);
                });
    }


    public void updateDeviceStatusFromTrustedChannel(String deviceId, String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        deviceRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE")
                .filter(this::isRealDevice)
                .ifPresent(device -> {
                    if ("OFFLINE".equals(status)) {
                        device.setConnectionStatus("OFFLINE");
                        deviceRepository.save(device);

                        // MQTT Last Will or device status topic reported OFFLINE.
                        // Create DEVICE_OFFLINE alert immediately instead of waiting for the scheduler timeout.
                        // If the alert is already OPEN, AlertService + NotificationService anti-spam will only log/suppress duplicates.
                        alertService.openAlertIfMissing(
                                device.getDeviceId(),
                                AlertType.DEVICE_OFFLINE,
                                AlertSeverity.WARNING,
                                "Thiết bị mất kết nối"
                        );
                        return;
                    }

                    device.setLastSeenAt(LocalDateTime.now());
                    device.setConnectionStatus("ONLINE");
                    deviceRepository.save(device);
                    alertService.autoResolveIfOpen(device.getDeviceId(), AlertType.DEVICE_OFFLINE);
                });
    }

    public List<CommandResponse> getCommandHistory(String deviceId) {
        return repository.findByDeviceIdOrderByCreatedAtDesc(deviceId).stream().map(CommandResponse::new).toList();
    }

    public boolean hasRecentCommand(String deviceId, int relayNo, RelayAction action, long cooldownSeconds) {
        return repository.findTopByDeviceIdAndRelayNoAndActionOrderByCreatedAtDesc(deviceId, relayNo, action)
                .map(command -> command.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(cooldownSeconds)))
                .orElse(false);
    }

    public long countByDeviceId(String deviceId) { return repository.countByDeviceId(deviceId); }

    public boolean wasLastCommandOn(String deviceId, int relayNo) {
        return repository.findTopByDeviceIdAndRelayNoOrderByCreatedAtDesc(deviceId, relayNo)
                .map(command -> command.getAction() == RelayAction.ON)
                .orElse(false);
    }

    public void expireStaleCommands() {
        LocalDateTime now = LocalDateTime.now();
        List<DeviceCommand> expired = repository.findTop50ByStatusInAndExpiresAtBeforeOrderByCreatedAtAsc(
                List.of(CommandStatus.PENDING, CommandStatus.SENT),
                now
        );
        for (DeviceCommand command : expired) {
            command.setStatus(CommandStatus.EXPIRED);
            command.setMessage("Command expired before device ACK. It will not be executed/retried.");
        }
        if (!expired.isEmpty()) {
            repository.saveAll(expired);
        }
    }

    /**
     * Kiểm tra relay hiện đang ON (từ RelayState)
     */
    public boolean isRelayCurrentlyOn(String deviceId, int relayNo) {
        return relayStateService.isRelayOn(deviceId, relayNo);
    }

    /**
     * Kiểm tra relay hiện đang OFF (từ RelayState)
     */
    public boolean isRelayCurrentlyOff(String deviceId, int relayNo) {
        return relayStateService.isRelayOff(deviceId, relayNo);
    }



    public boolean isRelayLocked(String deviceId, Integer relayNo) {
        return deviceRelayRepository.findByDeviceDeviceIdAndRelayNo(deviceId, relayNo)
                .map(DeviceRelay::isLocked)
                .orElse(false);
    }

    public int expirePendingCommandsForRelay(String deviceId, Integer relayNo, String reason) {
        List<DeviceCommand> commands = repository.findByDeviceIdAndRelayNoAndStatusInOrderByCreatedAtAsc(
                deviceId,
                relayNo,
                List.of(CommandStatus.PENDING, CommandStatus.SENT)
        );
        for (DeviceCommand command : commands) {
            command.setStatus(CommandStatus.EXPIRED);
            command.setMessage(reason == null ? "Command expired because relay was locked" : reason);
        }
        if (!commands.isEmpty()) {
            repository.saveAll(commands);
        }
        return commands.size();
    }

    private void ensureRelayCanReceiveCommand(String deviceId, Integer relayNo) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        if (relayNo == null || relayNo < 1 || relayNo > 4) {
            throw new IllegalArgumentException("relayNo must be from 1 to 4");
        }

        Device device = deviceRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE")
                .filter(this::isRealDevice)
                .orElseThrow(() -> new IllegalArgumentException("Device not found or not active: " + deviceId));

        DeviceRelay relay = deviceRelayRepository.findByDeviceDeviceIdAndRelayNo(device.getDeviceId(), relayNo)
                .orElseThrow(() -> new IllegalArgumentException("Relay not found: device=" + deviceId + ", relay=" + relayNo));

        if (relay.isLocked()) {
            throw new SecurityException("Relay " + relayNo + " of device " + deviceId + " is locked. Please unlock it before sending ON/OFF commands.");
        }
    }

    private RelayAction parseAction(String action) {
        try { return RelayAction.valueOf(action.trim().toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("action must be ON or OFF"); }
    }

    private boolean isRealDevice(Device device) {
        if (device == null || device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            return false;
        }
        String normalized = device.getDeviceId().trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("pond_");
    }
}
