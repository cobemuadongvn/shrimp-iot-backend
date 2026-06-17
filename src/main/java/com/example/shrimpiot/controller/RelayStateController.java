package com.example.shrimpiot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.RelayStateResponse;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.RelayStateService;

@RestController
@RequestMapping("/api/relay-states")
public class RelayStateController {
    private final RelayStateService service;
    private final AuthService authService;

    public RelayStateController(RelayStateService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    /**
     * Lấy trạng thái của một relay cụ thể
     * GET /api/relay-states/{deviceId}/{relayNo}
     */
    @GetMapping("/{deviceId}/{relayNo}")
    public ResponseEntity<ApiResponse<RelayStateResponse>> getRelayState(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @PathVariable Integer relayNo
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        RelayStateResponse response = service.getRelayState(deviceId, relayNo);
        return ResponseEntity.ok(ApiResponse.ok("Relay state retrieved", response));
    }

    /**
     * Lấy trạng thái tất cả relay của một thiết bị
     * GET /api/relay-states/{deviceId}
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<List<RelayStateResponse>>> getRelayStatesByDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        List<RelayStateResponse> response = service.getRelayStatesByDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Relay states retrieved", response));
    }
}
