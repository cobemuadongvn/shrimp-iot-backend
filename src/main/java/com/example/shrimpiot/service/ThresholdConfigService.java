package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.ThresholdConfigRequest;
import com.example.shrimpiot.dto.ThresholdConfigResponse;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.ThresholdConfig;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.ThresholdConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ThresholdConfigService {

    private final ThresholdConfigRepository repository;
    private final PondRepository pondRepository;

    public ThresholdConfigService(
            ThresholdConfigRepository repository,
            PondRepository pondRepository
    ) {
        this.repository = repository;
        this.pondRepository = pondRepository;
    }

    public ThresholdConfigResponse createOrUpdateThreshold(ThresholdConfigRequest request, String username) {
        Pond pond = pondRepository.findById(request.getPondId())
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + request.getPondId()));

        ThresholdConfig config = repository.findByPondIdAndParameterName(request.getPondId(), request.getParameterName())
                .orElse(new ThresholdConfig());

        config.setPond(pond);
        config.setParameterName(request.getParameterName().toUpperCase());
        config.setMinValue(request.getMinValue());
        config.setMaxValue(request.getMaxValue());
        config.setSeverity(request.getSeverity() != null ? request.getSeverity() : "WARNING");
        config.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        config.setUpdatedBy(username);
        config.setUpdatedAt(LocalDateTime.now());

        ThresholdConfig saved = repository.save(config);
        return new ThresholdConfigResponse(saved);
    }

    public ThresholdConfigResponse getThreshold(Long pondId, String parameterName) {
        ThresholdConfig config = repository.findByPondIdAndParameterName(pondId, parameterName.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Threshold not found for pond: " + pondId + ", parameter: " + parameterName));
        return new ThresholdConfigResponse(config);
    }

    public List<ThresholdConfigResponse> getThresholdsByPond(Long pondId) {
        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        return repository.findByPond(pond).stream()
                .map(ThresholdConfigResponse::new)
                .toList();
    }

    public List<ThresholdConfigResponse> getEnabledThresholdsByPond(Long pondId) {
        return repository.findByPondIdAndEnabledTrue(pondId).stream()
                .map(ThresholdConfigResponse::new)
                .toList();
    }

    public void deleteThreshold(Long id) {
        ThresholdConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Threshold config not found: " + id));
        repository.delete(config);
    }

    public ThresholdConfig getThresholdConfigOrDefault(Long pondId, String parameterName, Double defaultMin, Double defaultMax) {
        return repository.findByPondIdAndParameterName(pondId, parameterName.toUpperCase())
                .orElseGet(() -> {
                    ThresholdConfig defaultConfig = new ThresholdConfig();
                    defaultConfig.setParameterName(parameterName.toUpperCase());
                    defaultConfig.setMinValue(defaultMin);
                    defaultConfig.setMaxValue(defaultMax);
                    defaultConfig.setSeverity("WARNING");
                    defaultConfig.setEnabled(true);
                    return defaultConfig;
                });
    }
}
