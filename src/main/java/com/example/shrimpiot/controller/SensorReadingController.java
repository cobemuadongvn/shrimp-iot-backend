package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.SensorReadingRequest;
import com.example.shrimpiot.dto.SensorReadingResponse;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.SensorReadingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/readings")
public class SensorReadingController {

    private final SensorReadingService service;
    private final AuthService authService;

    public SensorReadingController(SensorReadingService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    // Arduino dùng X-API-Key để gửi dữ liệu cảm biến. Không dùng token user.
    @PostMapping
    public ResponseEntity<ApiResponse<SensorReadingResponse>> createReading(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody SensorReadingRequest request
    ) {
        SensorReadingResponse response = service.saveReading(request, apiKey);
        return ResponseEntity.ok(ApiResponse.ok("Sensor reading saved", response));
    }

    // App/Web phải đăng nhập và gửi Authorization: Bearer <token>
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<SensorReadingResponse>> getLatest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Latest sensor reading", service.getLatest(deviceId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<SensorReadingResponse>>> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Sensor reading history", service.getHistory(deviceId, limit)));
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<SensorReadingResponse>>> getRange(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Sensor readings by time range", service.getRange(deviceId, from, to)));
    }
}
