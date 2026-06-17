package com.example.shrimpiot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.shrimpiot.dto.AiPredictionResponse;
import com.example.shrimpiot.dto.SensorReadingRequest;
import com.example.shrimpiot.dto.SensorReadingResponse;
import com.example.shrimpiot.model.AlertSeverity;
import com.example.shrimpiot.model.AlertType;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.ReadingStatus;
import com.example.shrimpiot.model.SensorReading;
import com.example.shrimpiot.model.ThresholdConfig;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.SensorReadingRepository;

@Service
public class SensorReadingService {
    private final SensorReadingRepository repository;
    private final ApiKeyService apiKeyService;
    private final AlertService alertService;
    private final AutoControlService autoControlService;
    private final DeviceRepository deviceRepository;
    private final ThresholdConfigService thresholdConfigService;
    private final WebSocketEventService webSocketEventService;
    private final SalinityControlService salinityControlService;
    private final SensorCalibrationService sensorCalibrationService;
    private final DeviceLatestStateService deviceLatestStateService;
    private final AiPredictionService aiPredictionService;

    @Value("${threshold.temperature.min}") private double temperatureMin;
    @Value("${threshold.temperature.max}") private double temperatureMax;
    @Value("${threshold.ph.min}") private double phMin;
    @Value("${threshold.ph.max}") private double phMax;
    @Value("${threshold.salinity.min}") private double salinityMin;
    @Value("${threshold.salinity.max}") private double salinityMax;
    @Value("${threshold.dissolved-oxygen.min}") private double doMin;

    public SensorReadingService(
            SensorReadingRepository repository,
            ApiKeyService apiKeyService,
            AlertService alertService,
            AutoControlService autoControlService,
            DeviceRepository deviceRepository,
            ThresholdConfigService thresholdConfigService,
            WebSocketEventService webSocketEventService,
            SalinityControlService salinityControlService,
            SensorCalibrationService sensorCalibrationService,
            DeviceLatestStateService deviceLatestStateService,
            AiPredictionService aiPredictionService
    ) {
        this.repository = repository;
        this.apiKeyService = apiKeyService;
        this.alertService = alertService;
        this.autoControlService = autoControlService;
        this.deviceRepository = deviceRepository;
        this.thresholdConfigService = thresholdConfigService;
        this.webSocketEventService = webSocketEventService;
        this.salinityControlService = salinityControlService;
        this.sensorCalibrationService = sensorCalibrationService;
        this.deviceLatestStateService = deviceLatestStateService;
        this.aiPredictionService = aiPredictionService;
    }

    public SensorReadingResponse saveReading(SensorReadingRequest request, String apiKey) {
        apiKeyService.validate(apiKey);
        return saveReadingFromTrustedDevice(request);
    }

    /**
     * Save telemetry received from a trusted channel such as MQTT.
     * HTTP API-key validation is intentionally skipped here because device authentication
     * should be handled by the MQTT broker, topic ACL, and backend-side topic validation.
     */
    public SensorReadingResponse saveReadingFromTrustedDevice(SensorReadingRequest request) {
        Device trustedDevice = requireActiveRealDevice(request.getDeviceId());

        SensorReadingRequest calibratedRequest = sensorCalibrationService.applyCalibration(request);
        validateSensorRange(calibratedRequest);

        // Update device last seen and mark ONLINE only for active physical devices.
        trustedDevice.setLastSeenAt(LocalDateTime.now());
        trustedDevice.setConnectionStatus("ONLINE");
        deviceRepository.save(trustedDevice);
        alertService.autoResolveIfOpen(trustedDevice.getDeviceId(), AlertType.DEVICE_OFFLINE);

        SensorReading reading = new SensorReading();
        reading.setDeviceId(calibratedRequest.getDeviceId());
        reading.setTemperature(calibratedRequest.getTemperature());
        reading.setPh(calibratedRequest.getPh());
        reading.setEcValue(calibratedRequest.getEcValue());
        reading.setSalinity(calibratedRequest.getSalinity());
        reading.setDoValue(calibratedRequest.getDoValue());

        applyRuleBasedStatusAndAlerts(reading);
        applyAiPredictionIfAvailable(reading);

        SensorReading saved = repository.save(reading);
        deviceLatestStateService.updateFromReading(saved);
        autoControlService.applyAutoControl(saved);
        salinityControlService.handleReading(saved);
        // publish to websocket
        try { webSocketEventService.publishSensorReading(new SensorReadingResponse(saved)); } catch (Exception ignored) {}
        return new SensorReadingResponse(saved);
    }

