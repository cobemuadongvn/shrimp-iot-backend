import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/** Publishes one safe device_01 status and telemetry sample for backend integration testing. */
public final class MqttDeviceTelemetryPublisher {
    private MqttDeviceTelemetryPublisher() {
    }

    public static void main(String[] args) throws Exception {
        String host = required("MQTT_TEST_HOST");
        String broker = "ssl://" + host + ":" + env("MQTT_TEST_PORT", "8883");
        MqttClient client = new MqttClient(broker, "device-01-smoke-test", new MemoryPersistence());
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            options.setUserName(required("MQTT_DEVICE_USERNAME"));
            options.setPassword(required("MQTT_DEVICE_PASSWORD").toCharArray());
            options.setHttpsHostnameVerificationEnabled(true);
            options.setSocketFactory(SSLContext.getDefault().getSocketFactory());
            options.setWill("shrimp-iot/devices/device_01/status",
                    "OFFLINE".getBytes(StandardCharsets.UTF_8), 1, true);
            client.connect(options);

            publish(client, "shrimp-iot/devices/device_01/status", "ONLINE", true);
            publish(client, "shrimp-iot/devices/device_01/telemetry",
                    "{\"deviceId\":\"device_01\",\"temperature\":27.25,\"ph\":7.55,"
                            + "\"ecValue\":12.4,\"salinity\":10.8,\"doValue\":6.7}", false);
            System.out.println("MQTT device smoke sample published: status=ONLINE, telemetry=1");
        } finally {
            try {
                if (client.isConnected()) client.disconnect();
            } finally {
                client.close();
            }
        }
    }

    private static void publish(MqttClient client, String topic, String payload, boolean retained) throws Exception {
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        message.setRetained(retained);
        client.publish(topic, message);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing environment: " + name);
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
