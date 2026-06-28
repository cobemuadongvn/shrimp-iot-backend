/*
  IOT - HE THONG GIAM SAT MOI TRUONG AO NUOI THUY HAI SAN
  Board: Arduino UNO R4 WiFi
  Sensors: DS18B20 D6, pH A0, EC A1, SEN0681 DO via RS485-TTL Serial1 D0/D1
  Relays: D2-D5, active HIGH
  Required libraries: WiFiS3, PubSubClient, OneWire, DallasTemperature, DFRobot_PH, DFRobot_EC10, ArduinoJson
*/

#include <Arduino.h>
#include <WiFiS3.h>
#include <PubSubClient.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <ArduinoJson.h>
#include "DFRobot_PH.h"
#include "DFRobot_EC10.h"
#include "arduino_secrets.h"
#include "provisioning.h"

#ifdef __AVR__
  #include <avr/wdt.h>
#endif

char ssid[33] = SECRET_WIFI_SSID;
char pass[64] = SECRET_WIFI_PASSWORD;

const char SERVER_HOST[] = "175.16.16.108"; // HTTP fallback / debug only
const int SERVER_PORT = 8080;
const char API_READINGS_PATH[] = "/api/readings";
const char API_COMMANDS_PENDING_PATH[] = "/api/commands/pending?deviceId=";
const char API_KEY[] = SECRET_IOT_API_KEY;
const char DEVICE_ID[] = "device_01";
const char SETUP_AP_SSID[] = "ShrimpIoT-device_01";
const char SETUP_AP_PASSWORD[] = SECRET_SETUP_AP_PASSWORD;
const uint8_t PROVISION_RESET_PIN = 7; // Hold D7 to GND for 5 seconds during boot.

const char MQTT_HOST[] = SECRET_MQTT_HOST;
const int MQTT_PORT = 8883;
const char MQTT_USERNAME[] = SECRET_MQTT_USERNAME;
const char MQTT_PASSWORD[] = SECRET_MQTT_PASSWORD;
const char MQTT_TELEMETRY_TOPIC[] = "shrimp-iot/devices/device_01/telemetry";
const char MQTT_COMMAND_TOPIC[] = "shrimp-iot/devices/device_01/commands";
const char MQTT_ACK_TOPIC[] = "shrimp-iot/devices/device_01/commands/ack";
const char MQTT_STATUS_TOPIC[] = "shrimp-iot/devices/device_01/status";

// PINS
#define ONE_WIRE_BUS 6   // DS18B20 DATA on D6
#define PH_PIN A0
#define EC_PIN A1

// SEN0681 qua module RS485-TTL tu dong doi chieu.
// Module TXD -> Arduino D0/RX1
// Module RXD -> Arduino D1/TX1
// Module GND -> Arduino GND
// Module 5V  -> Arduino 5V
const uint32_t DO_BAUDRATE = 4800;
const uint8_t DO_SLAVE_ADDRESS = 0x01;
const uint8_t DO_FUNCTION_CODE = 0x03;
const uint16_t DO_START_REGISTER = 0x0002; // DO mg/L
const uint16_t DO_REGISTER_COUNT = 2;      // 1 float32 = 4 byte
const uint8_t DO_EXPECTED_DATA_BYTES = 4;
const size_t DO_EXPECTED_FRAME_LENGTH = 9;
const unsigned long DO_RESPONSE_TIMEOUT_MS = 1200;

// Chi dung cho demo/test: neu SEN0681 khong phan hoi thi tao DO gia lap.
// Khi van hanh that, doi thanh false de tranh che lap su co cam bien.
const bool DO_SIMULATION_FALLBACK_ENABLED = true;
const float DO_SIMULATION_CENTER = 6.80;
const float DO_SIMULATION_RANGE = 0.30;

#define RELAY1 2
#define RELAY2 3
#define RELAY3 4
#define RELAY4 5

// Relay module thực tế của bạn là ACTIVE HIGH:
// HIGH = ON, LOW = OFF
const int RELAY_ON_LEVEL = HIGH;
const int RELAY_OFF_LEVEL = LOW;

// Khong dung gia tri demo. Neu DS18B20 khong doc duoc, temperature = NAN
// va firmware se khong publish telemetry khong day du len backend.
const bool DEMO_TEMP_FALLBACK_ENABLED = false;

// Fail-safe OFF: if the device cannot confirm MQTT/backend communication
// for too long, turn every relay OFF locally. This avoids pumps staying ON
// when the backend can no longer supervise the device.
const bool FAILSAFE_OFF_ENABLED = true;
const unsigned long SERVER_FAILSAFE_TIMEOUT = 60000;

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature tempSensor(&oneWire);
DFRobot_PH ph;
DFRobot_EC10 ec;
WiFiClient client; // HTTP fallback/debug client
WiFiSSLClient mqttWifiClient;
PubSubClient mqttClient(mqttWifiClient);
ProvisioningManager provisioning;

float temperature = NAN;
float phValue = NAN;
float ecValue = NAN;
float salinity = NAN;
float doValue = NAN;
bool doValueIsSimulated = false;

const float ADC_MAX = 4095.0;
const float ADC_REF_MV = 5000.0;


unsigned long lastReadTime = 0;
unsigned long lastSendTime = 0;
unsigned long lastServerSuccessTime = 0;
unsigned long lastWiFiReconnectAttempt = 0;
unsigned long lastMqttReconnectAttempt = 0;
bool failsafeOffActive = false;
const unsigned long READ_INTERVAL = 1000;
const unsigned long SEND_INTERVAL = 10000;
const unsigned long WIFI_RECONNECT_INTERVAL = 10000;
const unsigned long MQTT_RECONNECT_INTERVAL = 5000;
const unsigned long PROVISIONING_COOLDOWN_MS = 60000;
unsigned long provisioningCooldownUntil = 0;

