package com.example.shrimpiot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;

class MqttConnectionOptionsFactoryTest {
    @Test
    void secureCloudOptionsUseTlsHostnameVerificationAndCredentials() {
        MqttProperties properties = new MqttProperties();
        properties.setBrokerUrl("ssl://example.s1.eu.hivemq.cloud:8883");
        properties.setUsername("backend-user");
        properties.setPassword("backend-password");
        properties.setTlsRequired(true);
        properties.setCredentialsRequired(true);

        MqttConnectOptions options = MqttConnectionOptionsFactory.create(properties, true);

        assertEquals(MqttConnectOptions.MQTT_VERSION_3_1_1, options.getMqttVersion());
        assertTrue(options.isAutomaticReconnect());
        assertTrue(options.isHttpsHostnameVerificationEnabled());
        assertTrue(options.getSocketFactory() instanceof SSLSocketFactory);
        assertEquals("backend-user", options.getUserName());
        assertNotNull(options.getPassword());
    }

    @Test
    void tlsRequiredRejectsPlainTcpBroker() {
        MqttProperties properties = new MqttProperties();
        properties.setBrokerUrl("tcp://127.0.0.1:1883");
        properties.setTlsRequired(true);

        assertThrows(IllegalStateException.class,
                () -> MqttConnectionOptionsFactory.create(properties, true));
    }

    @Test
    void credentialsRequiredRejectsMissingPassword() {
        MqttProperties properties = new MqttProperties();
        properties.setBrokerUrl("ssl://example.s1.eu.hivemq.cloud:8883");
        properties.setTlsRequired(true);
        properties.setCredentialsRequired(true);
        properties.setUsername("backend-user");

        assertThrows(IllegalStateException.class,
                () -> MqttConnectionOptionsFactory.create(properties, true));
    }
}
