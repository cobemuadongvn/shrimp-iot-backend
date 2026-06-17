package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.*;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.service.AuthService;
import com.example.shrimpiot.service.UserService;
import com.example.shrimpiot.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuthService authService, AuditLogService auditLogService) {
        this.userService = userService;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("Users", userService.getAllUsers()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateUserRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("User created", userService.createUser(request)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getPendingUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("Pending users", userService.getPendingUsers()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<UserResponse>> approveUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody ApproveUserRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount admin = authService.requireAnyRole(authorization, RoleName.ADMIN);
        UserResponse response = userService.approveUser(id, request, admin.getUsername());
        auditLogService.record(admin, "USER_APPROVE", "USER", String.valueOf(id), null, null, "Role=" + request.getRole(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("User approved", response));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<UserResponse>> rejectUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        UserAccount admin = authService.requireAnyRole(authorization, RoleName.ADMIN);
        UserResponse response = userService.rejectUser(id, admin.getUsername());
        auditLogService.record(admin, "USER_REJECT", "USER", String.valueOf(id), null, null, "User rejected", httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("User rejected", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount currentUser = authService.getCurrentUser(authorization);
        UserProfileResponse response = userService.updateOwnProfile(currentUser, request);
        auditLogService.record(currentUser, "USER_PROFILE_UPDATE", "USER", String.valueOf(currentUser.getId()), null, null, "Profile updated", httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hồ sơ thành công", response));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("User role updated", userService.updateRole(id, request.getRole())));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody(required = false) UserStatusChangeRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount currentAdmin = authService.requireAnyRole(authorization, RoleName.ADMIN);
        String reason = request != null ? request.getReason() : null;
        UserResponse response = userService.deactivateUser(id, currentAdmin, reason);
        auditLogService.record(currentAdmin, "USER_DEACTIVATE", "USER", String.valueOf(id), null, null, reason, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", response));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody(required = false) UserStatusChangeRequest request,
            HttpServletRequest httpRequest
    ) {
        UserAccount currentAdmin = authService.requireAnyRole(authorization, RoleName.ADMIN);
        String reason = request != null ? request.getReason() : null;
        UserResponse response = userService.activateUser(id, currentAdmin, reason);
        auditLogService.record(currentAdmin, "USER_ACTIVATE", "USER", String.valueOf(id), null, null, reason, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("User activated", response));
    }

    /**
     * Backward-compatible endpoint for older web clients.
     * New clients should use PATCH /api/users/{id}/deactivate and PATCH /api/users/{id}/activate.
     */
    @PostMapping("/lock")
    public ResponseEntity<ApiResponse<Object>> lockUnlockUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username,
            @RequestParam boolean active
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        userService.lockUnlockUser(username, active);
        return ResponseEntity.ok(ApiResponse.ok("User updated", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        String temp = userService.resetPassword(username);
        return ResponseEntity.ok(ApiResponse.ok("Password reset", java.util.Map.of("username", username, "tempPassword", temp)));
    }

    @PostMapping("/assign-pond")
    public ResponseEntity<ApiResponse<Object>> assignPond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AssignPondRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        userService.assignUserToPond(request.getUsername(), request.getPondId(), request.getAccessType());
        return ResponseEntity.ok(ApiResponse.ok("Assigned user to pond", null));
    }

    @PostMapping("/assign-technician")
    public ResponseEntity<ApiResponse<Object>> assignTechnician(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AssignPondRequest request
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        userService.assignTechnicianToPond(request.getUsername(), request.getPondId());
        return ResponseEntity.ok(ApiResponse.ok("Assigned technician to pond", null));
    }
}
