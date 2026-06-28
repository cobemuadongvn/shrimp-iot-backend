#ifndef SHRIMP_IOT_PROVISIONING_H
#define SHRIMP_IOT_PROVISIONING_H

#include <Arduino.h>
#include <WiFiS3.h>

enum class ProvisioningState : uint8_t {
  SETUP_AP,
  WIFI_CONNECTING,
  MQTT_CONNECTING,
  ONLINE,
  WIFI_FAILED,
  MQTT_FAILED
};

class ProvisioningManager {
 public:
  ProvisioningManager();

  void begin(
      const char *deviceId,
      const char *setupSsid,
      const char *setupPassword,
      uint8_t resetPin
  );

  bool resetRequestedAtBoot(unsigned long holdMs = 5000);
  bool loadStoredCredentials(
      char *ssidOut,
      size_t ssidSize,
      char *passwordOut,
      size_t passwordSize
  );
  bool saveCredentials(const char *ssid, const char *password);
  void clearStoredCredentials();

  bool start(const char *lastError = nullptr);
  void stop();
  void loop();
  bool isActive() const;

  bool takePendingCredentials(
      char *ssidOut,
      size_t ssidSize,
      char *passwordOut,
      size_t passwordSize
  );
  bool consumeTimeoutRequest();

  void setState(ProvisioningState state, const char *lastError = nullptr);
  ProvisioningState state() const;
  const char *stateName() const;
  const char *lastError() const;
  const char *setupSsid() const;

 private:
  static const uint32_t STORAGE_MAGIC = 0x53485250;
  static const uint16_t STORAGE_VERSION = 1;
  static const uint8_t MAX_NETWORKS = 10;
  static const size_t MAX_REQUEST_BODY = 512;
  static const unsigned long AP_IDLE_TIMEOUT_MS = 10UL * 60UL * 1000UL;

  struct __attribute__((packed)) StoredWifiConfig {
    uint32_t magic;
    uint16_t version;
    char ssid[33];
    char password[64];
    uint32_t checksum;
  };

  WiFiServer _server;
  bool _active;
  bool _pendingCredentials;
  bool _timeoutRequested;
  uint8_t _resetPin;
  unsigned long _lastActivityMs;
  ProvisioningState _state;

  char _deviceId[65];
  char _setupSsid[33];
  char _setupPassword[64];
  char _lastError[49];
  char _pendingSsid[33];
  char _pendingPassword[64];

  String _networkSsids[MAX_NETWORKS];
  int32_t _networkRssi[MAX_NETWORKS];
  bool _networkSecure[MAX_NETWORKS];
  uint8_t _networkCount;

  uint32_t calculateChecksum(const StoredWifiConfig &config) const;
  bool isStoredConfigValid(const StoredWifiConfig &config) const;
  void scanNetworksBeforeAp();
  void handleClient(WiFiClient &client);
  bool readHttpRequest(
      WiFiClient &client,
      String &method,
      String &path,
      String &setupCode,
      String &body
  );
  bool hasValidSetupCode(const String &setupCode) const;
  void routeRequest(
      WiFiClient &client,
      const String &method,
      const String &path,
      const String &setupCode,
      const String &body
  );
  void sendResponse(
      WiFiClient &client,
      int statusCode,
      const char *statusText,
      const String &contentType,
      const String &body
  );
  void sendJsonError(
      WiFiClient &client,
      int statusCode,
      const char *statusText,
      const char *code,
      const char *message
  );
  void sendStatus(WiFiClient &client);
  void sendNetworks(WiFiClient &client);
  void acceptWifiCredentials(WiFiClient &client, const String &body);
  void sendSetupPage(WiFiClient &client);
  static void copyString(char *destination, size_t destinationSize, const char *source);
};

#endif