bool connectWiFi();
void setActiveWifiCredentials(const char *newSsid, const char *newPassword);
void loadCompiledWifiCredentials();
void startProvisioningMode(const char *lastError = nullptr);
void handleProvisioningMode();
bool provisioningCooldownActive();
void readSensors();
int readAnalogAverage(int pin, int samples = 20);
float adcToMilliVolt(int adcValue);
uint16_t calculateModbusCRC(const uint8_t *data, size_t length);
float parseBigEndianFloat(const uint8_t *bytes);
bool findValidDOFrame(const uint8_t *buffer, size_t bufferLength, size_t &frameStart);
bool readSEN0681DO(float &outDoMgL);
float generateSimulatedDO();
bool isSensorDataValid();
void appendJsonFloatOrNull(String &jsonData, const String &fieldName, float value, int decimals, bool addComma);
void sendDataToServer();
void connectMqtt();
void onMqttMessage(char* topic, byte* payload, unsigned int length);
void publishDeviceStatus(const char* status, bool retained = true);
void publishCommandAck(long commandId, bool success, const String &message);
void pollCommandsFromServer(); // HTTP fallback/debug only
bool executeRelayCommand(int relayNo, const String &action, String &message);
void ackCommand(long commandId, bool success, const String &message, int relayNo = 0, const String &action = ""); // HTTP ACK fallback
void turnAllRelaysOff(const String &reason);
void applyFailsafeIfNeeded();
bool httpGet(const String &path, String &responseBody);
bool httpPostJson(const String &path, const String &jsonData, String &responseBody);
String readFullHttpResponse();
String extractHttpBody(const String &response);
String decodeChunkedBody(const String &chunked);
String jsonEscape(const String &input);
void handleSerialCommands();
void processCommand(const String &cmdRaw);
void printStatus();
void printSensors();
void printWiFiStatus();
void scanOneWire();
void doReboot();

void setup() {
  Serial.begin(115200);
  delay(1000);

  randomSeed(analogRead(A5));
  Serial.println("\n=== IOT AQUACULTURE MONITORING - UNO R4 WIFI ===");
  Serial.println("Pin mapping: pH=A0, EC=A1, DS18B20=D6, SEN0681 DO=Serial1 D0/D1, Relay=D2-D5");
  Serial.println("Relay logic: ACTIVE HIGH (HIGH = ON, LOW = OFF)");
  Serial.println("ACK mode: MQTT ACK + HTTP ACK fallback, no backend code change needed");
  Serial.println("Commands: help, scan, do, read, status, wifi, wifi setup, wifi reset, send, cmd, pump1 on/off, pump2 on/off, pump3 on/off, pump4 on/off, reboot");

  analogReadResolution(12);
  pinMode(ONE_WIRE_BUS, INPUT_PULLUP);
  tempSensor.begin();
  tempSensor.setWaitForConversion(true);

  // UART phan cung cua UNO R4 WiFi: D0=RX1, D1=TX1.
  Serial1.begin(DO_BAUDRATE);
  delay(200);

  ph.begin();
  ec.begin();

  pinMode(RELAY1, OUTPUT);
  pinMode(RELAY2, OUTPUT);
  pinMode(RELAY3, OUTPUT);
  pinMode(RELAY4, OUTPUT);
  // Mặc định tắt toàn bộ relay
  digitalWrite(RELAY1, RELAY_OFF_LEVEL);
digitalWrite(RELAY2, RELAY_OFF_LEVEL);
  digitalWrite(RELAY3, RELAY_OFF_LEVEL);
  digitalWrite(RELAY4, RELAY_OFF_LEVEL);

  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(onMqttMessage);
  mqttClient.setBufferSize(1024);

  provisioning.begin(
      DEVICE_ID,
      SETUP_AP_SSID,
      SETUP_AP_PASSWORD,
      PROVISION_RESET_PIN
  );

  bool forceProvisioning = provisioning.resetRequestedAtBoot();
  if (forceProvisioning) {
    provisioning.clearStoredCredentials();
    loadCompiledWifiCredentials();
    startProvisioningMode(nullptr);
  } else {
    bool hasStoredCredentials = provisioning.loadStoredCredentials(
        ssid,
        sizeof(ssid),
        pass,
        sizeof(pass)
    );
    if (hasStoredCredentials) {
      Serial.println("Using WiFi credentials saved in EEPROM.");
    } else {
      Serial.println("No saved WiFi credentials. Using local compiled fallback.");
      loadCompiledWifiCredentials();
    }

    if (connectWiFi()) {
      provisioning.setState(ProvisioningState::MQTT_CONNECTING, nullptr);
      connectMqtt();
    } else {
      startProvisioningMode("WIFI_FAILED");
    }
  }
}

void loop() {
  if (provisioning.isActive()) {
    handleProvisioningMode();
    handleSerialCommands();
    return;
  }

  if (provisioningCooldownActive()) {
    handleSerialCommands();
    delay(5);
    return;
  }

  if (WiFi.status() != WL_CONNECTED) {
    unsigned long now = millis();
    if (lastWiFiReconnectAttempt == 0 ||
        now - lastWiFiReconnectAttempt >= WIFI_RECONNECT_INTERVAL) {
      if (!connectWiFi()) {
        startProvisioningMode("WIFI_FAILED");
      }
    }
    handleSerialCommands();
    return;
  }

  if (!mqttClient.connected()) {
    connectMqtt();
  }
  if (mqttClient.connected()) {
    mqttClient.loop();
  }

  unsigned long now = millis();

  if (now - lastReadTime >= READ_INTERVAL) {
    lastReadTime = now;
    readSensors();
    applyFailsafeIfNeeded();
  }

  if (now - lastSendTime >= SEND_INTERVAL) {
    lastSendTime = now;
    sendDataToServer();
  }

  handleSerialCommands();
}

