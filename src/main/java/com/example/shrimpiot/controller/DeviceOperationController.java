package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.MeasurementConfigRequest;
import com.example.shrimpiot.dto.OperationConfigResponse;
import com.example.shrimpiot.dto.OperationModeRequest;
import com.example.shrimpiot.dto.SalinityControlConfigRequest;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.DeviceOperationConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices/{deviceId}")
public class DeviceOperationController {
    private final AuthService authService;
    private final DeviceOperationConfigService configService;

    public DeviceOperationController(AuthService authService, DeviceOperationConfigService configService) {
        this.authService = authService;
        this.configService = configService;
    }

    @GetMapping("/operation-mode")
    public ResponseEntity<ApiResponse<OperationConfigResponse>> getOperationMode(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Device operation config", configService.getResponse(deviceId)));
    }

    @PatchMapping("/operation-mode")
    public ResponseEntity<ApiResponse<OperationConfigResponse>> updateOperationMode(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @RequestBody OperationModeRequest request
    ) {
        UserAccount actor = authService.getCurrentUser(authorization);
        authService.validateWriteAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Operation mode updated", configService.updateOperationMode(deviceId, request, actor.getUsername())));
    }

    @PatchMapping("/salinity-control-config")
    public ResponseEntity<ApiResponse<OperationConfigResponse>> updateSalinityConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @RequestBody SalinityControlConfigRequest request
    ) {
        UserAccount actor = authService.getCurrentUser(authorization);
        authService.validateWriteAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Salinity control config updated", configService.updateSalinityConfig(deviceId, request, actor.getUsername())));
    }

    @PatchMapping("/measurement-config")
    public ResponseEntity<ApiResponse<OperationConfigResponse>> updateMeasurementConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @RequestBody MeasurementConfigRequest request
    ) {
        UserAccount actor = authService.getCurrentUser(authorization);
        authService.validateWriteAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Measurement config updated", configService.updateMeasurementConfig(deviceId, request, actor.getUsername())));
    }
}