    public SensorReadingResponse getLatest(String deviceId) {
        SensorReading reading = repository.findTopByDeviceIdOrderByCreatedAtDesc(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("No data found for deviceId: " + deviceId));
        return new SensorReadingResponse(reading);
    }

    public List<SensorReadingResponse> getHistory(String deviceId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return repository.findByDeviceIdOrderByCreatedAtDesc(deviceId, PageRequest.of(0, safeLimit)).stream().map(SensorReadingResponse::new).toList();
    }

    public List<SensorReadingResponse> getRange(String deviceId, LocalDateTime from, LocalDateTime to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("to must be after from");
        return repository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtDesc(deviceId, from, to).stream().map(SensorReadingResponse::new).toList();
    }

    public long countByDeviceId(String deviceId) { return repository.countByDeviceId(deviceId); }
    public long countByStatus(String deviceId, ReadingStatus status) { return repository.countByDeviceIdAndStatus(deviceId, status); }

    private void validateSensorRange(SensorReadingRequest request) {
        if (request.getTemperature() < -20 || request.getTemperature() > 80) throw new IllegalArgumentException("temperature out of physical range");
        if (request.getPh() < 0 || request.getPh() > 14) throw new IllegalArgumentException("ph out of physical range");
        if (request.getSalinity() < 0 || request.getSalinity() > 100) throw new IllegalArgumentException("salinity out of physical range");
        if (request.getEcValue() != null && request.getEcValue() < 0) throw new IllegalArgumentException("ecValue out of physical range");
        if (request.getDoValue() != null && (request.getDoValue() < 0 || request.getDoValue() > 30)) throw new IllegalArgumentException("doValue out of physical range");
    }

    /**
     * Rule-based threshold layer based on QCVN 02-19:2014/BNNPTNT, Appendix 1, Table 1:
     * temperature 18-33°C, pH 7-9, salinity 5-35‰, DO >= 3.5 mg/L.
     *
     * QCVN provides acceptable ranges, not software severity levels. The application maps:
     * - 0 violation  -> NORMAL
     * - 1 non-DO violation -> WARNING
     * - DO below QCVN minimum or >= 2 violations -> DANGER
     */
    private void applyRuleBasedStatusAndAlerts(SensorReading reading) {
        RuleEvaluation evaluation = new RuleEvaluation();
        checkTemperature(reading, evaluation);
        checkPh(reading, evaluation);
        checkSalinity(reading, evaluation);
        checkDo(reading, evaluation);

        ReadingStatus ruleStatus;
        if (evaluation.warningMessages.isEmpty()) {
            ruleStatus = ReadingStatus.NORMAL;
            reading.setMessage("Thông số môi trường đạt ngưỡng QCVN 02-19:2014/BNNPTNT");
        } else if (evaluation.hasDanger || evaluation.warningMessages.size() >= 2) {
            ruleStatus = ReadingStatus.DANGER;
            reading.setMessage(String.join("; ", evaluation.warningMessages));
        } else {
            ruleStatus = ReadingStatus.WARNING;
            reading.setMessage(String.join("; ", evaluation.warningMessages));
        }

        reading.setRuleStatus(ruleStatus.name());
        reading.setFinalStatus(ruleStatus.name());
        reading.setStatus(ruleStatus);
        reading.setAnomalyStatus("NOT_RUN");
        reading.setMlStatus("NOT_RUN");
        reading.setAiMessage("AI service disabled or unavailable; final status uses rule-based QCVN layer");
        reading.setRecommendedAction(buildRuleRecommendation(ruleStatus));
    }