bool connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;
  if (ssid[0] == '\0') {
    Serial.println("WiFi SSID is empty. Provisioning is required.");
    return false;
  }

  lastWiFiReconnectAttempt = millis();

  Serial.print("Connecting to WiFi: ");
  Serial.println(ssid);

  int status = WL_IDLE_STATUS;
  for (int attempt = 1; attempt <= 5; attempt++) {
    status = WiFi.begin(ssid, pass);
    Serial.print(".");
    if (status == WL_CONNECTED || WiFi.status() == WL_CONNECTED) {
      break;
    }
    delay(1000);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi connected!");
    Serial.print("Arduino IP: "); Serial.println(WiFi.localIP());
    Serial.print("RSSI: "); Serial.println(WiFi.RSSI());
    lastWiFiReconnectAttempt = 0;
    return true;
  } else {
    Serial.println("\nWiFi not connected. Starting provisioning mode.");
    return false;
  }
}

void setActiveWifiCredentials(
    const char *newSsid,
    const char *newPassword
) {
  strncpy(ssid, newSsid == nullptr ? "" : newSsid, sizeof(ssid) - 1);
  ssid[sizeof(ssid) - 1] = '\0';
  strncpy(pass, newPassword == nullptr ? "" : newPassword, sizeof(pass) - 1);
  pass[sizeof(pass) - 1] = '\0';
}

void loadCompiledWifiCredentials() {
  setActiveWifiCredentials(SECRET_WIFI_SSID, SECRET_WIFI_PASSWORD);
}

void startProvisioningMode(const char *lastError) {
  if (mqttClient.connected()) {
    mqttClient.disconnect();
  }
  turnAllRelaysOff("provisioning mode, all relays forced OFF");
  failsafeOffActive = true;
  provisioningCooldownUntil = 0;
  provisioning.start(lastError);
}

void handleProvisioningMode() {
  digitalWrite(RELAY1, RELAY_OFF_LEVEL);
  digitalWrite(RELAY2, RELAY_OFF_LEVEL);
  digitalWrite(RELAY3, RELAY_OFF_LEVEL);
  digitalWrite(RELAY4, RELAY_OFF_LEVEL);
  provisioning.loop();

  char candidateSsid[33];
  char candidatePassword[64];
  if (provisioning.takePendingCredentials(
          candidateSsid,
          sizeof(candidateSsid),
          candidatePassword,
          sizeof(candidatePassword))) {
    char previousSsid[33];
    char previousPassword[64];
    strncpy(previousSsid, ssid, sizeof(previousSsid));
    previousSsid[sizeof(previousSsid) - 1] = '\0';
    strncpy(previousPassword, pass, sizeof(previousPassword));
    previousPassword[sizeof(previousPassword) - 1] = '\0';

    provisioning.stop();
    setActiveWifiCredentials(candidateSsid, candidatePassword);
    provisioning.setState(ProvisioningState::WIFI_CONNECTING, nullptr);

    if (connectWiFi()) {
      bool saved = provisioning.saveCredentials(ssid, pass);
      provisioning.setState(
          ProvisioningState::MQTT_CONNECTING,
          saved ? nullptr : "STORAGE_WRITE_FAILED"
      );
      connectMqtt();
      return;
    }

    setActiveWifiCredentials(previousSsid, previousPassword);
    startProvisioningMode("WIFI_FAILED");
    return;
  }

  if (provisioning.consumeTimeoutRequest()) {
    Serial.println("Provisioning AP idle timeout. Cooling down for 60 seconds.");
    provisioning.stop();
    provisioningCooldownUntil = millis() + PROVISIONING_COOLDOWN_MS;
  }
}

bool provisioningCooldownActive() {
  if (provisioningCooldownUntil == 0) {
    return false;
  }
  if (static_cast<long>(millis() - provisioningCooldownUntil) >= 0) {
    provisioningCooldownUntil = 0;
    return false;
  }
  return true;
}

void readSensors() {
  tempSensor.requestTemperatures();
  float t = tempSensor.getTempCByIndex(0);

  if (t == DEVICE_DISCONNECTED_C || t < -55.0 || t > 125.0) {
    Serial.println("DS18B20 REAL read failed on D6. No demo value is used.");
    Serial.println("Check DATA=D6, VCC=5V, GND, and external 4.7k resistor between D6 and 5V.");
    temperature = NAN;
  } else {
    temperature = t;
    Serial.print("DS18B20 REAL temperature: ");
    Serial.print(temperature, 2);
    Serial.println(" C");
  }

  float tempForCompensation = isnan(temperature) ? 25.0 : temperature;

  int rawPH = readAnalogAverage(PH_PIN);
  float mvPH = adcToMilliVolt(rawPH);
  phValue = ph.readPH(mvPH, tempForCompensation);

  int rawEC = readAnalogAverage(EC_PIN);
  float mvEC = adcToMilliVolt(rawEC);
  ecValue = ec.readEC(mvEC, tempForCompensation);

  if (!isnan(ecValue) && !isnan(temperature)) {
    salinity = 0.4665 * pow(ecValue, 1.0878) * (1 + 0.02 * (temperature - 25.0));
  } else {
    salinity = NAN;
  }

  // Uu tien doc DO that tu SEN0681.
  float measuredDo = NAN;

  if (readSEN0681DO(measuredDo)) {
    doValue = measuredDo;
    doValueIsSimulated = false;
  } else if (DO_SIMULATION_FALLBACK_ENABLED) {
    doValue = generateSimulatedDO();
    doValueIsSimulated = true;

    Serial.print("SEN0681 DO read failed. Use simulated DO: ");
    Serial.print(doValue, 2);
    Serial.println(" mg/L");
  } else {
    doValue = NAN;
    doValueIsSimulated = false;

    Serial.println("SEN0681 DO read failed. DO fallback is disabled.");
  }

  static int dbgCnt = 0;
  if (++dbgCnt >= 5) {
    dbgCnt = 0;
Serial.print("RAW/mV | pH: ");
    Serial.print(rawPH);
    Serial.print(" / ");
    Serial.print(mvPH, 1);
    Serial.print(" | EC: ");
    Serial.print(rawEC);
    Serial.print(" / ");
    Serial.print(mvEC, 1);
    Serial.print(" | DO: ");

    if (isnan(doValue)) {
      Serial.println("N/A");
    } else {
      Serial.print(doValue, 2);
      Serial.println(doValueIsSimulated ? " (SIMULATED)" : " (REAL SEN0681)");
    }
  }

  printSensors();
}

