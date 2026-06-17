# Docker runtime fix: Mosquitto and pgAdmin

## What was fixed

1. `pgAdmin` no longer uses `admin@shrimp-iot.local` because recent pgAdmin versions reject `.local` domains.
   - New default: `admin@example.com`

2. Mosquitto now mounts the config file directly:
   - `./mosquitto/config/mosquitto.conf:/mosquitto/config/mosquitto.conf:ro`

This prevents the common Docker Desktop issue where running `docker compose` from the wrong working directory creates an empty `mosquitto/config` folder and Mosquitto then fails with:

```text
Error: Unable to open config file '/mosquitto/config/mosquitto.conf'.
```

## Correct startup steps

Run these commands from the project root, the folder that contains `docker-compose.yml`:

```bat
cd /d D:\shrimp-iot-backend-qcvn-ai-in-app-notification-offline-complete

docker compose down

docker compose up -d --force-recreate

docker ps
```

Expected containers:

```text
shrimp-postgres     Up
shrimp-mosquitto    Up
shrimp-pgadmin      Up
```

## If you have an old `.env`

If your local `.env` still contains:

```env
PGADMIN_DEFAULT_EMAIL=admin@shrimp-iot.local
```

change it to:

```env
PGADMIN_DEFAULT_EMAIL=admin@example.com
```

Then recreate pgAdmin:

```bat
docker rm -f shrimp-pgadmin
docker compose up -d pgadmin
```

## AI service note

The Python AI service does not create a `shrimp-iot-complete-work` folder. It only reads:

```text
ai-service/models/*.joblib
```

If a `shrimp-iot-complete-work` folder appears, it is usually from extracting an older zip, running old PowerShell helper scripts, or using VS Code/Copilot Java upgrade logs from a previous project path. It is not required by the AI service.
