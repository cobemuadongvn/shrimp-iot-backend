package com.example.shrimpiot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.ClaimCodeIssueRequest;
import com.example.shrimpiot.dto.ClaimCodeIssueResponse;
import com.example.shrimpiot.dto.DeviceClaimRequest;
import com.example.shrimpiot.dto.DeviceClaimResponse;
import com.example.shrimpiot.dto.DeviceProvisioningStatusResponse;
import com.example.shrimpiot.exception.DeviceProvisioningException;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.service.AuditLogService;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.DeviceProvisioningService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/device-provisioning")
public class DeviceProvisioningController {
    private final DeviceProvisioningService provisioningService;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public DeviceProvisioningController(
            DeviceProvisioningService provisioningService,
            AuthService authService,
            AuditLogService auditLogService
    ) {
        this.provisioningService = provisioningService;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/claims")
    public ResponseEntity<ApiResponse<DeviceClaimResponse>> claim(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) DeviceClaimRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount actor = requireCurrentUser(authorization);
        DeviceClaimResponse response = provisioningService.claim(request, actor);
        auditLogService.record(
                actor,
                "DEVICE_CLAIM",
                "DEVICE",
                response.deviceId(),
                response.deviceId(),
                response.pondId(),
                "Provisioning claim status=" + response.claimStatus(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Device claimed successfully", response));
    }

    @GetMapping("/devices/{deviceId}/status")
    public ResponseEntity<ApiResponse<DeviceProvisioningStatusResponse>> status(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId
    ) {
        UserAccount actor = requireCurrentUser(authorization);
        return ResponseEntity.ok(ApiResponse.ok(
                "Provisioning status retrieved",
                provisioningService.getStatus(deviceId, actor)
        ));
    }

    @PostMapping("/devices/{deviceId}/claim-code")
    public ResponseEntity<ApiResponse<ClaimCodeIssueResponse>> issueClaimCode(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String deviceId,
            @RequestBody(required = false) ClaimCodeIssueRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount actor = requireCurrentUser(authorization);
        if (actor.getRole() != RoleName.ADMIN) {
            throw new DeviceProvisioningException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Only ADMIN can issue claim codes");
        }
        Integer expirationMinutes = request == null ? null : request.getExpiresInMinutes();
        ClaimCodeIssueResponse response = provisioningService.issueClaimCode(deviceId, expirationMinutes);
        auditLogService.record(
                actor,
                "DEVICE_CLAIM_CODE_ISSUE",
                "DEVICE",
                deviceId,
                deviceId,
                null,
                "A one-time claim code was issued; expiresAt=" + response.expiresAt(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Claim code issued; this value is shown only once", response));
    }

    private UserAccount requireCurrentUser(String authorization) {
        try {
            return authService.getCurrentUser(authorization);
        } catch (SecurityException ex) {
            throw new DeviceProvisioningException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
        }
    }
}
