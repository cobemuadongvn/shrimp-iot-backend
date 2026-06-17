package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.DeviceLatestStateResponse;
import com.example.shrimpiot.model.DeviceLatestState;
import com.example.shrimpiot.model.SensorReading;
import com.example.shrimpiot.repository.DeviceLatestStateRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceLatestStateService {
    private final DeviceLatestStateRepository repository;

    public DeviceLatestStateService(DeviceLatestStateRepository repository) {
        this.repository = repository;
    }

    public void updateFromReading(SensorReading reading) {
        DeviceLatestState state = repository.findById(reading.getDeviceId())
                .orElseGet(() -> DeviceLatestState.fromReading(reading));
        state.applyReading(reading);
        repository.save(state);
    }

    public DeviceLatestStateResponse getLatestState(String deviceId) {
        DeviceLatestState state = repository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("No latest state found for deviceId: " + deviceId));
        return new DeviceLatestStateResponse(state);
    }
}
