package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.SensorCalibrationRequest;
import com.example.shrimpiot.dto.SensorCalibrationResponse;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.SensorCalibrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices/{deviceId}/calibrations")
public class SensorCalibrationController {
    private final SensorCalibrationService calibrationService;
    private final AuthService authService;

    public SensorCalibrationController(SensorCalibrationService calibrationService, AuthService authService) {
        this.calibrationService = calibrationService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SensorCalibrationResponse>>> getCalibrations(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Sensor calibrations retrieved", calibrationService.getCalibrations(deviceId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SensorCalibrationResponse>> createCalibration(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @Valid @RequestBody SensorCalibrationRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("Sensor calibration created", calibrationService.createCalibration(deviceId, request)));
    }

    @PutMapping("/{calibrationId}")
    public ResponseEntity<ApiResponse<SensorCalibrationResponse>> updateCalibration(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @PathVariable Long calibrationId,
            @Valid @RequestBody SensorCalibrationRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("Sensor calibration updated", calibrationService.updateCalibration(deviceId, calibrationId, request)));
    }

    @DeleteMapping("/{calibrationId}")
    public ResponseEntity<ApiResponse<Void>> deactivateCalibration(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @PathVariable Long calibrationId
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        calibrationService.deactivateCalibration(deviceId, calibrationId);
        return ResponseEntity.ok(ApiResponse.ok("Sensor calibration deactivated", null));
    }
}
