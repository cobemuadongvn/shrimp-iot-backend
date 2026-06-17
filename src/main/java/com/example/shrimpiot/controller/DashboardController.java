package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.DashboardSummaryResponse;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final AuthService authService;

    public DashboardController(DashboardService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard summary", service.getSummary(deviceId)));
    }
}
