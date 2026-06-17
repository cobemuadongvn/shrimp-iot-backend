package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.DeviceLatestStateResponse;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.DeviceLatestStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceLatestStateController {
    private final DeviceLatestStateService latestStateService;
    private final AuthService authService;

    public DeviceLatestStateController(DeviceLatestStateService latestStateService, AuthService authService) {
        this.latestStateService = latestStateService;
        this.authService = authService;
    }

    @GetMapping("/{deviceId}/latest-state")
    public ResponseEntity<ApiResponse<DeviceLatestStateResponse>> getLatestState(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Latest device state retrieved", latestStateService.getLatestState(deviceId)));
    }
}
