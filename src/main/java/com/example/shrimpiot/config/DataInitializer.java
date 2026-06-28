package com.example.shrimpiot.config;

import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.shrimpiot.model.ApprovalStatus;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.DeviceRelay;
import com.example.shrimpiot.model.DeviceSensor;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.model.UserPondAccess;
import com.example.shrimpiot.repository.DeviceRelayRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.DeviceSensorRepository;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.UserAccountRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;

@Component
@ConditionalOnProperty(name = "seed.demo-data.enabled", havingValue = "true", matchIfMissing = false)
public class DataInitializer implements CommandLineRunner {

    @Value("${seed.demo-data.admin-password:}")
    private String adminPassword;

    @Value("${seed.demo-data.user-password:}")
    private String userPassword;

    @Value("${seed.demo-data.technician-password:}")
    private String technicianPassword;

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PondRepository pondRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceSensorRepository sensorRepository;
    private final DeviceRelayRepository relayRepository;
    private final UserPondAccessRepository accessRepository;

    public DataInitializer(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            PondRepository pondRepository,
            DeviceRepository deviceRepository,
            DeviceSensorRepository sensorRepository,
            DeviceRelayRepository relayRepository,
            UserPondAccessRepository accessRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pondRepository = pondRepository;
        this.deviceRepository = deviceRepository;
        this.sensorRepository = sensorRepository;
        this.relayRepository = relayRepository;
        this.accessRepository = accessRepository;
    }

    @Override
    public void run(String... args) {
        requireSeedPassword("SEED_ADMIN_PASSWORD", adminPassword);
        requireSeedPassword("SEED_USER_PASSWORD", userPassword);
        requireSeedPassword("SEED_TECH_PASSWORD", technicianPassword);

        // 1. Tạo các tài khoản người dùng
        UserAccount admin = createIfMissing("admin", "Quản trị hệ thống", adminPassword, RoleName.ADMIN);
        UserAccount user1 = createIfMissing("user", "Chủ ao nuôi A", userPassword, RoleName.USER);
        UserAccount user2 = createIfMissing("user2", "Chủ ao nuôi B", userPassword, RoleName.USER);
        UserAccount tech = createIfMissing("tech", "Kỹ thuật viên", technicianPassword, RoleName.TECHNICIAN);

        // 2. Tạo các Ao nuôi
        Pond pond1 = createPondIfMissing("Ao tôm thẻ 01", "Khu A - Bến Tre", 1000.0);
        Pond pond2 = createPondIfMissing("Ao tôm thẻ 02", "Khu A - Bến Tre", 1200.0);
        Pond pond3 = createPondIfMissing("Ao tôm thẻ 03", "Khu B - Bến Tre", 1500.0);

        // 3. Đăng ký và liên kết thiết bị
        Device dev1 = createDeviceIfMissing("device_01", "Bộ điều khiển Ao 1", pond1);
        Device dev2 = createDeviceIfMissing("device_02", "Bộ điều khiển Ao 2", pond2);
        Device dev3 = createDeviceIfMissing("device_03", "Bộ điều khiển Ao 3", pond3);

        // 4. Phân quyền truy cập
        // user1 truy cập Ao 1 và Ao 2
        grantAccessIfMissing(user1, pond1, "OWNER");
        grantAccessIfMissing(user1, pond2, "OWNER");
        // user2 truy cập Ao 3
        grantAccessIfMissing(user2, pond3, "OWNER");
        // tech truy cập Ao 1
        grantAccessIfMissing(tech, pond1, "READ_WRITE");

        // 5. Cấu hình cảm biến cho thiết bị device_01
        createSensorIfMissing(dev1, "TEMPERATURE", "Cảm biến Nhiệt độ", "D6", 20.0, 35.0);
        createSensorIfMissing(dev1, "PH", "Cảm biến pH", "A0", 6.5, 8.5);
        createSensorIfMissing(dev1, "EC", "Cảm biến EC", "A1", 0.0, 100.0);
        createSensorIfMissing(dev1, "SALINITY", "Cảm biến Độ mặn", "A1", 0.0, 35.0);
        createSensorIfMissing(dev1, "DO", "Cảm biến Oxy hòa tan (DO)", "A2", 4.0, 20.0);

        // Cấu hình cảm biến cho thiết bị device_02
        createSensorIfMissing(dev2, "TEMPERATURE", "Cảm biến Nhiệt độ", "D6", 20.0, 35.0);
        createSensorIfMissing(dev2, "PH", "Cảm biến pH", "A0", 6.5, 8.5);
        createSensorIfMissing(dev2, "SALINITY", "Cảm biến Độ mặn", "A1", 0.0, 35.0);
        createSensorIfMissing(dev2, "DO", "Cảm biến Oxy hòa tan (DO)", "A2", 4.0, 20.0);

        // Cấu hình cảm biến cho thiết bị device_03
        createSensorIfMissing(dev3, "TEMPERATURE", "Cảm biến Nhiệt độ", "D6", 20.0, 35.0);
        createSensorIfMissing(dev3, "PH", "Cảm biến pH", "A0", 6.5, 8.5);
        createSensorIfMissing(dev3, "SALINITY", "Cảm biến Độ mặn", "A1", 0.0, 35.0);
        createSensorIfMissing(dev3, "DO", "Cảm biến Oxy hòa tan (DO)", "A2", 4.0, 20.0);

        // 6. Cấu hình rơ-le cho thiết bị device_01
        createRelayIfMissing(dev1, 1, "Bơm nước vào buồng đo", "MEASUREMENT_INLET_PUMP", 2);
        createRelayIfMissing(dev1, 2, "Bơm xả nước khỏi buồng đo", "MEASUREMENT_OUTLET_PUMP", 3);
        createRelayIfMissing(dev1, 3, "Bơm xả nước ao khi độ mặn cao", "POND_SALTY_WATER_DRAIN_PUMP", 4);
        createRelayIfMissing(dev1, 4, "Bơm nước ngọt vào ao", "FRESHWATER_INLET_PUMP", 5);

        // Cấu hình rơ-le cho thiết bị device_02 và device_03
        createRelayIfMissing(dev2, 1, "Bơm nước vào buồng đo", "MEASUREMENT_INLET_PUMP", 2);
        createRelayIfMissing(dev2, 2, "Bơm xả nước khỏi buồng đo", "MEASUREMENT_OUTLET_PUMP", 3);
        createRelayIfMissing(dev2, 3, "Bơm xả nước ao khi độ mặn cao", "POND_SALTY_WATER_DRAIN_PUMP", 4);
        createRelayIfMissing(dev2, 4, "Bơm nước ngọt vào ao", "FRESHWATER_INLET_PUMP", 5);

        createRelayIfMissing(dev3, 1, "Bơm nước vào buồng đo", "MEASUREMENT_INLET_PUMP", 2);
        createRelayIfMissing(dev3, 2, "Bơm xả nước khỏi buồng đo", "MEASUREMENT_OUTLET_PUMP", 3);
        createRelayIfMissing(dev3, 3, "Bơm xả nước ao khi độ mặn cao", "POND_SALTY_WATER_DRAIN_PUMP", 4);
        createRelayIfMissing(dev3, 4, "Bơm nước ngọt vào ao", "FRESHWATER_INLET_PUMP", 5);
    }

