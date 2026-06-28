package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.CommandAckRequest;
import com.example.shrimpiot.dto.SensorReadingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttInboundService {
    private static final Logger log = LoggerFactory.getLogger(MqttInboundService.class);

    private final ObjectMapper objectMapper;
    private final SensorReadingService sensorReadingService;
    private final CommandService commandService;

    public MqttInboundService(
            ObjectMapper objectMapper,
            SensorReadingService sensorReadingService,
            CommandService commandService
    ) {
        this.objectMapper = objectMapper;
        this.sensorReadingService = sensorReadingService;
        this.commandService = commandService;
    }

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handleMqttMessage(Message<?> message) throws Exception {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = toPayloadString(message.getPayload());

        if (topic == null || topic.isBlank()) {
            return;
        }

        if (topic.endsWith("/telemetry")) {
            handleTelemetry(topic, payload);
            return;
        }

        if (topic.endsWith("/commands/ack") || topic.endsWith("/ack")) {
            handleCommandAck(topic, payload);
            return;
        }

        if (topic.endsWith("/status")) {
            handleDeviceStatus(topic, payload);
        }
    }

    private void handleTelemetry(String topic, String payload) throws Exception {
        String deviceIdFromTopic = extractDeviceId(topic);
        SensorReadingRequest request = objectMapper.readValue(payload, SensorReadingRequest.class);

        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            request.setDeviceId(deviceIdFromTopic);
        }

        if (!deviceIdFromTopic.equals(request.getDeviceId())) {
            throw new IllegalArgumentException("MQTT topic deviceId does not match payload deviceId");
        }

        sensorReadingService.saveReadingFromTrustedDevice(request);
        log.info("MQTT telemetry saved and published to realtime channel: deviceId={}", request.getDeviceId());
    }

    private void handleCommandAck(String topic, String payload) throws Exception {
        String deviceIdFromTopic = extractDeviceId(topic);
        MqttCommandAckPayload ack = objectMapper.readValue(payload, MqttCommandAckPayload.class);

        Long commandId = ack.commandId();
        if (commandId == null || commandId <= 0) {
            throw new IllegalArgumentException("MQTT ACK must contain a valid command id");
        }

        CommandAckRequest request = new CommandAckRequest();
        request.setSuccess(Boolean.TRUE.equals(ack.success()));
        request.setMessage(ack.message());

        commandService.acknowledgeFromTrustedDevice(deviceIdFromTopic, commandId, request);
    }

    private void handleDeviceStatus(String topic, String payload) {
        String deviceIdFromTopic = extractDeviceId(topic);
        commandService.updateDeviceStatusFromTrustedChannel(deviceIdFromTopic, payload);
        System.out.println("MQTT device status: " + deviceIdFromTopic + " -> " + payload);
    }

    private String extractDeviceId(String topic) {
        String[] parts = topic.split("/");

        // Preferred backend topic shape:
        // shrimp-iot/devices/{deviceId}/telemetry
        // shrimp-iot/devices/{deviceId}/commands/ack
        // shrimp-iot/devices/{deviceId}/status
        if (parts.length >= 4 && "shrimp-iot".equals(parts[0]) && "devices".equals(parts[1])) {
            return parts[2];
        }

        // Compatibility with report/thesis topic shape:
        // aquaculture/{farmId}/{pondId}/{deviceId}/telemetry
        // aquaculture/{farmId}/{pondId}/{deviceId}/ack
        // aquaculture/{farmId}/{pondId}/{deviceId}/status
        if (parts.length >= 5 && "aquaculture".equals(parts[0])) {
            return parts[3];
        }

        throw new IllegalArgumentException("Invalid MQTT topic: " + topic);
    }

    private String toPayloadString(Object payload) {
        if (payload == null) {
            return "";
        }
        if (payload instanceof byte[] bytes) {
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
        return String.valueOf(payload);
    }

    public record MqttCommandAckPayload(
            Long id,
            Long commandId,
            Boolean success,
            String message
    ) {
        public Long commandId() {
            return commandId != null ? commandId : id;
        }
    }
}
