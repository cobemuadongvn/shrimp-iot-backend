#include "provisioning.h"

#include <ArduinoJson.h>
#include <EEPROM.h>
#include <stddef.h>
#include <string.h>

ProvisioningManager::ProvisioningManager()
    : _server(80),
      _active(false),
      _pendingCredentials(false),
      _timeoutRequested(false),
      _resetPin(7),
      _lastActivityMs(0),
      _state(ProvisioningState::SETUP_AP),
      _networkCount(0) {
  _deviceId[0] = '\0';
  _setupSsid[0] = '\0';
  _setupPassword[0] = '\0';
  _lastError[0] = '\0';
  _pendingSsid[0] = '\0';
  _pendingPassword[0] = '\0';
}

void ProvisioningManager::begin(
    const char *deviceId,
    const char *setupSsid,
    const char *setupPassword,
    uint8_t resetPin
) {
  copyString(_deviceId, sizeof(_deviceId), deviceId);
  copyString(_setupSsid, sizeof(_setupSsid), setupSsid);
  copyString(_setupPassword, sizeof(_setupPassword), setupPassword);
  _resetPin = resetPin;
  pinMode(_resetPin, INPUT_PULLUP);
}

bool ProvisioningManager::resetRequestedAtBoot(unsigned long holdMs) {
  if (digitalRead(_resetPin) != LOW) {
    return false;
  }

  Serial.print("Provisioning reset pin D");
  Serial.print(_resetPin);
  Serial.println(" is LOW. Keep holding to clear saved WiFi...");

  unsigned long started = millis();
  while (millis() - started < holdMs) {
    if (digitalRead(_resetPin) != LOW) {
      Serial.println("Provisioning reset cancelled.");
      return false;
    }
    delay(20);
  }

  Serial.println("Provisioning reset confirmed.");
  return true;
}

uint32_t ProvisioningManager::calculateChecksum(
    const StoredWifiConfig &config
) const {
  const uint8_t *bytes = reinterpret_cast<const uint8_t *>(&config);
  const size_t length = offsetof(StoredWifiConfig, checksum);
  uint32_t hash = 2166136261UL;

  for (size_t i = 0; i < length; i++) {
    hash ^= bytes[i];
    hash *= 16777619UL;
  }

  return hash;
}

bool ProvisioningManager::isStoredConfigValid(
    const StoredWifiConfig &config
) const {
  if (config.magic != STORAGE_MAGIC || config.version != STORAGE_VERSION) {
    return false;
  }
  if (memchr(config.ssid, '\0', sizeof(config.ssid)) == nullptr) {
    return false;
  }
  if (memchr(config.password, '\0', sizeof(config.password)) == nullptr) {
    return false;
  }
  size_t ssidLength = strlen(config.ssid);
  size_t passwordLength = strlen(config.password);
  if (ssidLength == 0 || ssidLength > 32) {
    return false;
  }
  if (passwordLength > 63 || (passwordLength > 0 && passwordLength < 8)) {
    return false;
  }
  return config.checksum == calculateChecksum(config);
}

bool ProvisioningManager::loadStoredCredentials(
    char *ssidOut,
    size_t ssidSize,
    char *passwordOut,
    size_t passwordSize
) {
  if (EEPROM.length() < sizeof(StoredWifiConfig)) {
    Serial.println("EEPROM is too small for WiFi provisioning data.");
    return false;
  }

  StoredWifiConfig config;
  EEPROM.get(0, config);
  if (!isStoredConfigValid(config)) {
    return false;
  }

  copyString(ssidOut, ssidSize, config.ssid);
  copyString(passwordOut, passwordSize, config.password);
  return true;
}

