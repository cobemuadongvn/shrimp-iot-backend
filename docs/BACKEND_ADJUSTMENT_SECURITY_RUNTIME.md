# Backend adjustment notes — security, MQTT and runtime safety

## Scope implemented in this package

This package keeps the current REST API compatible with the existing web/app, but adjusts the backend toward a safer IoT prototype architecture.

### 1. MQTT topic compatibility

Backend now accepts both topic styles:

- `shrimp-iot/devices/{deviceId}/telemetry`
- `aquaculture/{farmId}/{pondId}/{deviceId}/telemetry`

Command ACK can be sent to:

- `shrimp-iot/devices/{deviceId}/commands/ack`
- `aquaculture/{farmId}/{pondId}/{deviceId}/ack`

Recommended production topic remains:

```text
shrimp-iot/devices/{deviceId}/telemetry
shrimp-iot/devices/{deviceId}/commands
shrimp-iot/devices/{deviceId}/commands/ack
shrimp-iot/devices/{deviceId}/status
```

### 2. Telemetry payload compatibility

`SensorReadingRequest` accepts both camelCase and snake_case fields:

```json
{
  "deviceId": "device_01",
  "temperature": 31.38,
  "ph": 4.10,
  "ecValue": 0.48,
  "salinity": 0.24,
  "doValue": 2.16
}
```

or:

```json
{
  "node_code": "device_01",
  "temperature": 31.38,
  "ph": 4.10,
  "ec_value": 0.48,
  "salinity": 0.24,
  "dissolved_oxygen": 2.16
}
```

### 3. Command expiration

Relay commands now have `expiresAt`.

Default:

```properties
COMMAND_DEFAULT_EXPIRATION_SECONDS=60
```

Expired commands are marked `EXPIRED` and are not retried. This prevents old ON commands from being executed after backend/broker/device reconnects.

### 4. Demo data seeding

Demo accounts are now controlled by:

```properties
SEED_DEMO_DATA_ENABLED=true
```

For production-like deployment:

```properties
SEED_DEMO_DATA_ENABLED=false
```

### 5. MQTT security template

Development config remains anonymous for easy local testing:

```text
mosquitto/config/mosquitto.conf
```

A secure template is provided:

```text
mosquitto/config/mosquitto.secure.conf
mosquitto/config/aclfile.example
```

Generate password file:

```bash
mosquitto_passwd -c mosquitto/config/passwordfile backend_user
mosquitto_passwd mosquitto/config/passwordfile device_01
copy mosquitto/config/aclfile.example mosquitto/config/aclfile
```

Then configure Docker/Mosquitto to use `mosquitto.secure.conf`.

### 6. Database indexes

The frequently queried IoT tables now declare indexes:

- `sensor_readings(device_id, created_at)`
- `sensor_readings(status, created_at)`
- `device_commands(device_id, status)`
- `device_commands(device_id, created_at)`
- `device_commands(status, expires_at)`

For an existing database, check whether Hibernate created these indexes. In production, convert this into Flyway/Liquibase migration scripts.

## Not implemented yet

The following items are design recommendations but are not fully implemented in this patch because they require larger refactors:

1. Full `spring-boot-starter-security` filter chain with `SecurityContext`.
2. Per-device hashed API key/credential table.
3. Full state machine for salinity correction.
4. Hardware-level pump verification: flow sensor, water-level sensor, relay feedback and emergency stop.
5. Flyway/Liquibase migrations.
6. Complete unit/integration test suite.

## Presentation wording

Use:

```text
Smart Auto / Rule-based Auto Control
```

Do not claim this is a trained AI model unless a real ML service is added.