    private void requireSeedPassword(String environmentName, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    environmentName + " must be set when SEED_DEMO_DATA_ENABLED=true"
            );
        }
    }

    private UserAccount createIfMissing(String username, String fullName, String rawPassword, RoleName role) {
        Optional<UserAccount> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            UserAccount found = existing.get();
            if (found.getApprovalStatus() == null) {
                found.setApprovalStatus(ApprovalStatus.APPROVED);
                found.setApprovedBy("SYSTEM_INIT");
                found.setApprovedAt(java.time.LocalDateTime.now());
                found.setActive(true);
                return userRepository.save(found);
            }
            return found;
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedBy("SYSTEM_INIT");
        user.setApprovedAt(java.time.LocalDateTime.now());

        return userRepository.save(user);
    }

    private Pond createPondIfMissing(String name, String location, Double area) {
        // Tìm ao theo tên để tránh trùng lặp khi chạy ddl-auto update
        return pondRepository.findAll().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Pond pond = new Pond();
                    pond.setName(name);
                    pond.setLocation(location);
                    pond.setAreaSquareMeters(area);
                    pond.setSpeciesType("Tôm thẻ chân trắng");
                    pond.setPondType("Ao nuôi bán thâm canh");
                    pond.setRegion(location);
                    pond.setStatus("ACTIVE");
                    return pondRepository.save(pond);
                });
    }

    private Device createDeviceIfMissing(String deviceId, String name, Pond pond) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseGet(() -> {
                    Device device = new Device();
                    device.setDeviceId(deviceId);
                    device.setName(name);
                    device.setPond(pond);
                    device.setStatus("ACTIVE");
                    return deviceRepository.save(device);
                });
    }

    private void grantAccessIfMissing(UserAccount user, Pond pond, String accessType) {
        if (!accessRepository.existsByUserAndPond(user, pond)) {
            UserPondAccess access = new UserPondAccess();
            access.setUser(user);
            access.setPond(pond);
            access.setAccessType(accessType);
            accessRepository.save(access);
        }
    }

    private void createSensorIfMissing(Device device, String sensorType, String name, String pin, Double minVal, Double maxVal) {
        if (sensorRepository.findByDeviceDeviceIdAndSensorType(device.getDeviceId(), sensorType).isEmpty()) {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setDevice(device);
            sensor.setSensorType(sensorType);
            sensor.setName(name);
            sensor.setPin(pin);
            sensor.setMinThreshold(minVal);
            sensor.setMaxThreshold(maxVal);
            sensor.setStatus("ACTIVE");
            sensorRepository.save(sensor);
        }
    }

    private void createRelayIfMissing(Device device, Integer relayNo, String name, String type, Integer pin) {
        relayRepository.findByDeviceDeviceIdAndRelayNo(device.getDeviceId(), relayNo)
                .ifPresentOrElse(existing -> {
                    existing.setName(name);
                    existing.setRelayType(type);
                    existing.setPin(pin);
                    if (existing.getLocked() == null) {
                        existing.setLocked(false);
                    }
                    relayRepository.save(existing);
                }, () -> {
                    DeviceRelay relay = new DeviceRelay();
                    relay.setDevice(device);
                    relay.setRelayNo(relayNo);
                    relay.setName(name);
                    relay.setRelayType(type);
                    relay.setPin(pin);
                    relay.setStatus("OFF");
                    relay.setLocked(false);
                    relayRepository.save(relay);
                });
    }
}