bool ProvisioningManager::saveCredentials(
    const char *ssid,
    const char *password
) {
  if (ssid == nullptr || password == nullptr) {
    return false;
  }
  size_t ssidLength = strlen(ssid);
  size_t passwordLength = strlen(password);
  if (ssidLength == 0 || ssidLength > 32) {
    return false;
  }
  if (passwordLength > 63 || (passwordLength > 0 && passwordLength < 8)) {
    return false;
  }
  if (EEPROM.length() < sizeof(StoredWifiConfig)) {
    return false;
  }

  StoredWifiConfig existing;
  EEPROM.get(0, existing);
  if (isStoredConfigValid(existing) &&
      strcmp(existing.ssid, ssid) == 0 &&
      strcmp(existing.password, password) == 0) {
    return true;
  }

  StoredWifiConfig config;
  memset(&config, 0, sizeof(config));
  config.magic = STORAGE_MAGIC;
  config.version = STORAGE_VERSION;
  copyString(config.ssid, sizeof(config.ssid), ssid);
  copyString(config.password, sizeof(config.password), password);
  config.checksum = calculateChecksum(config);
  EEPROM.put(0, config);
  return true;
}

void ProvisioningManager::clearStoredCredentials() {
  if (EEPROM.length() < sizeof(StoredWifiConfig)) {
    return;
  }
  StoredWifiConfig empty;
  memset(&empty, 0, sizeof(empty));
  EEPROM.put(0, empty);
  Serial.println("Saved WiFi credentials cleared.");
}

void ProvisioningManager::scanNetworksBeforeAp() {
  _networkCount = 0;
  int count = WiFi.scanNetworks();
  if (count <= 0) {
    return;
  }

  for (int i = 0; i < count && _networkCount < MAX_NETWORKS; i++) {
    String candidate = WiFi.SSID(i);
    if (candidate.length() == 0) {
      continue;
    }

    bool duplicate = false;
    for (uint8_t existing = 0; existing < _networkCount; existing++) {
      if (_networkSsids[existing] == candidate) {
        duplicate = true;
        if (WiFi.RSSI(i) > _networkRssi[existing]) {
          _networkRssi[existing] = WiFi.RSSI(i);
          _networkSecure[existing] = WiFi.encryptionType(i) != ENC_TYPE_NONE;
        }
        break;
      }
    }

    if (!duplicate) {
      _networkSsids[_networkCount] = candidate;
      _networkRssi[_networkCount] = WiFi.RSSI(i);
      _networkSecure[_networkCount] = WiFi.encryptionType(i) != ENC_TYPE_NONE;
      _networkCount++;
    }
  }

  for (uint8_t i = 0; i < _networkCount; i++) {
    for (uint8_t j = i + 1; j < _networkCount; j++) {
      if (_networkRssi[j] > _networkRssi[i]) {
        String ssidSwap = _networkSsids[i];
        _networkSsids[i] = _networkSsids[j];
        _networkSsids[j] = ssidSwap;
        int32_t rssiSwap = _networkRssi[i];
        _networkRssi[i] = _networkRssi[j];
        _networkRssi[j] = rssiSwap;
        bool secureSwap = _networkSecure[i];
        _networkSecure[i] = _networkSecure[j];
        _networkSecure[j] = secureSwap;
      }
    }
  }
}

bool ProvisioningManager::start(const char *lastError) {
  if (_active) {
    setState(ProvisioningState::SETUP_AP, lastError);
    return true;
  }

  WiFi.disconnect();
  delay(250);
  scanNetworksBeforeAp();

  int status = WiFi.beginAP(_setupSsid, _setupPassword);
  if (status != WL_AP_LISTENING && status != WL_AP_CONNECTED) {
    Serial.println("Failed to create provisioning access point.");
    setState(ProvisioningState::WIFI_FAILED, "AP_START_FAILED");
    return false;
  }

  _server.begin();
  _active = true;
  _pendingCredentials = false;
  _timeoutRequested = false;
  _lastActivityMs = millis();
  setState(ProvisioningState::SETUP_AP, lastError);

  Serial.println("Provisioning access point started.");
  Serial.print("Setup SSID: ");
  Serial.println(_setupSsid);
  Serial.print("Setup URL: http://");
  Serial.println(WiFi.localIP());
  return true;
}

