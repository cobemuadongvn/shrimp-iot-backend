package com.example.shrimpiot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {
    private boolean enabled = true;
    private String brokerUrl = "tcp://192.168.1.89:1883";
    private String username = "";
    private String password = "";
    private String backendClientId = "shrimp-iot-backend";
    private int qos = 1;
    private String telemetryTopicPattern = "shrimp-iot/devices/+/telemetry";
    private String ackTopicPattern = "shrimp-iot/devices/+/commands/ack";
    private String statusTopicPattern = "shrimp-iot/devices/+/status";
    private String commandTopicTemplate = "shrimp-iot/devices/{deviceId}/commands";
    private long commandRetryMs = 5000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBrokerUrl() { return brokerUrl; }
    public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBackendClientId() { return backendClientId; }
    public void setBackendClientId(String backendClientId) { this.backendClientId = backendClientId; }

    public int getQos() { return qos; }
    public void setQos(int qos) { this.qos = qos; }

    public String getTelemetryTopicPattern() { return telemetryTopicPattern; }
    public void setTelemetryTopicPattern(String telemetryTopicPattern) { this.telemetryTopicPattern = telemetryTopicPattern; }

    public String getAckTopicPattern() { return ackTopicPattern; }
    public void setAckTopicPattern(String ackTopicPattern) { this.ackTopicPattern = ackTopicPattern; }

    public String getStatusTopicPattern() { return statusTopicPattern; }
    public void setStatusTopicPattern(String statusTopicPattern) { this.statusTopicPattern = statusTopicPattern; }

    public String getCommandTopicTemplate() { return commandTopicTemplate; }
    public void setCommandTopicTemplate(String commandTopicTemplate) { this.commandTopicTemplate = commandTopicTemplate; }

    public long getCommandRetryMs() { return commandRetryMs; }
    public void setCommandRetryMs(long commandRetryMs) { this.commandRetryMs = commandRetryMs; }
}
