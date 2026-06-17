package com.example.shrimpiot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.shrimpiot.dto.ControlScenarioRequest;
import com.example.shrimpiot.dto.ControlScenarioResponse;
import com.example.shrimpiot.model.ControlScenario;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.RelayAction;
import com.example.shrimpiot.repository.ControlScenarioRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.PondRepository;

@Service
public class ControlScenarioService {
    private final ControlScenarioRepository repository;
    private final DeviceRepository deviceRepository;
    private final PondRepository pondRepository;

    public ControlScenarioService(ControlScenarioRepository repository, DeviceRepository deviceRepository, PondRepository pondRepository) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
        this.pondRepository = pondRepository;
    }

    public ControlScenarioResponse createScenario(ControlScenarioRequest request) {
        Pond pond = pondRepository.findById(request.getPondId())
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + request.getPondId()));

        ControlScenario scenario = new ControlScenario();
        scenario.setPond(pond);
        scenario.setConditionParameter(request.getConditionParameter());
        scenario.setOperator(request.getOperator());
        scenario.setThresholdValue(request.getThresholdValue());
        scenario.setRelayNo(request.getRelayNo());
        scenario.setAction(parseAction(request.getAction()));
        scenario.setCooldownSeconds(request.getCooldownSeconds());
        scenario.setMaxRuntimeSeconds(request.getMaxRuntimeSeconds());
        scenario.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        scenario.setAutoOffEnabled(request.getAutoOffEnabled() != null ? request.getAutoOffEnabled() : true);

        return new ControlScenarioResponse(repository.save(scenario));
    }

    public ControlScenarioResponse updateScenario(Long id, ControlScenarioRequest request) {
        ControlScenario existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Control scenario not found: " + id));

        Pond pond = pondRepository.findById(request.getPondId())
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + request.getPondId()));

        existing.setPond(pond);
        existing.setConditionParameter(request.getConditionParameter());
        existing.setOperator(request.getOperator());
        existing.setThresholdValue(request.getThresholdValue());
        existing.setRelayNo(request.getRelayNo());
        existing.setAction(parseAction(request.getAction()));
        existing.setCooldownSeconds(request.getCooldownSeconds());
        existing.setMaxRuntimeSeconds(request.getMaxRuntimeSeconds());
        existing.setEnabled(request.getEnabled() != null ? request.getEnabled() : existing.getEnabled());
        existing.setAutoOffEnabled(request.getAutoOffEnabled() != null ? request.getAutoOffEnabled() : existing.getAutoOffEnabled());

        return new ControlScenarioResponse(repository.save(existing));
    }

    public ControlScenarioResponse getScenario(Long id) {
        return repository.findById(id)
                .map(ControlScenarioResponse::new)
                .orElseThrow(() -> new IllegalArgumentException("Control scenario not found: " + id));
    }

    public List<ControlScenarioResponse> getScenariosByPond(Long pondId) {
        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        return repository.findByPondAndEnabledTrue(pond).stream()
                .map(ControlScenarioResponse::new)
                .toList();
    }

    public void deleteScenario(Long id) {
        ControlScenario existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Control scenario not found: " + id));
        repository.delete(existing);
    }

    public List<ControlScenario> getActiveScenariosForPond(Long pondId) {
        return repository.findByPondIdAndEnabledTrue(pondId);
    }

    public List<ControlScenario> getActiveScenariosForDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        if (device.getPond() == null) return List.of();
        return getActiveScenariosForPond(device.getPond().getId());
    }

    private RelayAction parseAction(String action) {
        try {
            return RelayAction.valueOf(action.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Action must be ON or OFF");
        }
    }
}
