package com.example.shrimpiot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shrimpiot.dto.AlertResponse;
import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.model.Alert;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.repository.AlertRepository;
import com.example.shrimpiot.service.AlertService;
import com.example.shrimpiot.service.AuthService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService alertService;
    private final AuthService authService;
    private final AlertRepository alertRepository;

    public AlertController(AlertService alertService, AuthService authService, AlertRepository alertRepository) {
        this.alertService = alertService;
        this.authService = authService;
        this.alertRepository = alertRepository;
    }

    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getOpenAlerts(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam String deviceId) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Open alerts", alertService.getOpenAlerts(deviceId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getHistory(@RequestHeader(value = "Authorization", required = false) String authorization, @RequestParam String deviceId) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Alert history", alertService.getHistory(deviceId)));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlertResponse>> resolve(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable Long id) {
        UserAccount user = authService.getCurrentUser(authorization);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        authService.validateAccessToDevice(authorization, alert.getDeviceId());
        return ResponseEntity.ok(ApiResponse.ok("Alert resolved", alertService.resolveAlert(id, user.getUsername())));
    }
}
