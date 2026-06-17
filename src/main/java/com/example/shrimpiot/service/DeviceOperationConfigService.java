package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.MeasurementConfigRequest;
import com.example.shrimpiot.dto.OperationConfigResponse;
import com.example.shrimpiot.dto.OperationModeRequest;
import com.example.shrimpiot.dto.SalinityControlConfigRequest;
import com.example.shrimpiot.model.DeviceOperationConfig;
import com.example.shrimpiot.model.OperationMode;
import com.example.shrimpiot.repository.DeviceOperationConfigRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceOperationConfigService {
    private final DeviceOperationConfigRepository repository;
    private final DeviceRepository deviceRepository;

    public DeviceOperationConfigService(DeviceOperationConfigRepository repository, DeviceRepository deviceRepository) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
    }

    public DeviceOperationConfig getOrCreate(String deviceId) {
        deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        return repository.findByDeviceId(deviceId).map(config -> {
            applyScientificDefaults(config);
            return repository.save(config);
        }).orElseGet(() -> {
            DeviceOperationConfig config = new DeviceOperationConfig();
            config.setDeviceId(deviceId);
            config.setOperationMode(OperationMode.MANUAL);
            config.setSalinityAutoEnabled(false);
            applyScientificDefaults(config);
            config.setUpdatedBy("SYSTEM_DEFAULT");
            return repository.save(config);
        });
    }

    public OperationConfigResponse getResponse(String deviceId) {
        return new OperationConfigResponse(getOrCreate(deviceId));
    }

    public OperationConfigResponse updateOperationMode(String deviceId, OperationModeRequest request, String updatedBy) {
        DeviceOperationConfig config = getOrCreate(deviceId);
        OperationMode mode = parseMode(request.getOperationMode());
        config.setOperationMode(mode);
        config.setSalinityAutoEnabled(mode == OperationMode.AI_AUTO);
        config.setUpdatedBy(updatedBy);
        return new OperationConfigResponse(repository.save(config));
    }

    public OperationConfigResponse updateSalinityConfig(String deviceId, SalinityControlConfigRequest request, String updatedBy) {
        DeviceOperationConfig config = getOrCreate(deviceId);
        if (request.getSalinityAutoEnabled() != null) config.setSalinityAutoEnabled(request.getSalinityAutoEnabled());
        if (request.getSalinityHighThreshold() != null) config.setSalinityHighThreshold(request.getSalinityHighThreshold());
        if (request.getSalinityStopThreshold() != null) config.setSalinityStopThreshold(request.getSalinityStopThreshold());
        if (request.getSalinityDrainDurationSeconds() != null) config.setSalinityDrainDurationSeconds(request.getSalinityDrainDurationSeconds());
        if (request.getFreshwaterDurationSeconds() != null) config.setFreshwaterDurationSeconds(request.getFreshwaterDurationSeconds());
        if (request.getMixingWaitSeconds() != null) config.setMixingWaitSeconds(request.getMixingWaitSeconds());
        if (request.getMaxRetryCount() != null) config.setMaxRetryCount(request.getMaxRetryCount());
        if (request.getCooldownMinutes() != null) config.setCooldownMinutes(request.getCooldownMinutes());
        if (request.getReadingMaxAgeSeconds() != null) config.setReadingMaxAgeSeconds(request.getReadingMaxAgeSeconds());
        if (request.getAutoRemeasureEnabled() != null) config.setAutoRemeasureEnabled(request.getAutoRemeasureEnabled());
        if (request.getSafetyLockEnabled() != null) config.setSafetyLockEnabled(request.getSafetyLockEnabled());
        validateConfig(config);
        config.setUpdatedBy(updatedBy);
        return new OperationConfigResponse(repository.save(config));
    }

    public OperationConfigResponse updateMeasurementConfig(String deviceId, MeasurementConfigRequest request, String updatedBy) {
        DeviceOperationConfig config = getOrCreate(deviceId);
        if (request.getFillDurationSeconds() != null) config.setFillDurationSeconds(request.getFillDurationSeconds());
        if (request.getStabilizingSeconds() != null) config.setStabilizingSeconds(request.getStabilizingSeconds());
        if (request.getMeasurementDurationSeconds() != null) config.setMeasurementDurationSeconds(request.getMeasurementDurationSeconds());
        if (request.getMeasurementDrainDurationSeconds() != null) config.setMeasurementDrainDurationSeconds(request.getMeasurementDrainDurationSeconds());
        validateConfig(config);
        config.setUpdatedBy(updatedBy);
        return new OperationConfigResponse(repository.save(config));
    }

    private OperationMode parseMode(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("operationMode is required: MANUAL or AI_AUTO");
        try { return OperationMode.valueOf(raw.trim().toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("operationMode must be MANUAL or AI_AUTO"); }
    }

    /**
     * Bộ mặc định phù hợp cho mô hình đồ án: an toàn, đủ trực quan khi demo và có thể chỉnh trên web/app.
     * Khi triển khai ao thật, các thời lượng bơm phải được hiệu chuẩn theo thể tích ao, công suất bơm và độ mặn nguồn nước.
     */
    private void applyScientificDefaults(DeviceOperationConfig config) {
        if (config.getSalinityHighThreshold() == null) config.setSalinityHighThreshold(35.0);
        if (config.getSalinityStopThreshold() == null) config.setSalinityStopThreshold(32.0);
        if (config.getFillDurationSeconds() == null) config.setFillDurationSeconds(20);
        if (config.getStabilizingSeconds() == null) config.setStabilizingSeconds(45);
        if (config.getMeasurementDurationSeconds() == null) config.setMeasurementDurationSeconds(30);
        if (config.getMeasurementDrainDurationSeconds() == null) config.setMeasurementDrainDurationSeconds(20);
        if (config.getSalinityDrainDurationSeconds() == null) config.setSalinityDrainDurationSeconds(20);
        if (config.getFreshwaterDurationSeconds() == null) config.setFreshwaterDurationSeconds(25);
        if (config.getMixingWaitSeconds() == null) config.setMixingWaitSeconds(120);
        if (config.getMaxRetryCount() == null) config.setMaxRetryCount(2);
        if (config.getCooldownMinutes() == null) config.setCooldownMinutes(10);
        if (config.getReadingMaxAgeSeconds() == null) config.setReadingMaxAgeSeconds(120);
        if (config.getAutoRemeasureEnabled() == null) config.setAutoRemeasureEnabled(true);
        if (config.getSafetyLockEnabled() == null) config.setSafetyLockEnabled(false);
    }

    private void validateConfig(DeviceOperationConfig config) {
        applyScientificDefaults(config);
        if (config.getSalinityHighThreshold() <= config.getSalinityStopThreshold()) {
            throw new IllegalArgumentException("salinityHighThreshold must be greater than salinityStopThreshold");
        }
        validateDuration(config.getFillDurationSeconds(), "fillDurationSeconds");
        validateDuration(config.getStabilizingSeconds(), "stabilizingSeconds");
        validateDuration(config.getMeasurementDurationSeconds(), "measurementDurationSeconds");
        validateDuration(config.getMeasurementDrainDurationSeconds(), "measurementDrainDurationSeconds");
        validateDuration(config.getSalinityDrainDurationSeconds(), "salinityDrainDurationSeconds");
        validateDuration(config.getFreshwaterDurationSeconds(), "freshwaterDurationSeconds");
        validateDuration(config.getMixingWaitSeconds(), "mixingWaitSeconds");
        if (config.getSalinityHighThreshold() < 0 || config.getSalinityHighThreshold() > 100) {
            throw new IllegalArgumentException("salinityHighThreshold must be between 0 and 100‰");
        }
        if (config.getSalinityStopThreshold() < 0 || config.getSalinityStopThreshold() > 100) {
            throw new IllegalArgumentException("salinityStopThreshold must be between 0 and 100‰");
        }
        if ((config.getSalinityHighThreshold() - config.getSalinityStopThreshold()) < 1.0) {
            throw new IllegalArgumentException("salinityHighThreshold should be at least 1‰ greater than salinityStopThreshold to avoid relay oscillation");
        }
        if (config.getMaxRetryCount() < 1 || config.getMaxRetryCount() > 5) {
            throw new IllegalArgumentException("maxRetryCount must be between 1 and 5");
        }
        if (config.getCooldownMinutes() < 1 || config.getCooldownMinutes() > 1440) {
            throw new IllegalArgumentException("cooldownMinutes must be between 1 and 1440");
        }
        if (config.getReadingMaxAgeSeconds() < 10 || config.getReadingMaxAgeSeconds() > 3600) {
            throw new IllegalArgumentException("readingMaxAgeSeconds must be between 10 and 3600 seconds");
        }
    }

    private void validateDuration(Integer value, String field) {
        if (value == null || value < 1 || value > 3600) {
            throw new IllegalArgumentException(field + " must be between 1 and 3600 seconds");
        }
    }
}
