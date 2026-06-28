package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.AiPredictionResponse;
import com.example.shrimpiot.model.SensorReading;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AiPredictionService {
    private static final Logger log = LoggerFactory.getLogger(AiPredictionService.class);

    private final boolean enabled;
    private final String serviceUrl;
    private final int timeoutMs;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiPredictionService(
            @Value("${ai.enabled:false}") boolean enabled,
            @Value("${ai.service-url:http://127.0.0.1:8001/predict}") String serviceUrl,
            @Value("${ai.timeout-ms:3000}") int timeoutMs,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.serviceUrl = serviceUrl;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @PostConstruct
    public void logConfig() {
        log.info("AI prediction config: enabled={}, serviceUrl={}, timeoutMs={}", enabled, serviceUrl, timeoutMs);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<AiPredictionResponse> predict(SensorReading reading) {
        if (!enabled) {
            log.warn("AI prediction skipped because ai.enabled=false");
            return Optional.empty();
        }

        if (hasMissingRequiredMetric(reading)) {
            log.warn("AI prediction skipped because telemetry has missing sensor value: deviceId={}, readingId={}",
                    reading.getDeviceId(), reading.getId());
            return Optional.empty();
        }

        try {
            // FastAPI /predict requires snake_case keys: ec_value and do_value.
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("temperature", reading.getTemperature());
            payload.put("ph", reading.getPh());
            payload.put("ec_value", reading.getEcValue());
            payload.put("salinity", reading.getSalinity());
            payload.put("do_value", reading.getDoValue());

            String body = objectMapper.writeValueAsString(payload);
            log.info("Calling AI service: url={}, payload={}", serviceUrl, body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("AI service response: status={}, body={}", response.statusCode(), response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI service returned non-2xx status: status={}, body={}", response.statusCode(), response.body());
                return Optional.empty();
            }

            AiPredictionResponse aiResponse = objectMapper.readValue(response.body(), AiPredictionResponse.class);
            if (aiResponse.getError() != null && !aiResponse.getError().isBlank()) {
                log.error("AI service returned error field: {}", aiResponse.getError());
                return Optional.empty();
            }
            return Optional.of(aiResponse);
        } catch (Exception ex) {
            log.error("AI prediction failed. enabled={}, url={}, error={}", enabled, serviceUrl, ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private boolean hasMissingRequiredMetric(SensorReading reading) {
        return reading.getTemperature() == null
                || reading.getPh() == null
                || reading.getEcValue() == null
                || reading.getSalinity() == null
                || reading.getDoValue() == null;
    }
}