void ProvisioningManager::stop() {
  if (_active) {
    _server.end();
  }
  _active = false;
  WiFi.disconnect();
  delay(250);
}

void ProvisioningManager::loop() {
  if (!_active) {
    return;
  }

  WiFiClient client = _server.available();
  if (client) {
    _lastActivityMs = millis();
    handleClient(client);
  }

  if (!_pendingCredentials &&
      millis() - _lastActivityMs >= AP_IDLE_TIMEOUT_MS) {
    _timeoutRequested = true;
  }
}

bool ProvisioningManager::isActive() const {
  return _active;
}

bool ProvisioningManager::takePendingCredentials(
    char *ssidOut,
    size_t ssidSize,
    char *passwordOut,
    size_t passwordSize
) {
  if (!_pendingCredentials) {
    return false;
  }
  copyString(ssidOut, ssidSize, _pendingSsid);
  copyString(passwordOut, passwordSize, _pendingPassword);
  _pendingCredentials = false;
  return true;
}

bool ProvisioningManager::consumeTimeoutRequest() {
  if (!_timeoutRequested) {
    return false;
  }
  _timeoutRequested = false;
  return true;
}

void ProvisioningManager::setState(
    ProvisioningState state,
    const char *lastError
) {
  _state = state;
  copyString(_lastError, sizeof(_lastError), lastError == nullptr ? "" : lastError);
}

ProvisioningState ProvisioningManager::state() const {
  return _state;
}

const char *ProvisioningManager::stateName() const {
  switch (_state) {
    case ProvisioningState::SETUP_AP: return "SETUP_AP";
    case ProvisioningState::WIFI_CONNECTING: return "WIFI_CONNECTING";
    case ProvisioningState::MQTT_CONNECTING: return "MQTT_CONNECTING";
    case ProvisioningState::ONLINE: return "ONLINE";
    case ProvisioningState::WIFI_FAILED: return "WIFI_FAILED";
    case ProvisioningState::MQTT_FAILED: return "MQTT_FAILED";
    default: return "SETUP_AP";
  }
}

const char *ProvisioningManager::lastError() const {
  return _lastError;
}

const char *ProvisioningManager::setupSsid() const {
  return _setupSsid;
}

void ProvisioningManager::handleClient(WiFiClient &client) {
  client.setTimeout(1500);
  String method;
  String path;
  String setupCode;
  String body;

  if (!readHttpRequest(client, method, path, setupCode, body)) {
    sendJsonError(
        client,
        400,
        "Bad Request",
        "INVALID_REQUEST",
        "Malformed or oversized HTTP request"
    );
    client.stop();
    return;
  }

  routeRequest(client, method, path, setupCode, body);
  delay(10);
  client.stop();
}

bool ProvisioningManager::readHttpRequest(
    WiFiClient &client,
    String &method,
    String &path,
    String &setupCode,
    String &body
) {
  unsigned long waitStarted = millis();
  while (!client.available() && millis() - waitStarted < 1500) {
    delay(2);
  }
  if (!client.available()) {
    return false;
  }

  String requestLine = client.readStringUntil('\n');
  requestLine.trim();
  int firstSpace = requestLine.indexOf(' ');
  int secondSpace = requestLine.indexOf(' ', firstSpace + 1);
  if (firstSpace <= 0 || secondSpace <= firstSpace) {
    return false;
  }

  method = requestLine.substring(0, firstSpace);
  path = requestLine.substring(firstSpace + 1, secondSpace);
  int queryStart = path.indexOf('?');
  if (queryStart >= 0) {
    path = path.substring(0, queryStart);
  }

  int contentLength = 0;
  while (true) {
    String header = client.readStringUntil('\n');
    header.trim();
    if (header.length() == 0) {
      break;
    }

    int colon = header.indexOf(':');
    if (colon <= 0) {
      continue;
    }
    String name = header.substring(0, colon);
    String value = header.substring(colon + 1);
    name.trim();
    name.toLowerCase();
    value.trim();

    if (name == "content-length") {
      contentLength = value.toInt();
    } else if (name == "x-setup-code") {
      setupCode = value;
    }
  }

  if (contentLength < 0 || contentLength > static_cast<int>(MAX_REQUEST_BODY)) {
    return false;
  }

  body = "";
  unsigned long bodyStarted = millis();
  while (body.length() < static_cast<unsigned int>(contentLength) &&
         millis() - bodyStarted < 1500) {
    while (client.available() &&
           body.length() < static_cast<unsigned int>(contentLength)) {
      body += static_cast<char>(client.read());
    }
    delay(1);
  }

  return body.length() == static_cast<unsigned int>(contentLength);
}