int readAnalogAverage(int pin, int samples) {
  long total = 0;
  for (int i = 0; i < samples; i++) { total += analogRead(pin); delay(5); }
  return total / samples;
}

float adcToMilliVolt(int adcValue) { return adcValue * (ADC_REF_MV / ADC_MAX); }

uint16_t calculateModbusCRC(const uint8_t *data, size_t length) {
  uint16_t crc = 0xFFFF;

  for (size_t i = 0; i < length; i++) {
    crc ^= data[i];

    for (uint8_t bit = 0; bit < 8; bit++) {
      if (crc & 0x0001) {
        crc = (crc >> 1) ^ 0xA001;
      } else {
        crc >>= 1;
      }
    }
  }

  return crc;
}

float parseBigEndianFloat(const uint8_t *bytes) {
  uint32_t raw =
      ((uint32_t)bytes[0] << 24) |
      ((uint32_t)bytes[1] << 16) |
      ((uint32_t)bytes[2] << 8) |
      ((uint32_t)bytes[3]);

  float value;
  memcpy(&value, &raw, sizeof(value));
  return value;
}

bool findValidDOFrame(
  const uint8_t *buffer,
  size_t bufferLength,
  size_t &frameStart
) {
  if (bufferLength < DO_EXPECTED_FRAME_LENGTH) {
    return false;
  }

  for (
    size_t start = 0;
    start + DO_EXPECTED_FRAME_LENGTH <= bufferLength;
    start++
  ) {
    if (buffer[start] != DO_SLAVE_ADDRESS) {
      continue;
    }

    if (buffer[start + 1] != DO_FUNCTION_CODE) {
      continue;
    }

    if (buffer[start + 2] != DO_EXPECTED_DATA_BYTES) {
      continue;
    }

    uint16_t receivedCRC =
        (uint16_t)buffer[start + 7] |
        ((uint16_t)buffer[start + 8] << 8);

    uint16_t calculatedCRC =
        calculateModbusCRC(&buffer[start], 7);

    if (receivedCRC == calculatedCRC) {
      frameStart = start;
      return true;
    }
  }

  return false;
}

bool readSEN0681DO(float &outDoMgL) {
  outDoMgL = NAN;

  uint8_t request[8] = {
    DO_SLAVE_ADDRESS,
    DO_FUNCTION_CODE,
    highByte(DO_START_REGISTER),
    lowByte(DO_START_REGISTER),
    highByte(DO_REGISTER_COUNT),
    lowByte(DO_REGISTER_COUNT),
    0x00,
    0x00
  };

  uint16_t requestCRC = calculateModbusCRC(request, 6);
  request[6] = lowByte(requestCRC);
  request[7] = highByte(requestCRC);

  while (Serial1.available()) {
    Serial1.read();
  }

  delay(10);

  Serial1.write(request, sizeof(request));
  Serial1.flush();

  uint8_t received[64];
  size_t receivedLength = 0;
  size_t frameStart = 0;
  unsigned long startTime = millis();

  while (millis() - startTime < DO_RESPONSE_TIMEOUT_MS) {
    while (
      Serial1.available() &&
      receivedLength < sizeof(received)
    ) {
      received[receivedLength++] = (uint8_t)Serial1.read();
    }

    if (
      findValidDOFrame(
        received,
        receivedLength,
        frameStart
      )
    ) {
const uint8_t *data = &received[frameStart + 3];
      float doMgL = parseBigEndianFloat(data);

      if (
        isnan(doMgL) ||
        isinf(doMgL) ||
        doMgL < 0.0 ||
        doMgL > 30.0
      ) {
        Serial.print("SEN0681 returned invalid DO: ");
        Serial.println(doMgL);
        return false;
      }

      outDoMgL = doMgL;

      Serial.print("SEN0681 REAL DO: ");
      Serial.print(outDoMgL, 2);
      Serial.println(" mg/L");

      return true;
    }

    delay(2);
  }

  Serial.print("SEN0681 invalid/no response. Bytes received: ");
  Serial.println(receivedLength);

  if (receivedLength > 0) {
    Serial.print("SEN0681 RX: ");

    for (size_t i = 0; i < receivedLength; i++) {
      if (received[i] < 0x10) {
        Serial.print('0');
      }

      Serial.print(received[i], HEX);
      Serial.print(' ');
    }

    Serial.println();
  }

  return false;
}


float generateSimulatedDO() {
  float offset = random(-300, 301) / 1000.0;
  float simulated = DO_SIMULATION_CENTER + offset;

  if (simulated < 0.0) simulated = 0.0;
  if (simulated > 30.0) simulated = 30.0;

  return simulated;
}

bool isSensorDataValid() {
  if (isnan(temperature) || isnan(phValue) || isnan(ecValue) || isnan(salinity) || isnan(doValue)) return false;
  if (temperature < 0 || temperature > 60) return false;
  if (phValue < 0 || phValue > 14) return false;
  if (ecValue < 0 || ecValue > 100) return false;
  if (salinity < 0 || salinity > 100) return false;
  if (doValue < 0 || doValue > 30) return false;
  return true;
}

