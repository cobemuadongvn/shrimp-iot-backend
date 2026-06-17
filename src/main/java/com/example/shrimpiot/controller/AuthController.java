package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.dto.AuthResponse;
import com.example.shrimpiot.dto.LoginRequest;
import com.example.shrimpiot.dto.RegisterRequest;
import com.example.shrimpiot.dto.UserResponse;
import com.example.shrimpiot.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đăng ký thành công. Tài khoản đang chờ quản trị viên phê duyệt",
                authService.register(request)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Current user", new UserResponse(authService.getCurrentUser(authorization))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authService.logout(authorization);
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody com.example.shrimpiot.dto.ChangePasswordRequest request
    ) {
        authService.changePassword(authorization, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
    }
}
