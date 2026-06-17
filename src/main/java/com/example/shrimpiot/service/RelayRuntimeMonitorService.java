package com.example.shrimpiot.service;

import com.example.shrimpiot.model.AlertSeverity;
import com.example.shrimpiot.model.AlertType;
import com.example.shrimpiot.model.DeviceCommand;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.model.RelayState;
import com.example.shrimpiot.repository.DeviceCommandRepository;
import com.example.shrimpiot.repository.RelayStateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RelayRuntimeMonitorService {
    private final RelayStateRepository relayStateRepository;
    private final DeviceCommandRepository deviceCommandRepository;
    private final ControlScenarioService scenarioService;
    private final CommandService commandService;
    private final AlertService alertService;

    public RelayRuntimeMonitorService(RelayStateRepository relayStateRepository,
                                      DeviceCommandRepository deviceCommandRepository,
                                      ControlScenarioService scenarioService,
                                      CommandService commandService,
                                      AlertService alertService) {
        this.relayStateRepository = relayStateRepository;
        this.deviceCommandRepository = deviceCommandRepository;
        this.scenarioService = scenarioService;
        this.commandService = commandService;
        this.alertService = alertService;
    }

    // Run every minute
    @Scheduled(fixedDelayString = "60000")
    public void checkLongRunningRelays() {
        List<RelayState> states = relayStateRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (RelayState state : states) {
            if (state.getCurrentState() != RelayAction.ON) continue;

            Long maxRuntime = findMaxRuntimeForRelay(state);
            if (maxRuntime == null || maxRuntime <= 0) continue;

                // Find the command that turned it ON
            DeviceCommand lastOn = deviceCommandRepository.findTopByDeviceIdAndRelayNoAndActionOrderByCreatedAtDesc(
                    state.getDeviceId(), state.getRelayNo(), RelayAction.ON
            ).orElse(null);

            if (lastOn == null) continue;

            LocalDateTime onAt = lastOn.getCreatedAt();
            if (onAt == null) onAt = lastOn.getCreatedAt();
            long elapsedSeconds = java.time.Duration.between(onAt, now).getSeconds();
            if (elapsedSeconds > maxRuntime) {
                // create OFF command
                DeviceCommand offCommand = commandService.createAutoCommand(state.getDeviceId(), state.getRelayNo(), RelayAction.OFF, "AUTO-OFF: exceeded max runtime");
                // create alert
                alertService.openAlertIfMissing(state.getDeviceId(), AlertType.DEVICE_OVER_RUNTIME, AlertSeverity.DANGER, "Thiết bị chạy quá lâu: relay " + state.getRelayNo());
                // update relay state as OFF optimistically
                state.setCurrentState(RelayAction.OFF);
                state.setLastCommandId(offCommand.getId());
                relayStateRepository.save(state);
            }
        }
    }

    private Long findMaxRuntimeForRelay(RelayState state) {
        // Priority: control scenarios -> default values per relay type (simple mapping)
        // Try to get from control scenarios by pond via CommandService -> Device -> Pond
        try {
            List<com.example.shrimpiot.model.ControlScenario> scenarios = scenarioService.getActiveScenariosForDevice(state.getDeviceId());
            if (scenarios != null) {
                for (com.example.shrimpiot.model.ControlScenario s : scenarios) {
                    if (s.getRelayNo() != null && s.getRelayNo().equals(state.getRelayNo()) && s.getAction() == RelayAction.ON && s.getMaxRuntimeSeconds() != null) {
                        if (s.getAutoOffEnabled() != null && !s.getAutoOffEnabled()) continue;
                        return s.getMaxRuntimeSeconds();
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback defaults by relay name (basic)
        String name = state.getRelayName() != null ? state.getRelayName().toLowerCase() : "";
        if (name.contains("bơm") || name.contains("bom") || name.contains("pump")) return 10L * 60L; // 10 minutes
        if (name.contains("oxy") || name.contains("sục") || name.contains("oxygen")) return 30L * 60L; // 30 minutes
        if (name.contains("quạt") || name.contains("fan")) return 30L * 60L; // 30 minutes
        return null;
    }
}