bool ProvisioningManager::hasValidSetupCode(
    const String &setupCode
) const {
  return setupCode.length() > 0 && setupCode.equals(_setupPassword);
}

void ProvisioningManager::routeRequest(
    WiFiClient &client,
    const String &method,
    const String &path,
    const String &setupCode,
    const String &body
) {
  if (method == "OPTIONS") {
    sendResponse(client, 204, "No Content", "text/plain", "");
    return;
  }
  if (method == "GET" && path == "/") {
    sendSetupPage(client);
    return;
  }
  if (method == "GET" && path == "/v1/provision/status") {
    sendStatus(client);
    return;
  }

  if (!hasValidSetupCode(setupCode)) {
    sendJsonError(
        client,
        401,
        "Unauthorized",
        "INVALID_SETUP_CODE",
        "Setup code is invalid"
    );
    return;
  }

  if (method == "GET" && path == "/v1/provision/networks") {
    sendNetworks(client);
    return;
  }
  if (method == "POST" && path == "/v1/provision/wifi") {
    acceptWifiCredentials(client, body);
    return;
  }
  if (method == "DELETE" && path == "/v1/provision/wifi") {
    clearStoredCredentials();
    setState(ProvisioningState::SETUP_AP, nullptr);
    sendResponse(client, 204, "No Content", "application/json", "");
    return;
  }

  sendJsonError(
      client,
      404,
      "Not Found",
      "NOT_FOUND",
      "Provisioning endpoint not found"
  );
}

void ProvisioningManager::sendResponse(
    WiFiClient &client,
    int statusCode,
    const char *statusText,
    const String &contentType,
    const String &body
) {
  client.print("HTTP/1.1 ");
  client.print(statusCode);
  client.print(' ');
  client.println(statusText);
  client.print("Content-Type: ");
  client.println(contentType);
  client.println("Cache-Control: no-store");
  client.println("Access-Control-Allow-Origin: *");
  client.println("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS");
  client.println("Access-Control-Allow-Headers: Content-Type, X-Setup-Code");
  client.print("Content-Length: ");
  client.println(body.length());
  client.println("Connection: close");
  client.println();
  if (body.length() > 0) {
    client.print(body);
  }
}

void ProvisioningManager::sendJsonError(
    WiFiClient &client,
    int statusCode,
    const char *statusText,
    const char *code,
    const char *message
) {
  DynamicJsonDocument document(384);
  JsonObject error = document.createNestedObject("error");
  error["code"] = code;
  error["message"] = message;
  String response;
  serializeJson(document, response);
  sendResponse(client, statusCode, statusText, "application/json", response);
}

void ProvisioningManager::sendStatus(WiFiClient &client) {
  DynamicJsonDocument document(384);
  document["version"] = 1;
  document["deviceId"] = _deviceId;
  document["state"] = stateName();
  if (_lastError[0] == '\0') {
    document["lastError"] = nullptr;
  } else {
    document["lastError"] = _lastError;
  }
  String response;
  serializeJson(document, response);
  sendResponse(client, 200, "OK", "application/json", response);
}

