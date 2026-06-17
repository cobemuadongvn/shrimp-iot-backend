package com.example.shrimpiot.service;

import com.example.shrimpiot.config.MqttProperties;
import com.example.shrimpiot.model.DeviceCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttCommandPublisher {

    private final ObjectMapper objectMapper;
    private final MqttProperties mqttProperties;

    public MqttCommandPublisher(ObjectMapper objectMapper, MqttProperties mqttProperties) {
        this.objectMapper = objectMapper;
        this.mqttProperties = mqttProperties;
    }

    public void publishCommand(DeviceCommand command) {
        String topic = mqttProperties.getCommandTopicTemplate().replace("{deviceId}", command.getDeviceId());

        try {
            MqttCommandPayload payload = new MqttCommandPayload(
                    command.getId(),
                    command.getDeviceId(),
                    command.getRelayNo(),
                    command.getAction().name(),
                    command.getSource(),
                    command.getMessage(),
                    command.getExpiresAt() == null ? null : command.getExpiresAt().toString()
            );

            String json = objectMapper.writeValueAsString(payload);
            publish(topic, json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish MQTT command: " + rootMessage(e), e);
        }
    }

    private void publish(String topic, String json) throws MqttException {
        String clientId = mqttProperties.getBackendClientId()
                + "-pub-"
                + UUID.randomUUID().toString().substring(0, 8);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{mqttProperties.getBrokerUrl()});
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        options.setAutomaticReconnect(false);

        if (mqttProperties.getUsername() != null && !mqttProperties.getUsername().isBlank()) {
            options.setUserName(mqttProperties.getUsername());
        }

        if (mqttProperties.getPassword() != null && !mqttProperties.getPassword().isBlank()) {
            options.setPassword(mqttProperties.getPassword().toCharArray());
        }

        MqttClient client = new MqttClient(mqttProperties.getBrokerUrl(), clientId, new MemoryPersistence());
        try {
            client.connect(options);
            MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            message.setQos(mqttProperties.getQos());
            message.setRetained(false);
            client.publish(topic, message);
        } finally {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        }
    }

    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return msg == null || msg.isBlank() ? root.getClass().getSimpleName() : msg;
    }

    public record MqttCommandPayload(
            Long id,
            String deviceId,
            Integer relayNo,
            String action,
            String source,
            String message,
            String expiresAt
    ) {}
}
