package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.CommandAckRequest;
import com.example.shrimpiot.dto.CommandRequest;
import com.example.shrimpiot.dto.CommandResponse;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.CommandService;
import com.example.shrimpiot.service.AuditLogService;
import com.example.shrimpiot.model.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commands")
public class CommandController {

    private final CommandService service;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public CommandController(CommandService service, AuthService authService, AuditLogService auditLogService) {
        this.service = service;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    // App/Web gọi API này để tạo lệnh bật/tắt relay.
    // ADMIN, USER và TECHNICIAN đều được tạo lệnh trong phạm vi được cấp.
    // Bản demo hiện kiểm tra theo role, chưa kiểm tra theo từng ao.
    @PostMapping
    public ResponseEntity<ApiResponse<CommandResponse>> createCommand(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CommandRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount actor = authService.getCurrentUser(authorization);
        authService.validateWriteAccessToDevice(authorization, request.getDeviceId());
        CommandResponse response = service.createCommand(request, actor.getUsername());
        auditLogService.record(actor, "COMMAND_CREATE", "RELAY", String.valueOf(request.getRelayNo()), request.getDeviceId(), null, "Action=" + request.getAction(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Command created", response));
    }

    // Arduino gọi API này để lấy lệnh đang chờ. Chỉ dùng X-API-Key của thiết bị.
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<CommandResponse>>> getPendingCommands(
            @RequestParam String deviceId,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Pending commands", service.getPendingCommands(deviceId, apiKey)));
    }

    // Arduino xác nhận đã thực hiện lệnh. Chỉ dùng X-API-Key của thiết bị.
    @PostMapping("/{id}/ack")
    public ResponseEntity<ApiResponse<CommandResponse>> acknowledge(
            @PathVariable Long id,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody CommandAckRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Command acknowledged", service.acknowledge(id, request, apiKey)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CommandResponse>>> getCommandHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String deviceId
    ) {
        authService.validateAccessToDevice(authorization, deviceId);
        return ResponseEntity.ok(ApiResponse.ok("Command history", service.getCommandHistory(deviceId)));
    }
}
