package com.example.shrimpiot.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.shrimpiot.dto.ApiResponse;
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
import com.example.shrimpiot.repository.UserPondAccessRepository;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.CommandService;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final PondRepository pondRepository;
    private final DeviceSensorRepository sensorRepository;
    private final DeviceRelayRepository relayRepository;
    private final UserPondAccessRepository accessRepository;
    private final AuthService authService;
    private final CommandService commandService;
    private final com.example.shrimpiot.service.RelayStateService relayStateService;

    public DeviceController(
            DeviceRepository deviceRepository,
            PondRepository pondRepository,
            DeviceSensorRepository sensorRepository,
            DeviceRelayRepository relayRepository,
            UserPondAccessRepository accessRepository,
            AuthService authService,
            CommandService commandService,
            com.example.shrimpiot.service.RelayStateService relayStateService
    ) {
        this.deviceRepository = deviceRepository;
        this.pondRepository = pondRepository;
        this.sensorRepository = sensorRepository;
        this.relayRepository = relayRepository;
        this.accessRepository = accessRepository;
        this.authService = authService;
        this.commandService = commandService;
        this.relayStateService = relayStateService;
    }

    // 1. Đăng ký thiết bị phần cứng mới.
    // ADMIN có thể tạo thiết bị chưa gán ao.
    // TECHNICIAN chỉ được tạo thiết bị trong ao mà họ có quyền OWNER/READ_WRITE/CONTROL.
    @PostMapping
    public ResponseEntity<ApiResponse<Device>> registerDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "pondId", required = false) Long pondId,
            @RequestBody Device device
    ) {
        UserAccount user = requireDeviceManager(authorization);
        if (!authService.isRealDeviceId(device.getDeviceId())) {
            throw new IllegalArgumentException("Invalid device id for physical device: " + device.getDeviceId());
        }
        if (deviceRepository.existsByDeviceId(device.getDeviceId())) {
            throw new IllegalArgumentException("Device ID already exists: " + device.getDeviceId());
        }

        Long targetPondId = pondId;
        if (targetPondId == null && device.getPond() != null) {
            targetPondId = device.getPond().getId();
        }

        if (targetPondId != null) {
            Long finalTargetPondId = targetPondId;
            Pond pond = pondRepository.findById(finalTargetPondId)
                    .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + finalTargetPondId));
            ensureCanManagePond(user, pond);
            device.setPond(pond);
        } else if (user.getRole() == RoleName.TECHNICIAN) {
            throw new SecurityException("TECHNICIAN must assign new device to a pond they can manage");
        }

        Device saved = deviceRepository.save(device);
        return ResponseEntity.ok(ApiResponse.ok("Device registered successfully", saved));
    }

    // 2. Lấy danh sách thiết bị (ADMIN xem tất cả ACTIVE; USER/TECHNICIAN xem ACTIVE devices trong ao được gán).
    // Không trả các mã ao cũ bị lưu nhầm trong bảng devices, ví dụ deviceId = pond_01.
    @GetMapping
    public ResponseEntity<ApiResponse<List<Device>>> getDevices(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        List<Device> devices;

        if (user.getRole() == RoleName.ADMIN) {
            devices = deviceRepository.findByStatus("ACTIVE");
        } else {
            List<Pond> userPonds = accessRepository.findByUser(user)
                    .stream()
                    .map(UserPondAccess::getPond)
                    .toList();

            devices = userPonds.isEmpty()
                    ? List.of()
                    : deviceRepository.findByPondInAndStatus(userPonds, "ACTIVE");
        }

        devices = devices.stream()
                .filter(this::isRealVisibleDevice)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Devices retrieved successfully", devices));
    }

    // 3. Xem chi tiết thiết bị (Phân quyền kiểm tra validateAccessToDevice)
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Device>> getDeviceDetails(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        Device device = authService.requireActiveRealDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Device details", device));
    }

    // 4. Liên kết thiết bị với ao nuôi.
    // ADMIN có thể gán mọi thiết bị. TECHNICIAN chỉ được gán vào ao họ có quyền quản lý,
    // và nếu thiết bị đã thuộc ao khác thì cũng phải có quyền quản lý ao hiện tại.
    @PostMapping("/{deviceId}/link")
    public ResponseEntity<ApiResponse<Device>> linkDeviceToPond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @RequestParam Long pondId
    ) {
        UserAccount user = requireDeviceManager(authorization);
        Device device = authService.requireActiveRealDevice(deviceId);

        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));

        if (device.getPond() != null) {
            ensureCanManagePond(user, device.getPond());
        }
        ensureCanManagePond(user, pond);

        device.setPond(pond);
        Device saved = deviceRepository.save(device);
        
        // Khởi tạo RelayState cho tất cả relay của device này
        List<DeviceRelay> relays = relayRepository.findByDeviceDeviceId(deviceId);
        relayStateService.initializeRelayStatesForDevice(deviceId, relays);
        
        return ResponseEntity.ok(ApiResponse.ok("Device linked to pond successfully", saved));
    }

    // 5. Xem danh sách cảm biến gắn với thiết bị (validateAccessToDevice)
    @GetMapping("/{deviceId}/sensors")
    public ResponseEntity<ApiResponse<List<DeviceSensor>>> getDeviceSensors(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        authService.requireActiveRealDevice(deviceId);
        List<DeviceSensor> sensors = sensorRepository.findByDeviceDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Device sensors retrieved", sensors));
    }

    // 6. Xem danh sách rơ-le gắn với thiết bị (validateAccessToDevice)
    @GetMapping("/{deviceId}/relays")
    public ResponseEntity<ApiResponse<List<DeviceRelay>>> getDeviceRelays(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        authService.requireActiveRealDevice(deviceId);
        List<DeviceRelay> relays = relayRepository.findByDeviceDeviceId(deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Device relays retrieved", relays));
    }


    // 6.1. Khóa riêng từng relay/bơm của thiết bị.
    // ADMIN và TECHNICIAN đều được khóa nếu có quyền quản lý thiết bị theo Cách 1.
    // Khi relay bị khóa, backend sẽ chặn cả lệnh MANUAL và AUTO tới relay đó.
    @PatchMapping("/{deviceId}/relays/{relayNo}/lock")
    public ResponseEntity<ApiResponse<DeviceRelay>> lockRelay(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @PathVariable Integer relayNo
    ) {
        UserAccount user = requireDeviceManager(authorization);
        Device device = authService.requireActiveRealDevice(deviceId);
        ensureCanManageDevice(user, device);

        DeviceRelay relay = requireRelay(deviceId, relayNo);
        relay.setLocked(true);
        relay.setLockedBy(user.getUsername());
        relay.setLockedAt(LocalDateTime.now());
        DeviceRelay saved = relayRepository.save(relay);

        int expiredCommands = commandService.expirePendingCommandsForRelay(
                deviceId,
                relayNo,
                "Command expired because relay " + relayNo + " was locked by " + user.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Relay locked successfully. Expired pending commands: " + expiredCommands,
                saved
        ));
    }

    // 6.2. Mở khóa riêng từng relay/bơm của thiết bị.
    // Mở khóa chỉ cho phép điều khiển lại, không tự động bật bơm.
    @PatchMapping("/{deviceId}/relays/{relayNo}/unlock")
    public ResponseEntity<ApiResponse<DeviceRelay>> unlockRelay(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @PathVariable Integer relayNo
    ) {
        UserAccount user = requireDeviceManager(authorization);
        Device device = authService.requireActiveRealDevice(deviceId);
        ensureCanManageDevice(user, device);

        DeviceRelay relay = requireRelay(deviceId, relayNo);
        relay.setLocked(false);
        relay.setLockedBy(null);
        relay.setLockedAt(null);
        DeviceRelay saved = relayRepository.save(relay);

        return ResponseEntity.ok(ApiResponse.ok("Relay unlocked successfully", saved));
    }

    private DeviceRelay requireRelay(String deviceId, Integer relayNo) {
        if (relayNo == null || relayNo < 1 || relayNo > 4) {
            throw new IllegalArgumentException("relayNo must be from 1 to 4");
        }
        return relayRepository.findByDeviceDeviceIdAndRelayNo(deviceId, relayNo)
                .orElseThrow(() -> new IllegalArgumentException("Relay not found: device=" + deviceId + ", relay=" + relayNo));
    }

    // 7. Cập nhật thông tin thiết bị, gồm tọa độ bản đồ và vị trí lắp đặt.
    // TECHNICIAN được sửa thiết bị thuộc ao họ có quyền OWNER/READ_WRITE/CONTROL.
    @PutMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<Device>> updateDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @RequestBody Device details
    ) {
        UserAccount user = requireDeviceManager(authorization);
        Device device = authService.requireActiveRealDevice(deviceId);
        ensureCanManageDevice(user, device);

        device.setName(details.getName() != null ? details.getName() : device.getName());
        device.setLatitude(details.getLatitude() != null ? details.getLatitude() : device.getLatitude());
        device.setLongitude(details.getLongitude() != null ? details.getLongitude() : device.getLongitude());
        device.setInstallationPosition(details.getInstallationPosition() != null ? details.getInstallationPosition() : device.getInstallationPosition());

        // Chỉ ADMIN được cập nhật trực tiếp status/connectionStatus qua PUT.
        // TECHNICIAN dùng API activate/deactivate riêng để thay đổi trạng thái thiết bị.
        if (user.getRole() == RoleName.ADMIN) {
            device.setStatus(details.getStatus() != null ? details.getStatus() : device.getStatus());
            device.setConnectionStatus(details.getConnectionStatus() != null ? details.getConnectionStatus() : device.getConnectionStatus());
        }

        Device saved = deviceRepository.save(device);
        return ResponseEntity.ok(ApiResponse.ok("Device updated successfully", saved));
    }

    // 8. Vô hiệu hóa thiết bị thay vì xóa cứng.
    // TECHNICIAN được vô hiệu hóa thiết bị thuộc ao họ có quyền OWNER/READ_WRITE/CONTROL.
    @PatchMapping("/{deviceId}/deactivate")
    public ResponseEntity<ApiResponse<Device>> deactivateDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        UserAccount user = requireDeviceManager(authorization);
        Device device = authService.requireActiveRealDevice(deviceId);
        ensureCanManageDevice(user, device);
        device.setStatus("INACTIVE");
        Device saved = deviceRepository.save(device);
        return ResponseEntity.ok(ApiResponse.ok("Device deactivated", saved));
    }

    // 9. Kích hoạt lại thiết bị.
    // TECHNICIAN được kích hoạt lại thiết bị thuộc ao họ có quyền OWNER/READ_WRITE/CONTROL.
    @PatchMapping("/{deviceId}/activate")
    public ResponseEntity<ApiResponse<Device>> activateDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        UserAccount user = requireDeviceManager(authorization);
        Device device = requireRealDeviceForActivation(deviceId);
        ensureCanManageDevice(user, device);
        device.setStatus("ACTIVE");
        Device saved = deviceRepository.save(device);
        return ResponseEntity.ok(ApiResponse.ok("Device activated", saved));
    }

    private Device requireRealDeviceForActivation(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        if (!authService.isRealDeviceId(device.getDeviceId())) {
            throw new IllegalArgumentException("Invalid device id for physical device: " + deviceId);
        }
        return device;
    }

    private UserAccount requireDeviceManager(String authorization) {
        return authService.requireAnyRole(authorization, RoleName.ADMIN, RoleName.TECHNICIAN);
    }

    private void ensureCanManageDevice(UserAccount user, Device device) {
        if (user.getRole() == RoleName.ADMIN) {
            return;
        }
        if (device.getPond() == null) {
            throw new SecurityException("TECHNICIAN cannot manage a device that is not assigned to any pond");
        }
        ensureCanManagePond(user, device.getPond());
    }

    private void ensureCanManagePond(UserAccount user, Pond pond) {
        if (user.getRole() == RoleName.ADMIN) {
            return;
        }
        if (user.getRole() != RoleName.TECHNICIAN) {
            throw new SecurityException("Only ADMIN or TECHNICIAN can manage devices");
        }

        UserPondAccess access = accessRepository.findByUserAndPond(user, pond)
                .orElseThrow(() -> new SecurityException("TECHNICIAN does not have access to pond: " + pond.getName()));

        String accessType = access.getAccessType() == null ? "" : access.getAccessType().trim().toUpperCase();
        if (!(accessType.equals("OWNER") || accessType.equals("READ_WRITE") || accessType.equals("CONTROL"))) {
            throw new SecurityException("TECHNICIAN needs OWNER/READ_WRITE/CONTROL access to manage devices in pond: " + pond.getName());
        }
    }

    private boolean isRealVisibleDevice(Device device) {
        if (device == null || device.getDeviceId() == null) {
            return false;
        }
        String normalizedDeviceId = device.getDeviceId().trim().toLowerCase();
        return !normalizedDeviceId.startsWith("pond_");
    }

}
