package com.example.shrimpiot.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.shrimpiot.model.ControlScenario;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.model.SensorReading;

@Service
public class AutoControlService {
    private final CommandService commandService;
    private final ControlScenarioService scenarioService;

    @Value("${auto-control.enabled}")
    private boolean enabled;

    @Value("${auto-control.cooldown-seconds}")
    private long defaultCooldownSeconds;

    public AutoControlService(CommandService commandService, ControlScenarioService scenarioService) {
        this.commandService = commandService;
        this.scenarioService = scenarioService;
    }

    public void applyAutoControl(SensorReading reading) {
        if (!enabled) return;
        String deviceId = reading.getDeviceId();

        List<ControlScenario> scenarios = scenarioService.getActiveScenariosForDevice(deviceId);
        for (ControlScenario s : scenarios) {
            Double value = extractParameterValue(reading, s.getConditionParameter());
            if (value == null) continue;

            boolean conditionTrue = evaluateCondition(value, s.getOperator(), s.getThresholdValue());
            long cooldown = s.getCooldownSeconds() != null ? s.getCooldownSeconds() : defaultCooldownSeconds;

            if (conditionTrue) {
                // Apply configured action
                if (s.getAction() == RelayAction.ON) {
                    createCommandIfNotInCooldown(deviceId, s.getRelayNo(), RelayAction.ON, "SCENARIO:" + s.getId() + " condition met", cooldown);
                } else {
                    createCommandIfNotInCooldown(deviceId, s.getRelayNo(), RelayAction.OFF, "SCENARIO:" + s.getId() + " condition met", cooldown);
                }
            } else {
                // If scenario was to turn ON, when condition clears we may want to turn it OFF
                if (s.getAction() == RelayAction.ON) {
                    createAutoOffIfPreviouslyOn(deviceId, s.getRelayNo(), RelayAction.OFF, "SCENARIO:" + s.getId() + " condition cleared", cooldown);
                }
                // If scenario action was OFF and condition clears, could consider turning ON back — skip for now
            }
        }
    }

    private Double extractParameterValue(SensorReading reading, String parameter) {
        if (parameter == null) return null;
        switch (parameter.trim().toUpperCase()) {
            case "DO":
            case "DISSOLVED_OXYGEN":
                return reading.getDoValue();
            case "TEMPERATURE":
            case "TEMP":
                return reading.getTemperature();
            case "PH":
                return reading.getPh();
            case "SALINITY":
                return reading.getSalinity();
            case "EC":
                return reading.getEcValue();
            default:
                return null;
        }
    }

    private boolean evaluateCondition(Double value, String operator, Double threshold) {
        if (value == null || operator == null || threshold == null) return false;
        return switch (operator.trim()) {
            case "<" -> value < threshold;
            case "<=" -> value <= threshold;
            case ">" -> value > threshold;
            case ">=" -> value >= threshold;
            default -> false;
        };
    }

    private void createCommandIfNotInCooldown(String deviceId, int relayNo, RelayAction action, String reason, long cooldownSeconds) {
        if (action == RelayAction.ON && commandService.isRelayCurrentlyOn(deviceId, relayNo)) return;
        if (action == RelayAction.OFF && commandService.isRelayCurrentlyOff(deviceId, relayNo)) return;
        if (commandService.hasRecentCommand(deviceId, relayNo, action, cooldownSeconds)) return;
        commandService.createAutoCommand(deviceId, relayNo, action, reason);
    }

    private void createAutoOffIfPreviouslyOn(String deviceId, int relayNo, RelayAction action, String reason, long cooldownSeconds) {
        if (commandService.isRelayCurrentlyOff(deviceId, relayNo)) return;
        if (commandService.hasRecentCommand(deviceId, relayNo, action, cooldownSeconds)) return;
        if (!commandService.wasLastCommandOn(deviceId, relayNo)) return;
        commandService.createAutoCommand(deviceId, relayNo, action, reason);
    }
}