void appendJsonFloatOrNull(String &jsonData, const String &fieldName, float value, int decimals, bool addComma) {
  jsonData += "\"";
  jsonData += fieldName;
  jsonData += "\":";

  if (isnan(value) || isinf(value)) {
    jsonData += "null";
  } else {
    jsonData += String(value, decimals);
  }

  if (addComma) {
    jsonData += ",";
  }
}

void sendDataToServer() {
  String jsonData = "{";
  jsonData += "\"deviceId\":\"" + String(DEVICE_ID) + "\",";
  appendJsonFloatOrNull(jsonData, "temperature", temperature, 2, true);
  appendJsonFloatOrNull(jsonData, "ph", phValue, 2, true);
  appendJsonFloatOrNull(jsonData, "ecValue", ecValue, 2, true);
  appendJsonFloatOrNull(jsonData, "salinity", salinity, 2, true);
  appendJsonFloatOrNull(jsonData, "doValue", doValue, 2, false);
  jsonData += "}";

  Serial.println("Publishing telemetry by MQTT...");
  Serial.println(jsonData);

  if (!mqttClient.connected()) {
    connectMqtt();
  }

  bool ok = mqttClient.publish(MQTT_TELEMETRY_TOPIC, jsonData.c_str(), false);
  if (!ok) {
    Serial.println("MQTT telemetry publish failed!");
    return;
  }

  lastServerSuccessTime = millis();
  failsafeOffActive = false;
  Serial.println("MQTT telemetry published.");
}

void connectMqtt() {
  if (WiFi.status() != WL_CONNECTED) return;
  if (mqttClient.connected()) return;

  unsigned long now = millis();
  if (lastMqttReconnectAttempt != 0 && now - lastMqttReconnectAttempt < MQTT_RECONNECT_INTERVAL) {
    return;
  }
  lastMqttReconnectAttempt = now;

  Serial.print("Connecting to MQTT broker ");
  Serial.print(MQTT_HOST);
  Serial.print(":");
  Serial.print(MQTT_PORT);
  Serial.print(" ... ");

  bool ok = mqttClient.connect(
    DEVICE_ID,
    MQTT_USERNAME,
    MQTT_PASSWORD,
    MQTT_STATUS_TOPIC,
    1,
    true,
    "OFFLINE"
  );

  if (ok) {
    Serial.println("connected");
    provisioning.setState(ProvisioningState::ONLINE, nullptr);
    publishDeviceStatus("ONLINE", true);
    mqttClient.subscribe(MQTT_COMMAND_TOPIC, 1);
    Serial.print("Subscribed command topic: ");
    Serial.println(MQTT_COMMAND_TOPIC);
    lastServerSuccessTime = millis();
    failsafeOffActive = false;
    lastMqttReconnectAttempt = 0;
  } else {
    provisioning.setState(
        ProvisioningState::MQTT_FAILED,
        "MQTT_CONNECT_FAILED"
    );
    Serial.print("failed, rc=");
    Serial.print(mqttClient.state());
    Serial.println(". Will retry later; Serial relay commands still work.");
  }
}

void publishDeviceStatus(const char* status, bool retained) {
  if (mqttClient.connected()) {
    mqttClient.publish(MQTT_STATUS_TOPIC, status, retained);
  }
}

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  String body = "";
  for (unsigned int i = 0; i < length; i++) {
    body += (char)payload[i];
  }

  Serial.print("MQTT message topic: ");
  Serial.println(topic);
  Serial.println(body);

  if (String(topic) != String(MQTT_COMMAND_TOPIC)) {
    Serial.println("Ignored message from unknown topic.");
    return;
  }

  DynamicJsonDocument doc(1024);
  DeserializationError error = deserializeJson(doc, body);
  if (error) {
    Serial.print("Command JSON parse failed: ");
    Serial.println(error.c_str());
    return;
  }

  long commandId = doc["id"] | 0;
  int relayNo = doc["relayNo"] | 0;
  const char* actionRaw = doc["action"] | "";
  String action = String(actionRaw);
  String message = "";

  Serial.print("Execute MQTT command id=");
  Serial.print(commandId);
  Serial.print(" relay=");
  Serial.print(relayNo);
  Serial.print(" action=");
  Serial.println(action);

  bool executed = executeRelayCommand(relayNo, action, message);
  if (commandId > 0) {
    // Gui ACK qua MQTT dung topic chuan.
    publishCommandAck(commandId, executed, message);

    // Gui them ACK qua HTTP endpoint san co cua backend.
    // Cach nay khong can sua backend MQTT ACK subscriber.
    ackCommand(commandId, executed, message, relayNo, action);
  }

  lastServerSuccessTime = millis();
  failsafeOffActive = false;
}

void publishCommandAck(long commandId, bool success, const String &message) {
  String jsonData = "{";
  jsonData += "\"id\":" + String(commandId) + ",";
  jsonData += "\"success\":";
  jsonData += success ? "true" : "false";
  jsonData += ",\"message\":\"";
  jsonData += jsonEscape(message);
  jsonData += "\"}";

  Serial.print("Publishing MQTT ACK for command ");
  Serial.println(commandId);
  Serial.println(jsonData);

  if (!mqttClient.connected()) {
    connectMqtt();
  }

  bool ok = mqttClient.publish(MQTT_ACK_TOPIC, jsonData.c_str(), false);
  if (!ok) {
    Serial.println("MQTT ACK publish failed!");
    return;
  }

  lastServerSuccessTime = millis();
  failsafeOffActive = false;
  Serial.println("MQTT ACK published.");
}

