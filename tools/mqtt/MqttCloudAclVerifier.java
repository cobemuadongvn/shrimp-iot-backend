import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/** Verifies HiveMQ TLS credentials, two-way messaging, and device namespace isolation. */
public final class MqttCloudAclVerifier {
    private static final String DEVICE_ROOT = "shrimp-iot/devices/device_01";

    private MqttCloudAclVerifier() {
    }

    public static void main(String[] args) throws Exception {
        String host = required("MQTT_TEST_HOST");
        int port = Integer.parseInt(env("MQTT_TEST_PORT", "8883"));
        String broker = "ssl://" + host + ":" + port;
        MqttClient backend = client(broker, "acl-backend");
        MqttClient device = client(broker, "acl-device-01");

        try {
            backend.connect(options(required("MQTT_BACKEND_USERNAME"), required("MQTT_BACKEND_PASSWORD")));
            device.connect(options(required("MQTT_DEVICE_USERNAME"), required("MQTT_DEVICE_PASSWORD")));

            String telemetryTest = DEVICE_ROOT + "/test/device-to-backend";
            CountDownLatch backendReceived = new CountDownLatch(1);
            backend.subscribe(telemetryTest, 1, (topic, message) -> backendReceived.countDown());
            publish(device, telemetryTest, "device-to-backend");
            requireMessage(backendReceived, "backend did not receive the device publish");

            String commandTest = DEVICE_ROOT + "/test/backend-to-device";
            CountDownLatch deviceReceived = new CountDownLatch(1);
            device.subscribe(commandTest, 1, (topic, message) -> deviceReceived.countDown());
            publish(backend, commandTest, "backend-to-device");
            requireMessage(deviceReceived, "device did not receive the backend publish");

            boolean denied = false;
            try {
                IMqttToken token = device.subscribeWithResponse("shrimp-iot/devices/device_02/#", 1);
                token.waitForCompletion(10_000);
                int[] granted = token.getGrantedQos();
                denied = granted != null && granted.length == 1 && granted[0] == 0x80;
            } catch (Exception expectedForDeniedSubscription) {
                denied = true;
            }
            if (!denied) {
                throw new IllegalStateException("device-01 was able to subscribe to device_02 namespace");
            }

            System.out.println("MQTT cloud ACL verified: tls=PASS, device_to_backend=PASS, "
                    + "backend_to_device=PASS, cross_device_subscribe=DENIED");
        } finally {
            close(device);
            close(backend);
        }
    }

    private static MqttClient client(String broker, String prefix) throws Exception {
        return new MqttClient(broker, prefix + "-" + UUID.randomUUID().toString().substring(0, 8),
                new MemoryPersistence());
    }

    private static MqttConnectOptions options(String username, String password) throws Exception {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setHttpsHostnameVerificationEnabled(true);
        options.setSocketFactory(SSLContext.getDefault().getSocketFactory());
        return options;
    }

    private static void publish(MqttClient client, String topic, String payload) throws Exception {
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        message.setRetained(false);
        client.publish(topic, message);
    }

    private static void requireMessage(CountDownLatch latch, String error) throws Exception {
        if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException(error);
    }

    private static void close(MqttClient client) {
        try {
            if (client.isConnected()) client.disconnect();
        } catch (Exception ignored) {
        }
        try {
            client.close();
        } catch (Exception ignored) {
        }
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
