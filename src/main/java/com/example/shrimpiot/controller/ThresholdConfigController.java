package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.ThresholdConfigRequest;
import com.example.shrimpiot.dto.ThresholdConfigResponse;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.ThresholdConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threshold-configs")
public class ThresholdConfigController {

    private final ThresholdConfigService service;
    private final AuthService authService;

    public ThresholdConfigController(ThresholdConfigService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    // 1. Tạo hoặc cập nhật ngưỡng cảnh báo (ADMIN hoặc user có quyền ao)
    @PostMapping
    public ResponseEntity<ApiResponse<ThresholdConfigResponse>> createOrUpdateThreshold(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ThresholdConfigRequest request
    ) {
        var user = authService.getCurrentUser(authorization);
        authService.validateAccessToPond(authorization, request.getPondId());

        String username = user.getUsername();
        ThresholdConfigResponse response = service.createOrUpdateThreshold(request, username);
        return ResponseEntity.ok(ApiResponse.ok("Threshold config updated successfully", response));
    }

    // 2. Lấy ngưỡng của một tham số trong ao (validate quyền)
    @GetMapping("/{pondId}/{parameterName}")
    public ResponseEntity<ApiResponse<ThresholdConfigResponse>> getThreshold(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId,
            @PathVariable String parameterName
    ) {
        authService.validateAccessToPond(authorization, pondId);
        ThresholdConfigResponse response = service.getThreshold(pondId, parameterName);
        return ResponseEntity.ok(ApiResponse.ok("Threshold config retrieved", response));
    }

    // 3. Lấy danh sách tất cả ngưỡng của một ao (validate quyền)
    @GetMapping("/{pondId}")
    public ResponseEntity<ApiResponse<List<ThresholdConfigResponse>>> getThresholdsByPond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId
    ) {
        authService.validateAccessToPond(authorization, pondId);
        List<ThresholdConfigResponse> response = service.getThresholdsByPond(pondId);
        return ResponseEntity.ok(ApiResponse.ok("Threshold configs retrieved", response));
    }

    // 4. Lấy danh sách ngưỡng enabled của ao
    @GetMapping("/{pondId}/enabled")
    public ResponseEntity<ApiResponse<List<ThresholdConfigResponse>>> getEnabledThresholds(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId
    ) {
        authService.validateAccessToPond(authorization, pondId);
        List<ThresholdConfigResponse> response = service.getEnabledThresholdsByPond(pondId);
        return ResponseEntity.ok(ApiResponse.ok("Enabled threshold configs retrieved", response));
    }

    // 5. Xóa một ngưỡng (ADMIN only)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteThreshold(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        service.deleteThreshold(id);
        return ResponseEntity.ok(ApiResponse.ok("Threshold config deleted successfully", null));
    }
}