void pollCommandsFromServer() {
  String path = String(API_COMMANDS_PENDING_PATH) + String(DEVICE_ID);
  String responseBody;
  Serial.println("Checking pending commands...");
  bool ok = httpGet(path, responseBody);
if (!ok) { Serial.println("Cannot get pending commands."); return; }
  lastServerSuccessTime = millis();
  failsafeOffActive = false;
  if (responseBody.length() == 0) { Serial.println("Empty command response."); return; }

  DynamicJsonDocument doc(4096);
  DeserializationError error = deserializeJson(doc, responseBody);
  if (error) {
    Serial.print("Command JSON parse failed: "); Serial.println(error.c_str());
    Serial.println(responseBody);
    return;
  }

  bool success = doc["success"] | false;
  if (!success) { Serial.println("Backend returned success=false for commands."); Serial.println(responseBody); return; }
  JsonArray commands = doc["data"].as<JsonArray>();
  if (commands.isNull() || commands.size() == 0) { Serial.println("No pending command."); return; }

  Serial.print("Pending commands: "); Serial.println(commands.size());
  for (JsonObject command : commands) {
    long commandId = command["id"] | 0;
    int relayNo = command["relayNo"] | 0;
    const char* actionRaw = command["action"] | "";
    String action = String(actionRaw);
    String message = "";
    Serial.print("Execute command id="); Serial.print(commandId);
    Serial.print(" relay="); Serial.print(relayNo);
    Serial.print(" action="); Serial.println(action);
    bool executed = executeRelayCommand(relayNo, action, message);
    if (commandId > 0) ackCommand(commandId, executed, message, relayNo, action);
  }
}

bool executeRelayCommand(int relayNo, const String &actionRaw, String &message) {
  String action = actionRaw;
  action.trim(); action.toUpperCase();
  int relayPin = -1;
  switch (relayNo) {
    case 1: relayPin = RELAY1; break;
    case 2: relayPin = RELAY2; break;
    case 3: relayPin = RELAY3; break;
    case 4: relayPin = RELAY4; break;
    default: message = "Invalid relay number"; Serial.println(message); return false;
  }
  if (action == "ON") { digitalWrite(relayPin, RELAY_ON_LEVEL); message = "Relay " + String(relayNo) + " turned ON"; Serial.println(message); return true; }
  if (action == "OFF") { digitalWrite(relayPin, RELAY_OFF_LEVEL); message = "Relay " + String(relayNo) + " turned OFF"; Serial.println(message); return true; }
  message = "Invalid action"; Serial.println(message); return false;
}

void ackCommand(long commandId, bool success, const String &message, int relayNo, const String &action) {
  String path = "/api/commands/" + String(commandId) + "/ack";

  String currentState = "";
  if (relayNo >= 1 && relayNo <= 4) {
    int relayPin = -1;
    switch (relayNo) {
      case 1: relayPin = RELAY1; break;
      case 2: relayPin = RELAY2; break;
      case 3: relayPin = RELAY3; break;
      case 4: relayPin = RELAY4; break;
    }
    if (relayPin > 0) {
      currentState = (digitalRead(relayPin) == RELAY_ON_LEVEL) ? "ON" : "OFF";
    }
  }

  String jsonData = "{";
  jsonData += "\"id\":" + String(commandId) + ",";
  jsonData += "\"commandId\":" + String(commandId) + ",";
  jsonData += "\"deviceId\":\"" + String(DEVICE_ID) + "\",";
jsonData += "\"relayNo\":" + String(relayNo) + ",";
  jsonData += "\"action\":\"" + jsonEscape(action) + "\",";
  jsonData += "\"currentState\":\"" + jsonEscape(currentState) + "\",";
  jsonData += "\"success\":";
  jsonData += (success ? "true" : "false");
  jsonData += ",\"message\":\"";
  jsonData += jsonEscape(message);
  jsonData += "\"}";

  String responseBody;

  Serial.print("Sending HTTP ACK for command ");
  Serial.println(commandId);
  Serial.println(jsonData);

  bool ok = false;
  for (int attempt = 1; attempt <= 3; attempt++) {
    ok = httpPostJson(path, jsonData, responseBody);

    Serial.print("HTTP ACK attempt ");
    Serial.print(attempt);
    Serial.print(": ");
    Serial.println(ok ? "OK" : "FAILED");

    if (ok) break;
    delay(300);
  }

  if (!ok) {
    Serial.println("HTTP ACK failed after retries!");
    return;
  }

  lastServerSuccessTime = millis();
  failsafeOffActive = false;
  Serial.println("HTTP ACK response:");
  Serial.println(responseBody);
}

void turnAllRelaysOff(const String &reason) {
  digitalWrite(RELAY1, RELAY_OFF_LEVEL);
  digitalWrite(RELAY2, RELAY_OFF_LEVEL);
  digitalWrite(RELAY3, RELAY_OFF_LEVEL);
  digitalWrite(RELAY4, RELAY_OFF_LEVEL);
  Serial.print("FAILSAFE OFF: ");
  Serial.println(reason);
}

void applyFailsafeIfNeeded() {
  if (!FAILSAFE_OFF_ENABLED) return;
  if (lastServerSuccessTime == 0) return;

  bool serverOfflineTooLong = millis() - lastServerSuccessTime > SERVER_FAILSAFE_TIMEOUT;
  if (!serverOfflineTooLong) return;

  if (!failsafeOffActive) {
    turnAllRelaysOff("communication timeout, all relays forced OFF");
    failsafeOffActive = true;
    return;
  }

  digitalWrite(RELAY1, RELAY_OFF_LEVEL);
  digitalWrite(RELAY2, RELAY_OFF_LEVEL);
  digitalWrite(RELAY3, RELAY_OFF_LEVEL);
  digitalWrite(RELAY4, RELAY_OFF_LEVEL);
}

bool httpGet(const String &path, String &responseBody) {
  if (!client.connect(SERVER_HOST, SERVER_PORT)) { Serial.println("Cannot connect to server for GET!"); return false; }
  client.print("GET "); client.print(path); client.println(" HTTP/1.1");
  client.print("Host: "); client.print(SERVER_HOST); client.print(":"); client.println(SERVER_PORT);
  client.print("X-API-Key: "); client.println(API_KEY);
  client.println("Connection: close"); client.println();
  String fullResponse = readFullHttpResponse(); client.stop();
  responseBody = extractHttpBody(fullResponse); responseBody.trim();
  return responseBody.length() > 0;
}