    private void applyAiPredictionIfAvailable(SensorReading reading) {
        Optional<AiPredictionResponse> prediction = aiPredictionService.predict(reading);
        if (prediction.isEmpty()) {
            return;
        }

        AiPredictionResponse ai = prediction.get();

        String anomalyStatus = normalizeStatus(ai.getIsolationForestStatus(), "NORMAL");
        String xgboostStatus = normalizeStatus(ai.getXgboostStatus(), "NORMAL");
        String randomForestStatus = normalizeStatus(ai.getRandomForestStatus(), "");

        // aiStatus is the ensemble result from Python AI service.
        // If aiStatus is missing, fallback to XGBoost status.
        String mlStatus = normalizeStatus(firstNonBlank(ai.getAiStatus(), xgboostStatus), "NORMAL");

        ReadingStatus ruleStatus = parseReadingStatus(reading.getRuleStatus(), ReadingStatus.NORMAL);
        ReadingStatus finalStatus;
        if (ai.getFinalStatus() != null && !ai.getFinalStatus().isBlank()) {
            finalStatus = parseReadingStatus(ai.getFinalStatus(), combineFinalStatus(ruleStatus, anomalyStatus, mlStatus));
        } else {
            finalStatus = combineFinalStatus(ruleStatus, anomalyStatus, mlStatus);
        }

        reading.setAnomalyStatus(anomalyStatus);
        reading.setMlStatus(mlStatus);
        reading.setFinalStatus(finalStatus.name());
        reading.setStatus(finalStatus);
        reading.setAiMessage(buildAiMessage(anomalyStatus, xgboostStatus, randomForestStatus, mlStatus));
        if (ai.getRecommendation() != null && !ai.getRecommendation().isBlank()) {
            reading.setRecommendedAction(ai.getRecommendation());
        } else {
            reading.setRecommendedAction(buildRuleRecommendation(finalStatus));
        }
        reading.setMessage(buildCombinedMessage(reading.getMessage(), reading.getAiMessage(), finalStatus));
    }

    private ReadingStatus combineFinalStatus(ReadingStatus ruleStatus, String anomalyStatus, String mlStatus) {
        ReadingStatus modelStatus = parseReadingStatus(mlStatus, ReadingStatus.NORMAL);
        if (ruleStatus == ReadingStatus.DANGER || modelStatus == ReadingStatus.DANGER) {
            return ReadingStatus.DANGER;
        }
        if ("ANOMALY".equalsIgnoreCase(anomalyStatus)) {
            return ReadingStatus.WARNING;
        }
        if (ruleStatus == ReadingStatus.WARNING || modelStatus == ReadingStatus.WARNING) {
            return ReadingStatus.WARNING;
        }
        return ReadingStatus.NORMAL;
    }

    private ReadingStatus parseReadingStatus(String value, ReadingStatus fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return ReadingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private String normalizeStatus(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) return first;
        return fallback;
    }

    private String buildAiMessage(String anomalyStatus, String xgboostStatus, String randomForestStatus, String aiStatus) {
        String rfPart = (randomForestStatus == null || randomForestStatus.isBlank()) ? "" : "; Random Forest: " + randomForestStatus;
        return "Isolation Forest: " + anomalyStatus + "; XGBoost: " + xgboostStatus + rfPart + "; AI Ensemble: " + aiStatus;
    }

