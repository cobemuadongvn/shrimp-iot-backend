package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.SensorCalibrationRequest;
import com.example.shrimpiot.dto.SensorCalibrationResponse;
import com.example.shrimpiot.dto.SensorReadingRequest;
import com.example.shrimpiot.model.SensorCalibration;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.SensorCalibrationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SensorCalibrationService {
    private final SensorCalibrationRepository repository;
    private final DeviceRepository deviceRepository;

    public SensorCalibrationService(SensorCalibrationRepository repository, DeviceRepository deviceRepository) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
    }

    public List<SensorCalibrationResponse> getCalibrations(String deviceId) {
        ensureDeviceExists(deviceId);
        return repository.findByDeviceIdOrderBySensorTypeAscCreatedAtDesc(deviceId)
                .stream()
                .map(SensorCalibrationResponse::new)
                .toList();
    }

    public SensorCalibrationResponse createCalibration(String deviceId, SensorCalibrationRequest request) {
        ensureDeviceExists(deviceId);
        SensorCalibration calibration = new SensorCalibration();
        applyRequest(calibration, deviceId, request);
        return new SensorCalibrationResponse(repository.save(calibration));
    }

    public SensorCalibrationResponse updateCalibration(String deviceId, Long calibrationId, SensorCalibrationRequest request) {
        ensureDeviceExists(deviceId);
        SensorCalibration calibration = repository.findById(calibrationId)
                .orElseThrow(() -> new IllegalArgumentException("Calibration not found: " + calibrationId));
        if (!deviceId.equals(calibration.getDeviceId())) {
            throw new SecurityException("Calibration does not belong to device: " + deviceId);
        }
        applyRequest(calibration, deviceId, request);
        return new SensorCalibrationResponse(repository.save(calibration));
    }

    public void deactivateCalibration(String deviceId, Long calibrationId) {
        ensureDeviceExists(deviceId);
        SensorCalibration calibration = repository.findById(calibrationId)
                .orElseThrow(() -> new IllegalArgumentException("Calibration not found: " + calibrationId));
        if (!deviceId.equals(calibration.getDeviceId())) {
            throw new SecurityException("Calibration does not belong to device: " + deviceId);
        }
        calibration.setActive(false);
        repository.save(calibration);
    }

    public SensorReadingRequest applyCalibration(SensorReadingRequest request) {
        SensorReadingRequest calibrated = new SensorReadingRequest();
        calibrated.setDeviceId(request.getDeviceId());
        calibrated.setTemperature(apply(request.getDeviceId(), "TEMPERATURE", request.getTemperature()));
        calibrated.setPh(apply(request.getDeviceId(), "PH", request.getPh()));
        calibrated.setEcValue(apply(request.getDeviceId(), "EC", request.getEcValue()));
        calibrated.setSalinity(apply(request.getDeviceId(), "SALINITY", request.getSalinity()));
        calibrated.setDoValue(apply(request.getDeviceId(), "DO", request.getDoValue()));
        return calibrated;
    }

    private Double apply(String deviceId, String sensorType, Double rawValue) {
        if (rawValue == null) return null;
        return repository.findTopByDeviceIdAndSensorTypeAndActiveTrueOrderByUpdatedAtDesc(deviceId, normalizeSensorType(sensorType))
                .map(calibration -> round(rawValue * calibration.getSlopeValue() + calibration.getOffsetValue()))
                .orElse(rawValue);
    }

    private void applyRequest(SensorCalibration calibration, String deviceId, SensorCalibrationRequest request) {
        String sensorType = normalizeSensorType(request.getSensorType());
        validateSensorType(sensorType);
        if (request.getSlopeValue() == null || request.getSlopeValue() == 0.0) {
            throw new IllegalArgumentException("slopeValue must not be 0");
        }
        calibration.setDeviceId(deviceId);
        calibration.setSensorType(sensorType);
        calibration.setOffsetValue(request.getOffsetValue());
        calibration.setSlopeValue(request.getSlopeValue());
        calibration.setCalibrationPoint1(request.getCalibrationPoint1());
        calibration.setCalibrationPoint2(request.getCalibrationPoint2());
        calibration.setNote(request.getNote());
        calibration.setActive(request.getActive() == null || request.getActive());
    }

    private void ensureDeviceExists(String deviceId) {
        if (!deviceRepository.existsByDeviceId(deviceId)) {
            throw new IllegalArgumentException("Device not found: " + deviceId);
        }
    }

    private String normalizeSensorType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sensorType is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("DISSOLVED_OXYGEN") || normalized.equals("DO_VALUE")) return "DO";
        if (normalized.equals("EC_VALUE")) return "EC";
        if (normalized.equals("TEMP")) return "TEMPERATURE";
        return normalized;
    }

    private void validateSensorType(String sensorType) {
        if (!(sensorType.equals("TEMPERATURE") || sensorType.equals("PH") || sensorType.equals("EC") || sensorType.equals("SALINITY") || sensorType.equals("DO"))) {
            throw new IllegalArgumentException("sensorType must be one of TEMPERATURE, PH, EC, SALINITY, DO");
        }
    }

    private Double round(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