bool httpPostJson(const String &path, const String &jsonData, String &responseBody) {
  if (!client.connect(SERVER_HOST, SERVER_PORT)) { Serial.println("Cannot connect to server for POST!"); return false; }
  client.print("POST "); client.print(path); client.println(" HTTP/1.1");
  client.print("Host: "); client.print(SERVER_HOST); client.print(":"); client.println(SERVER_PORT);
  client.println("Content-Type: application/json");
  client.print("X-API-Key: "); client.println(API_KEY);
  client.print("Content-Length: "); client.println(jsonData.length());
  client.println("Connection: close"); client.println();
  client.print(jsonData);
  String fullResponse = readFullHttpResponse(); client.stop();
  responseBody = extractHttpBody(fullResponse); responseBody.trim();
return responseBody.length() > 0;
}

String readFullHttpResponse() {
  String response = "";
  unsigned long startTime = millis();
  unsigned long lastDataTime = millis();
  while (client.connected() || client.available()) {
    while (client.available()) { char c = client.read(); response += c; lastDataTime = millis(); }
    if (response.length() > 0 && millis() - lastDataTime > 5000) break;
    if (millis() - startTime > 10000) break;
  }
  return response;
}

String extractHttpBody(const String &response) {
  int headerEnd = response.indexOf("\r\n\r\n");
  int bodyStart = -1;
  if (headerEnd >= 0) bodyStart = headerEnd + 4;
  else {
    headerEnd = response.indexOf("\n\n");
    if (headerEnd >= 0) bodyStart = headerEnd + 2;
  }
  if (bodyStart < 0) return response;
  String headers = response.substring(0, headerEnd);
  String body = response.substring(bodyStart);
  String lowerHeaders = headers;
  lowerHeaders.toLowerCase();
  if (lowerHeaders.indexOf("transfer-encoding: chunked") >= 0) return decodeChunkedBody(body);
  return body;
}

String decodeChunkedBody(const String &chunked) {
  String decoded = "";
  int pos = 0;
  while (pos < chunked.length()) {
    int lineEnd = chunked.indexOf("\r\n", pos);
    int nextStartOffset = 2;
    if (lineEnd < 0) { lineEnd = chunked.indexOf('\n', pos); nextStartOffset = 1; }
    if (lineEnd < 0) break;
    String sizeLine = chunked.substring(pos, lineEnd); sizeLine.trim();
    int semicolonIndex = sizeLine.indexOf(';');
    if (semicolonIndex >= 0) sizeLine = sizeLine.substring(0, semicolonIndex);
    long chunkSize = strtol(sizeLine.c_str(), NULL, 16);
    if (chunkSize <= 0) break;
    pos = lineEnd + nextStartOffset;
    if (pos + chunkSize > chunked.length()) break;
    decoded += chunked.substring(pos, pos + chunkSize);
    pos += chunkSize;
    if (pos + 2 <= chunked.length() && chunked.substring(pos, pos + 2) == "\r\n") pos += 2;
    else if (pos < chunked.length() && chunked.charAt(pos) == '\n') pos += 1;
  }
  return decoded;
}

String jsonEscape(const String &input) {
  String out = "";
  for (int i = 0; i < input.length(); i++) {
    char c = input.charAt(i);
    if (c == '\\') out += "\\\\";
    else if (c == '"') out += "\\\"";
    else if (c == '\n') out += "\\n";
    else if (c == '\r') out += "\\r";
    else out += c;
  }
  return out;
}

void handleSerialCommands() {
  static String input = "";
  while (Serial.available()) {
    char c = Serial.read();
    if (c == '\r') continue;
    if (c == '\n') {
      input.trim();
      if (input.length()) processCommand(input);
      input = "";
    } else {
      input += c;
      if (input.length() > 200) input = input.substring(input.length() - 200);
    }
  }
}

void processCommand(const String &cmdRaw) {
  String cmd = cmdRaw; cmd.toLowerCase(); cmd.trim();
  if (cmd == "help") { Serial.println("Commands: scan, do, read, status, wifi, wifi setup, wifi reset, send, cmd, pump1 on/off, pump2 on/off, pump3 on/off, pump4 on/off, reboot"); return; }
if (cmd == "scan") { scanOneWire(); return; }
  if (cmd == "do") {
    float testDo = NAN;

    if (readSEN0681DO(testDo)) {
      Serial.print("DO test OK: ");
      Serial.print(testDo, 2);
      Serial.println(" mg/L");
    } else {
      Serial.println("DO test FAILED.");
    }

    return;
  }
  if (cmd == "read") { readSensors(); return; }
  if (cmd == "status") { printStatus(); return; }
  if (cmd == "wifi") { printWiFiStatus(); return; }
  if (cmd == "wifi setup") {
    startProvisioningMode(nullptr);
    return;
  }
  if (cmd == "wifi reset") {
    provisioning.clearStoredCredentials();
    loadCompiledWifiCredentials();
    startProvisioningMode(nullptr);
    return;
  }
  if (provisioning.isActive() && cmd.startsWith("pump") && cmd.endsWith(" on")) {
    Serial.println("Relay ON is blocked while provisioning mode is active.");
    return;
  }
  if (cmd == "send") { sendDataToServer(); return; }
  if (cmd == "cmd") { Serial.println("MQTT mode: commands are received by subscribed topic. HTTP polling fallback is available in code but not used in loop."); return; }
  if (cmd == "reboot") { Serial.println("Rebooting..."); delay(50); doReboot(); return; }
  if (cmd == "pump1 on") { digitalWrite(RELAY1, RELAY_ON_LEVEL); Serial.println("pump1 ON"); return; }
  if (cmd == "pump1 off") { digitalWrite(RELAY1, RELAY_OFF_LEVEL); Serial.println("pump1 OFF"); return; }
  if (cmd == "pump2 on") { digitalWrite(RELAY2, RELAY_ON_LEVEL); Serial.println("pump2 ON"); return; }
  if (cmd == "pump2 off") { digitalWrite(RELAY2, RELAY_OFF_LEVEL); Serial.println("pump2 OFF"); return; }
  if (cmd == "pump3 on") { digitalWrite(RELAY3, RELAY_ON_LEVEL); Serial.println("pump3 ON"); return; }
  if (cmd == "pump3 off") { digitalWrite(RELAY3, RELAY_OFF_LEVEL); Serial.println("pump3 OFF"); return; }
  if (cmd == "pump4 on") { digitalWrite(RELAY4, RELAY_ON_LEVEL); Serial.println("pump4 ON"); return; }
  if (cmd == "pump4 off") { digitalWrite(RELAY4, RELAY_OFF_LEVEL); Serial.println("pump4 OFF"); return; }
  Serial.println("Unknown command. Type 'help'.");
}

