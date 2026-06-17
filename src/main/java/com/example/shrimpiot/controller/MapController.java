package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.MapDeviceResponse;
import com.example.shrimpiot.dto.MapPondResponse;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.model.UserPondAccess;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;
import com.example.shrimpiot.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private final AuthService authService;
    private final PondRepository pondRepository;
    private final DeviceRepository deviceRepository;
    private final UserPondAccessRepository accessRepository;

    public MapController(AuthService authService,
                         PondRepository pondRepository,
                         DeviceRepository deviceRepository,
                         UserPondAccessRepository accessRepository) {
        this.authService = authService;
        this.pondRepository = pondRepository;
        this.deviceRepository = deviceRepository;
        this.accessRepository = accessRepository;
    }

    @GetMapping("/ponds")
    public ResponseEntity<ApiResponse<List<MapPondResponse>>> getMapPonds(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        List<Pond> ponds = user.getRole() == RoleName.ADMIN
                ? pondRepository.findAll()
                : accessRepository.findByUser(user).stream().map(UserPondAccess::getPond).toList();

        List<MapPondResponse> response = ponds.stream()
                .map(pond -> new MapPondResponse(
                        pond,
                        deviceRepository.findByPondAndStatus(pond, "ACTIVE")
                                .stream()
                                .filter(this::isRealVisibleDevice)
                                .map(MapDeviceResponse::new)
                                .toList()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Map ponds", response));
    }

    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<MapDeviceResponse>>> getMapDevices(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        List<Device> devices;
        if (user.getRole() == RoleName.ADMIN) {
            devices = deviceRepository.findByStatus("ACTIVE");
        } else {
            List<Pond> ponds = accessRepository.findByUser(user).stream().map(UserPondAccess::getPond).toList();
            devices = ponds.isEmpty()
                    ? List.of()
                    : deviceRepository.findByPondInAndStatus(ponds, "ACTIVE");
        }

        List<MapDeviceResponse> response = devices.stream()
                .filter(this::isRealVisibleDevice)
                .map(MapDeviceResponse::new)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Map devices", response));
    }

    private boolean isRealVisibleDevice(Device device) {
        if (device == null || device.getDeviceId() == null) {
            return false;
        }
        String normalizedDeviceId = device.getDeviceId().trim().toLowerCase();
        return !normalizedDeviceId.startsWith("pond_");
    }
}
