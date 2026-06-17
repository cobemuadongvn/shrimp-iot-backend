package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.model.AuditLog;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuditLogService;
import com.example.shrimpiot.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final AuthService authService;

    public AuditLogController(AuditLogService auditLogService, AuthService authService) {
        this.auditLogService = auditLogService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLog>>> getLogs(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        List<AuditLog> logs;
        if (actor != null && !actor.isBlank()) {
            logs = auditLogService.getByActor(actor, limit);
        } else if (deviceId != null && !deviceId.isBlank()) {
            logs = auditLogService.getByDevice(deviceId, limit);
        } else {
            logs = auditLogService.getRecent(limit);
        }
        return ResponseEntity.ok(ApiResponse.ok("Audit logs", logs));
    }
}