void scanOneWire() {
  Serial.println("OneWire scan on D6:");
  byte addr[8];
  oneWire.reset_search();
  bool found = false;

  while (oneWire.search(addr)) {
    found = true;
    for (int i = 0; i < 8; i++) {
      if (addr[i] < 16) Serial.print('0');
      Serial.print(addr[i], HEX);
      Serial.print(' ');
    }

    if (OneWire::crc8(addr, 7) == addr[7]) {
      Serial.println("CRC OK");
    } else {
      Serial.println("CRC ERROR");
    }
  }

  if (!found) {
    Serial.println("No OneWire device found on D6.");
    Serial.println("Check DS18B20: VCC=5V, GND=GND, DATA=D6, pull-up 4.7k between DATA and 5V.");
    Serial.println("No fallback is enabled; telemetry will be skipped until DS18B20 is detected.");
  }

  oneWire.reset_search();
}

void printStatus() {
  Serial.print("MQTT: "); Serial.println(mqttClient.connected() ? "CONNECTED" : "DISCONNECTED");
  Serial.print("Provisioning mode: "); Serial.println(provisioning.isActive() ? "ACTIVE" : "INACTIVE");
  Serial.print("Provisioning state: "); Serial.println(provisioning.stateName());
  Serial.print("Provisioning reset pin: D"); Serial.println(PROVISION_RESET_PIN);
  Serial.print("MQTT broker: "); Serial.print(MQTT_HOST); Serial.print(":"); Serial.println(MQTT_PORT);
  Serial.print("Telemetry topic: "); Serial.println(MQTT_TELEMETRY_TOPIC);
  Serial.print("Command topic: "); Serial.println(MQTT_COMMAND_TOPIC);
  Serial.print("ACK topic: "); Serial.println(MQTT_ACK_TOPIC);
  Serial.println("ACK mode: MQTT ACK + HTTP ACK fallback enabled");
  Serial.println("DS18B20 pin: D6");
Serial.println("Temperature mode: REAL ONLY - no fallback");
  Serial.println("DO mode: SEN0681 REAL via Serial1 D0/RX1 and D1/TX1");
  Serial.print("DO simulation fallback: ");
  Serial.println(DO_SIMULATION_FALLBACK_ENABLED ? "ENABLED" : "DISABLED");
  Serial.print("pump1: "); Serial.println(digitalRead(RELAY1) == RELAY_ON_LEVEL ? "ON" : "OFF");
  Serial.print("pump2: "); Serial.println(digitalRead(RELAY2) == RELAY_ON_LEVEL ? "ON" : "OFF");
  Serial.print("pump3: "); Serial.println(digitalRead(RELAY3) == RELAY_ON_LEVEL ? "ON" : "OFF");
  Serial.print("pump4: "); Serial.println(digitalRead(RELAY4) == RELAY_ON_LEVEL ? "ON" : "OFF");
}

void printSensors() {
  Serial.print("PH: "); Serial.print(isnan(phValue) ? "N/A" : String(phValue, 2));
  Serial.print(" | EC: "); Serial.print(isnan(ecValue) ? "N/A" : String(ecValue, 2));
  Serial.print(" | Salt: "); Serial.print(isnan(salinity) ? "N/A" : String(salinity, 2));
  Serial.print(" | DO: ");

  if (isnan(doValue)) {
    Serial.print("N/A");
  } else {
    Serial.print(doValue, 2);
  }

  Serial.print(" mg/L");
  Serial.print(doValueIsSimulated ? " [SIMULATED]" : " [REAL]");
  Serial.print(" | Temp: ");
  Serial.println(isnan(temperature) ? "N/A" : String(temperature, 2));
}

void printWiFiStatus() {
  if (provisioning.isActive()) {
    Serial.println("WiFi status: PROVISIONING_AP");
    Serial.print("Setup SSID: "); Serial.println(provisioning.setupSsid());
    Serial.print("Setup URL: http://"); Serial.println(WiFi.localIP());
    return;
  }
  Serial.print("WiFi status: "); Serial.println(WiFi.status() == WL_CONNECTED ? "CONNECTED" : "DISCONNECTED");
  Serial.print("IP: "); Serial.println(WiFi.localIP());
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("RSSI: "); Serial.println(WiFi.RSSI());
  }
}

void doReboot() {
  Serial.println("Attempting software reboot...");
#ifdef __AVR__
  wdt_enable(WDTO_15MS);
  while (1) { }
#endif
#if defined(ARDUINO_ARCH_RENESAS)
  NVIC_SystemReset();
  while (1) { }
#endif
  Serial.println("Software reboot not supported. Please power-cycle.");
}
