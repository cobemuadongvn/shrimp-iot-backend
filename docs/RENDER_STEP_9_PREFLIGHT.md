# Step 9 - Render Preflight

Trang thai hien tai: dang chuan bi deploy Render, chua tao service that.

## Da xac nhan

- Repo da co `render.yaml`, `Dockerfile`, `.dockerignore` va GitHub Actions workflow.
- Backend Render service dung Docker, region Singapore, health check `/api/health/ready`.
- Secret local quan trong van dang bi Git ignore:
- `.env.supabase.local`
- `.env.mqtt-cloud.local`
- `arduino/shrimp_iot_uno_r4_complete/arduino_secrets.h`
- Quet lai tracked files chi voi nhom `PASSWORD` / `TOKEN` / `API_KEY` / `SECRET`: khong thay bi mat local bi lot vao Git.
- Java 21 co san tren may.
- Ket qua test gan nhat trong `target/surefire-reports` ngay `2026-06-28` deu PASS:
- `DeploymentConfigTest`
- `MqttConnectionOptionsFactoryTest`
- `DeviceProvisioningApiIntegrationTest`
- `DeviceProvisioningServiceTest`
- Remote GitHub da duoc cau hinh san:
- `origin = https://github.com/cobemuadongvn/shrimp-iot-backend.git`

## Chua lam o buoc 9

- Chua tao tai khoan va service tren Render.
- Chua push dot thay doi hien tai len GitHub.
- Chua nhap environment variables vao Render Dashboard.
- Chua test URL cloud cua backend.
- Chua deploy AI service that.

## Viec tiep theo

1. Dang ky / dang nhap Render.
2. Push code hien tai len GitHub.
3. Tao Render Web Service cho backend.
4. Nhap bien moi truong Supabase va HiveMQ.
5. Deploy backend va test health URL cloud.
6. Neu can moi bat AI service.

## Ghi chu

- Tren may nay khong goi duoc lenh `mvn` global, nen minh dua vao ket qua surefire vua tao trong `target/` va CI workflow da commit san de xac nhan phan test.
- Buoc tao service that co the phat sinh chi phi, nen can nguoi dung thao tac tai khoan Render khi toi doan nay.
