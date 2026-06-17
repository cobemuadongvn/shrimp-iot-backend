package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.MeasurementCycleRequest;
import com.example.shrimpiot.dto.MeasurementCycleResponse;
import com.example.shrimpiot.dto.SalinityCorrectionRequest;
import com.example.shrimpiot.dto.SalinityCorrectionResponse;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.MeasurementCycleService;
import com.example.shrimpiot.service.SalinityControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sampling")
public class SamplingController {
    private final AuthService authService;
    private final MeasurementCycleService measurementCycleService;
    private final SalinityControlService salinityControlService;

    public SamplingController(AuthService authService, MeasurementCycleService measurementCycleService, SalinityControlService salinityControlService) {
        this.authService = authService;
        this.measurementCycleService = measurementCycleService;
        this.salinityControlService = salinityControlService;
    }

    /**
     * API chính cho nút "Đo ngay" trên web/app.
     * Bơm 1 chỉ bắt đầu chạy khi người dùng gọi API này.
     * Backend không tự chạy bơm 1 theo lịch nếu frontend không gửi lệnh đo.
     */
    @PostMapping("/measurement/measure-now")
    public ResponseEntity<ApiResponse<MeasurementCycleResponse>> measureNow(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody MeasurementCycleRequest request
    ) {
        UserAccount actor = authService.getCurrentUser(authorization);
        authService.validateWriteAccessToDevice(authorization, request.getDeviceId());
        return ResponseEntity.ok(ApiResponse.ok("Measure-now cycle started", measurementCycleService.start(request, actor.getUsername())));
    }

    /**
     * Giữ lại endpoint cũ để tương thích với web/app đang gọi /measurement/start.
     * Logic giống /measurement/measure-now.
     */
    @PostMapping("/measurement/start")
    public ResponseEntity<ApiResponse<MeasurementCycleResponse>> startMeasurement(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody MeasurementCycleRequest request
    ) {
        return measureNow(authorization, request);
    }

    @GetMapping("/measurement/current")
    public ResponseEntity<ApiResponse<MeasurementCycleResponse>> currentMeasurement(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Current measurement cycle", measurementCycleService.getCurrent(deviceId)));
    }

    @GetMapping("/measurement/history")
    public ResponseEntity<ApiResponse<List<MeasurementCycleResponse>>> measurementHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Measurement cycle history", measurementCycleService.history(deviceId, limit)));
    }

    /**
     * Bắt đầu chu trình xử lý độ mặn thủ công từ web/app.
     * Chế độ MANUAL vẫn cho phép chủ ao/kỹ thuật viên chủ động gọi API này nếu muốn chạy chu trình tự động 1 lần.
     */
    @PostMapping("/salinity/start")
    public ResponseEntity<ApiResponse<SalinityCorrectionResponse>> startSalinityCorrection(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SalinityCorrectionRequest request
    ) {
        UserAccount actor = authService.getCurrentUser(authorization);
        authService.validateWriteAccessToDevice(authorization, request.getDeviceId());
        return ResponseEntity.ok(ApiResponse.ok("Salinity correction cycle started", salinityControlService.startManualOrAdmin(request, actor.getUsername())));
    }

    @GetMapping("/salinity/current")
    public ResponseEntity<ApiResponse<SalinityCorrectionResponse>> currentSalinityCorrection(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Current salinity correction cycle", salinityControlService.getCurrent(deviceId)));
    }

    @GetMapping("/salinity/history")
    public ResponseEntity<ApiResponse<List<SalinityCorrectionResponse>>> salinityCorrectionHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Salinity correction cycle history", salinityControlService.history(deviceId, limit)));
    }
}
