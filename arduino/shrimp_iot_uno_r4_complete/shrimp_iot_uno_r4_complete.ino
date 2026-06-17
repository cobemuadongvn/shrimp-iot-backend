/*
  IOT - HE THONG GIAM SAT MOI TRUONG AO NUOI THUY HAI SAN
  Board: Arduino UNO R4 WiFi
  Sensors: DS18B20 D6, pH A0, EC A1, DO simulated
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

#ifdef __AVR__
  #include <avr/wdt.h>
#endif

char ssid[] = "Thủy Lợi Cafeteria";
char pass[] = "";

const char SERVER_HOST[] = "192.168.1.171"; // HTTP fallback / debug only
const int SERVER_PORT = 8080;
const char API_READINGS_PATH[] = "/api/readings";
const char API_COMMANDS_PENDING_PATH[] = "/api/commands/pending?deviceId=";
const char API_KEY[] = "MY_SECRET_KEY";
const char DEVICE_ID[] = "device_01";

const char MQTT_HOST[] = "192.168.1.171";
const int MQTT_PORT = 1883;
const char MQTT_TELEMETRY_TOPIC[] = "shrimp-iot/devices/device_01/telemetry";
const char MQTT_COMMAND_TOPIC[] = "shrimp-iot/devices/device_01/commands";
const char MQTT_ACK_TOPIC[] = "shrimp-iot/devices/device_01/commands/ack";
const char MQTT_STATUS_TOPIC[] = "shrimp-iot/devices/device_01/status";

// PINS - theo luong code Serial control ban gui
#define ONE_WIRE_BUS 6   // DS18B20 DATA on D6
#define PH_PIN A0
#define EC_PIN A1

// DO trong luong code ban gui dang la gia lap 6.8 +- 0.30,
// khong doc chan A2 de tranh sai gia tri DO khi chua co module DO analog.
#define RELAY1 2
#define RELAY2 3
#define RELAY3 4
#define RELAY4 5

// Relay module thực tế của bạn là ACTIVE HIGH:
// HIGH = ON, LOW = OFF
const int RELAY_ON_LEVEL = HIGH;
const int RELAY_OFF_LEVEL = LOW;

// Fallback de demo: neu DS18B20 o D6 chua doc duoc thi dung tam 28.0 do C.
// Khi cam bien doc duoc that, co the doi thanh false.
const bool DEMO_TEMP_FALLBACK_ENABLED = true;
const float DEMO_TEMP_FALLBACK_VALUE = 28.0;

// Tắt failsafe trong giai đoạn test/manual control để relay không tự bật lại ngoài ý muốn.
// Khi vận hành thật có thể đổi thành true và cấu hình đúng relay theo thiết bị thực tế.
const bool FAILSAFE_ENABLED = false;
const unsigned long SERVER_FAILSAFE_TIMEOUT = 30000;
const float DO_FAILSAFE_MIN = 4.0;
const float TEMP_FAILSAFE_MAX = 35.0;

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature tempSensor(&oneWire);
DFRobot_PH ph;
DFRobot_EC10 ec;
WiFiClient client; // HTTP fallback/debug client
WiFiClient mqttWifiClient;
PubSubClient mqttClient(mqttWifiClient);

float temperature = NAN;
float phValue = NAN;
float ecValue = NAN;
float salinity = NAN;
float doValue = NAN;

const float ADC_MAX = 4095.0;
const float ADC_REF_MV = 5000.0;

#define DO_TWO_POINT_CALIBRATION 0
const float DO_CAL1_V = 1600.0;
const float DO_CAL1_T = 25.0;
const float DO_CAL2_V = 1300.0;
const float DO_CAL2_T = 15.0;

const uint16_t DO_TABLE[41] = {
  14460, 14220, 13820, 13440, 13090,
  12740, 12420, 12110, 11810, 11530,
  11260, 11010, 10770, 10530, 10300,
  10080, 9860, 9660, 9460, 9270,
  9080, 8900, 8730, 8570, 8410,
  8250, 8110, 7960, 7820, 7690,
  7560, 7430, 7300, 7180, 7070,
  6950, 6840, 6730, 6630, 6530,
  6410
};

unsigned long lastReadTime = 0;
unsigned long lastSendTime = 0;
unsigned long lastServerSuccessTime = 0;
unsigned long lastWiFiReconnectAttempt = 0;
unsigned long lastMqttReconnectAttempt = 0;
const unsigned long READ_INTERVAL = 1000;
const unsigned long SEND_INTERVAL = 10000;
const unsigned long WIFI_RECONNECT_INTERVAL = 10000;
const unsigned long MQTT_RECONNECT_INTERVAL = 5000;

void connectWiFi();
void readSensors();
int readAnalogAverage(int pin, int samples = 20);
float adcToMilliVolt(int adcValue);
float readDOFromMilliVolt(float voltageMv, float temperatureC);
bool isSensorDataValid();
void sendDataToServer();
void connectMqtt();
void onMqttMessage(char* topic, byte* payload, unsigned int length);
void publishDeviceStatus(const char* status, bool retained = true);
void publishCommandAck(long commandId, bool success, const String &message);
void pollCommandsFromServer(); // HTTP fallback/debug only
bool executeRelayCommand(int relayNo, const String &action, String &message);
void ackCommand(long commandId, bool success, const String &message, int relayNo = 0, const String &action = ""); // HTTP ACK fallback
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
  Serial.println("Pin mapping: pH=A0, EC=A1, DS18B20=D6, Relay=D2-D5, DO=simulated");
  Serial.println("Relay logic: ACTIVE HIGH (HIGH = ON, LOW = OFF)");
  Serial.println("ACK mode: MQTT ACK + HTTP ACK fallback, no backend code change needed");
  Serial.println("Commands: help, scan, read, status, wifi, send, cmd, pump1 on/off, pump2 on/off, pump3 on/off, pump4 on/off, reboot");

  analogReadResolution(12);
  tempSensor.begin();
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

  connectWiFi();
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(onMqttMessage);
  mqttClient.setBufferSize(1024);
  connectMqtt();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
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
    if (isSensorDataValid()) sendDataToServer();
    else {
      Serial.println("Sensor data invalid. Skip sending to server.");
      printSensors();
    }
  }

  handleSerialCommands();
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  unsigned long now = millis();
  if (lastWiFiReconnectAttempt != 0 && now - lastWiFiReconnectAttempt < WIFI_RECONNECT_INTERVAL) {
    return;
  }
  lastWiFiReconnectAttempt = now;

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
  } else {
    Serial.println("\nWiFi not connected. Will retry later; Serial relay commands still work.");
  }
}

void readSensors() {
  tempSensor.requestTemperatures();
  float t = tempSensor.getTempCByIndex(0);

  if (t == DEVICE_DISCONNECTED_C) {
    if (DEMO_TEMP_FALLBACK_ENABLED) {
      Serial.println("Temp sensor disconnected on D6! Use fallback temp = 28.0 for demo.");
      temperature = DEMO_TEMP_FALLBACK_VALUE;
    } else {
      Serial.println("Temp sensor disconnected!");
      temperature = NAN;
    }
  } else {
    temperature = t;
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

  // Theo luong code Serial control ban gui: DO gia lap quanh 6.8 mg/L.
  // Neu sau nay co cam bien DO analog that, co the them lai DO_PIN A2 va doc readDOFromMilliVolt().
  doValue = 6.8 + random(-30, 31) / 100.0;

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
    Serial.print(" | DO: simulated ");
    Serial.println(doValue, 2);
  }

  printSensors();
}

int readAnalogAverage(int pin, int samples) {
  long total = 0;
  for (int i = 0; i < samples; i++) { total += analogRead(pin); delay(5); }
  return total / samples;
}

float adcToMilliVolt(int adcValue) { return adcValue * (ADC_REF_MV / ADC_MAX); }

float readDOFromMilliVolt(float voltageMv, float temperatureC) {
  int tempIndex = (int)round(temperatureC);
  if (tempIndex < 0) tempIndex = 0;
  if (tempIndex > 40) tempIndex = 40;
  float vSaturation = 0;
#if DO_TWO_POINT_CALIBRATION
  vSaturation = (temperatureC - DO_CAL2_T) * (DO_CAL1_V - DO_CAL2_V) / (DO_CAL1_T - DO_CAL2_T) + DO_CAL2_V;
#else
  vSaturation = DO_CAL1_V + (temperatureC - DO_CAL1_T) * 35.0;
#endif
  if (vSaturation <= 0) return NAN;
  return (voltageMv * DO_TABLE[tempIndex] / vSaturation) / 1000.0;
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

void sendDataToServer() {
  String jsonData = "{";
  jsonData += "\"deviceId\":\"" + String(DEVICE_ID) + "\",";
  jsonData += "\"temperature\":" + String(temperature, 2) + ",";
  jsonData += "\"ph\":" + String(phValue, 2) + ",";
  jsonData += "\"ecValue\":" + String(ecValue, 2) + ",";
  jsonData += "\"salinity\":" + String(salinity, 2) + ",";
  jsonData += "\"doValue\":" + String(doValue, 2);
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
    MQTT_STATUS_TOPIC,
    1,
    true,
    "OFFLINE"
  );

  if (ok) {
    Serial.println("connected");
    publishDeviceStatus("ONLINE", true);
    mqttClient.subscribe(MQTT_COMMAND_TOPIC, 1);
    Serial.print("Subscribed command topic: ");
    Serial.println(MQTT_COMMAND_TOPIC);
    lastServerSuccessTime = millis();
    lastMqttReconnectAttempt = 0;
  } else {
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
  Serial.println("MQTT ACK published.");
}

void pollCommandsFromServer() {
  String path = String(API_COMMANDS_PENDING_PATH) + String(DEVICE_ID);
  String responseBody;
  Serial.println("Checking pending commands...");
  bool ok = httpGet(path, responseBody);
  if (!ok) { Serial.println("Cannot get pending commands."); return; }
  lastServerSuccessTime = millis();
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
  Serial.println("HTTP ACK response:");
  Serial.println(responseBody);
}

void applyFailsafeIfNeeded() {
  if (!FAILSAFE_ENABLED) return;
  if (lastServerSuccessTime == 0) return;
  bool serverOfflineTooLong = millis() - lastServerSuccessTime > SERVER_FAILSAFE_TIMEOUT;
  if (!serverOfflineTooLong) return;
  if (!isnan(doValue) && doValue < DO_FAILSAFE_MIN) {
    digitalWrite(RELAY3, RELAY_ON_LEVEL);
    Serial.println("FAILSAFE: Server offline + DO low -> oxygen relay ON");
  }
  if (!isnan(temperature) && temperature > TEMP_FAILSAFE_MAX) {
    digitalWrite(RELAY2, RELAY_ON_LEVEL);
    Serial.println("FAILSAFE: Server offline + temperature high -> water fan relay ON");
  }
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
  if (cmd == "help") { Serial.println("Commands: scan, read, status, wifi, send, cmd, pump1 on/off, pump2 on/off, pump3 on/off, pump4 on/off, reboot"); return; }
  if (cmd == "scan") { scanOneWire(); return; }
  if (cmd == "read") { readSensors(); return; }
  if (cmd == "status") { printStatus(); return; }
  if (cmd == "wifi") { printWiFiStatus(); return; }
  if (cmd == "send") { if (isSensorDataValid()) sendDataToServer(); else { Serial.println("Sensor data invalid. Cannot send."); printSensors(); } return; }
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
    if (DEMO_TEMP_FALLBACK_ENABLED) {
      Serial.println("Fallback temp is ON, system can still send demo telemetry with temp=28.0.");
    }
  }

  oneWire.reset_search();
}

void printStatus() {
  Serial.print("MQTT: "); Serial.println(mqttClient.connected() ? "CONNECTED" : "DISCONNECTED");
  Serial.print("MQTT broker: "); Serial.print(MQTT_HOST); Serial.print(":"); Serial.println(MQTT_PORT);
  Serial.print("Telemetry topic: "); Serial.println(MQTT_TELEMETRY_TOPIC);
  Serial.print("Command topic: "); Serial.println(MQTT_COMMAND_TOPIC);
  Serial.print("ACK topic: "); Serial.println(MQTT_ACK_TOPIC);
  Serial.println("ACK mode: MQTT ACK + HTTP ACK fallback enabled");
  Serial.println("DS18B20 pin: D6");
  Serial.print("pump1: "); Serial.println(digitalRead(RELAY1) == RELAY_ON_LEVEL ? "ON" : "OFF");
  Serial.print("pump2: "); Serial.println(digitalRead(RELAY2) == RELAY_ON_LEVEL ? "ON" : "OFF");
  Serial.print("pump3: "); Serial.println(digitalRead(RELAY3) == RELAY_ON_LEVEL ? "ON" : "OFF");
  Serial.print("pump4: "); Serial.println(digitalRead(RELAY4) == RELAY_ON_LEVEL ? "ON" : "OFF");
}

void printSensors() {
  Serial.print("PH: "); Serial.print(isnan(phValue) ? "N/A" : String(phValue, 2));
  Serial.print(" | EC: "); Serial.print(isnan(ecValue) ? "N/A" : String(ecValue, 2));
  Serial.print(" | Salt: "); Serial.print(isnan(salinity) ? "N/A" : String(salinity, 2));
  Serial.print(" | DO: "); Serial.print(isnan(doValue) ? "N/A" : String(doValue, 2)); Serial.print(" mg/L");
  Serial.print(" | Temp: "); Serial.println(isnan(temperature) ? "N/A" : String(temperature, 2));
}

void printWiFiStatus() {
  Serial.print("WiFi status: "); Serial.println(WiFi.status() == WL_CONNECTED ? "CONNECTED" : "DISCONNECTED");
  Serial.print("IP: "); Serial.println(WiFi.localIP());
  Serial.print("RSSI: "); Serial.println(WiFi.RSSI());
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