void ProvisioningManager::sendNetworks(WiFiClient &client) {
  DynamicJsonDocument document(2048);
  JsonArray networks = document.createNestedArray("networks");
  for (uint8_t i = 0; i < _networkCount; i++) {
    JsonObject network = networks.createNestedObject();
    network["ssid"] = _networkSsids[i];
    network["rssi"] = _networkRssi[i];
    network["secure"] = _networkSecure[i];
  }
  String response;
  serializeJson(document, response);
  sendResponse(client, 200, "OK", "application/json", response);
}

void ProvisioningManager::acceptWifiCredentials(
    WiFiClient &client,
    const String &body
) {
  if (_pendingCredentials) {
    sendJsonError(
        client,
        409,
        "Conflict",
        "PROVISIONING_BUSY",
        "The device is already applying WiFi credentials"
    );
    return;
  }

  DynamicJsonDocument document(512);
  DeserializationError parseError = deserializeJson(document, body);
  if (parseError) {
    sendJsonError(
        client,
        400,
        "Bad Request",
        "INVALID_REQUEST",
        "Request body must be valid JSON"
    );
    return;
  }

  const char *ssid = document["ssid"] | "";
  const char *password = document["password"] | "";
  size_t ssidLength = strlen(ssid);
  size_t passwordLength = strlen(password);
  if (ssidLength == 0 || ssidLength > 32 ||
      passwordLength > 63 ||
      (passwordLength > 0 && passwordLength < 8)) {
    sendJsonError(
        client,
        422,
        "Unprocessable Entity",
        "WIFI_CREDENTIALS_REJECTED",
        "SSID must be 1-32 bytes and password must be empty or 8-63 characters"
    );
    return;
  }

  copyString(_pendingSsid, sizeof(_pendingSsid), ssid);
  copyString(_pendingPassword, sizeof(_pendingPassword), password);
  _pendingCredentials = true;
  setState(ProvisioningState::WIFI_CONNECTING, nullptr);

  DynamicJsonDocument responseDocument(384);
  responseDocument["accepted"] = true;
  responseDocument["deviceId"] = _deviceId;
  responseDocument["state"] = "WIFI_CONNECTING";
  responseDocument["cloudPollAfterMs"] = 3000;
  String response;
  serializeJson(responseDocument, response);
  sendResponse(client, 202, "Accepted", "application/json", response);
}

void ProvisioningManager::sendSetupPage(WiFiClient &client) {
  String page = F(
      "<!doctype html><html><head><meta charset='utf-8'>"
      "<meta name='viewport' content='width=device-width,initial-scale=1'>"
      "<title>Shrimp IoT Setup</title></head><body>"
      "<h2>Shrimp IoT Wi-Fi Setup</h2>"
      "<p>Use this page only for local testing before the mobile app is ready.</p>"
      "<label>Setup code <input id='code' type='password'></label><br>"
      "<label>Wi-Fi SSID <input id='ssid'></label><br>"
      "<label>Wi-Fi password <input id='password' type='password'></label><br>"
      "<button onclick='applyWifi()'>Connect</button><pre id='result'></pre>"
      "<script>async function applyWifi(){const r=await fetch('/v1/provision/wifi',{"
      "method:'POST',headers:{'Content-Type':'application/json','X-Setup-Code':"
      "document.getElementById('code').value},body:JSON.stringify({ssid:"
      "document.getElementById('ssid').value,password:document.getElementById('password').value})});"
      "document.getElementById('result').textContent=await r.text();}</script>"
      "</body></html>"
  );
  sendResponse(client, 200, "OK", "text/html; charset=utf-8", page);
}

void ProvisioningManager::copyString(
    char *destination,
    size_t destinationSize,
    const char *source
) {
  if (destination == nullptr || destinationSize == 0) {
    return;
  }
  if (source == nullptr) {
    destination[0] = '\0';
    return;
  }
  strncpy(destination, source, destinationSize - 1);
  destination[destinationSize - 1] = '\0';
}

