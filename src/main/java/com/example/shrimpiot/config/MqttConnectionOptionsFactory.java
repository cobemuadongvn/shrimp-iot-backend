package com.example.shrimpiot.config;

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/** Creates identical, production-safe MQTT options for inbound and outbound clients. */
public final class MqttConnectionOptionsFactory {
    private MqttConnectionOptionsFactory() {
    }

    public static MqttConnectOptions create(MqttProperties properties, boolean automaticReconnect) {
        validate(properties);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getBrokerUrl()});
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setAutomaticReconnect(automaticReconnect);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        options.setHttpsHostnameVerificationEnabled(properties.isHostnameVerificationEnabled());

        if (isSecure(properties.getBrokerUrl())) {
            try {
                options.setSocketFactory(SSLContext.getDefault().getSocketFactory());
            } catch (GeneralSecurityException error) {
                throw new IllegalStateException("Unable to initialize the default TLS context", error);
            }
        }

        if (hasText(properties.getUsername())) {
            options.setUserName(properties.getUsername());
        }
        if (hasText(properties.getPassword())) {
            options.setPassword(properties.getPassword().toCharArray());
        }
        return options;
    }

    static void validate(MqttProperties properties) {
        if (!hasText(properties.getBrokerUrl())) {
            throw new IllegalStateException("MQTT broker URL is required");
        }
        if (properties.isTlsRequired() && !isSecure(properties.getBrokerUrl())) {
            throw new IllegalStateException("MQTT TLS is required; broker URL must use ssl:// or wss://");
        }
        if (properties.isCredentialsRequired()
                && (!hasText(properties.getUsername()) || !hasText(properties.getPassword()))) {
            throw new IllegalStateException("MQTT username and password are required");
        }
    }

    private static boolean isSecure(String brokerUrl) {
        return brokerUrl != null
                && (brokerUrl.startsWith("ssl://") || brokerUrl.startsWith("wss://"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