    private String buildCombinedMessage(String ruleMessage, String aiMessage, ReadingStatus finalStatus) {
        return limitLength("Final=" + finalStatus.name() + " | Rule=" + ruleMessage + " | AI=" + aiMessage, 500);
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String buildRuleRecommendation(ReadingStatus status) {
        return switch (status) {
            case NORMAL -> "Tiếp tục giám sát định kỳ";
            case WARNING -> "Kiểm tra lại cảm biến và theo dõi xu hướng thông số trong các lần đo tiếp theo";
            case DANGER -> "Kiểm tra ao ngay, ưu tiên xử lý oxy hòa tan/pH/độ mặn/nhiệt độ theo thông số vượt chuẩn";
        };
    }

    private void checkTemperature(SensorReading reading, RuleEvaluation evaluation) {
        Double value = reading.getTemperature(); if (value == null) return;
        Long pondId = getPondIdFromDevice(reading.getDeviceId());
        ThresholdConfig config = thresholdConfigService.getThresholdConfigOrDefault(pondId, "TEMPERATURE", temperatureMin, temperatureMax);
        double min = config.getMinValue();
        double max = config.getMaxValue();
        if (value < min) {
            evaluation.addWarning("Nhiệt độ thấp hơn ngưỡng QCVN (" + min + "-" + max + " °C)");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.TEMP_LOW, AlertSeverity.WARNING, "Nhiệt độ thấp hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.TEMP_LOW);
        }
        if (value > max) {
            evaluation.addWarning("Nhiệt độ cao hơn ngưỡng QCVN (" + min + "-" + max + " °C)");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.TEMP_HIGH, AlertSeverity.WARNING, "Nhiệt độ cao hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.TEMP_HIGH);
        }
    }

    private void checkPh(SensorReading reading, RuleEvaluation evaluation) {
        Double value = reading.getPh(); if (value == null) return;
        Long pondId = getPondIdFromDevice(reading.getDeviceId());
        ThresholdConfig config = thresholdConfigService.getThresholdConfigOrDefault(pondId, "PH", phMin, phMax);
        double min = config.getMinValue();
        double max = config.getMaxValue();
        if (value < min) {
            evaluation.addWarning("pH thấp hơn ngưỡng QCVN (" + min + "-" + max + ")");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.PH_LOW, AlertSeverity.WARNING, "pH thấp hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.PH_LOW);
        }
        if (value > max) {
            evaluation.addWarning("pH cao hơn ngưỡng QCVN (" + min + "-" + max + ")");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.PH_HIGH, AlertSeverity.WARNING, "pH cao hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.PH_HIGH);
        }
    }

    private void checkSalinity(SensorReading reading, RuleEvaluation evaluation) {
        Double value = reading.getSalinity(); if (value == null) return;
        Long pondId = getPondIdFromDevice(reading.getDeviceId());
        ThresholdConfig config = thresholdConfigService.getThresholdConfigOrDefault(pondId, "SALINITY", salinityMin, salinityMax);
        double min = config.getMinValue();
        double max = config.getMaxValue();
        if (value < min) {
            evaluation.addWarning("Độ mặn thấp hơn ngưỡng QCVN (" + min + "-" + max + "‰)");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.SALINITY_LOW, AlertSeverity.WARNING, "Độ mặn thấp hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.SALINITY_LOW);
        }
        if (value > max) {
            evaluation.addWarning("Độ mặn cao hơn ngưỡng QCVN (" + min + "-" + max + "‰)");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.SALINITY_HIGH, AlertSeverity.WARNING, "Độ mặn cao hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.SALINITY_HIGH);
        }
    }

    private void checkDo(SensorReading reading, RuleEvaluation evaluation) {
        Double value = reading.getDoValue(); if (value == null) return;
        Long pondId = getPondIdFromDevice(reading.getDeviceId());
        ThresholdConfig config = thresholdConfigService.getThresholdConfigOrDefault(pondId, "DO", doMin, Double.MAX_VALUE);
        double min = config.getMinValue();
        if (value < min) {
            evaluation.addDanger("Oxy hòa tan thấp hơn ngưỡng QCVN (DO >= " + min + " mg/L)");
            alertService.openAlertIfMissing(reading.getDeviceId(), AlertType.DO_LOW, AlertSeverity.DANGER, "Oxy hòa tan thấp hơn ngưỡng QCVN");
        } else {
            alertService.autoResolveIfOpen(reading.getDeviceId(), AlertType.DO_LOW);
        }
    }

    private Long getPondIdFromDevice(String deviceId) {
        Device device = requireActiveRealDevice(deviceId);
        if (device.getPond() == null) {
            throw new IllegalArgumentException("Device is not assigned to any pond: " + deviceId);
        }
        return device.getPond().getId();
    }

    private Device requireActiveRealDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Active device not found: " + deviceId));

        if (!isRealDeviceId(device.getDeviceId())) {
            throw new IllegalArgumentException("Invalid device id for physical device: " + deviceId);
        }

        return device;
    }

    private boolean isRealDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        String normalized = deviceId.trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("pond_");
    }

    private static class RuleEvaluation {
        private final List<String> warningMessages = new ArrayList<>();
        private boolean hasDanger;

        private void addWarning(String message) {
            warningMessages.add(message);
        }

        private void addDanger(String message) {
            warningMessages.add(message);
            hasDanger = true;
        }
    }
}
