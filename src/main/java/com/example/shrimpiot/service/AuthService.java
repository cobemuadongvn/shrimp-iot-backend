package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.AuthResponse;
import com.example.shrimpiot.dto.LoginRequest;
import com.example.shrimpiot.dto.RegisterRequest;
import com.example.shrimpiot.dto.UserResponse;
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceRepository deviceRepository;
    private final PondRepository pondRepository;
    private final UserPondAccessRepository userPondAccessRepository;

    @Value("${auth.token-expiration-hours}")
    private long tokenExpirationHours;

    public AuthService(
            UserAccountRepository userRepository,
            AuthTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            DeviceRepository deviceRepository,
            PondRepository pondRepository,
            UserPondAccessRepository userPondAccessRepository
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.deviceRepository = deviceRepository;
        this.pondRepository = pondRepository;
        this.userPondAccessRepository = userPondAccessRepository;
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new SecurityException("Invalid username or password"));

        ensureUserCanLogin(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new SecurityException("Invalid username or password");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);

        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setToken(UUID.randomUUID() + "." + UUID.randomUUID());
        authToken.setExpiresAt(expiresAt);

        AuthToken saved = tokenRepository.save(authToken);

        return new AuthResponse(saved.getToken(), expiresAt, new UserResponse(user));
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        String email = normalizeEmail(request.getEmail());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleName.USER);
        user.setActive(false);
        user.setApprovalStatus(ApprovalStatus.PENDING);

        return new UserResponse(userRepository.save(user));
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        AuthToken authToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new SecurityException("Invalid token"));
        authToken.setRevoked(true);
        tokenRepository.save(authToken);
    }

    public void changePassword(String authorizationHeader, String oldPassword, String newPassword) {
        UserAccount user = getCurrentUser(authorizationHeader);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new SecurityException("Old password incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public UserAccount getCurrentUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);

        AuthToken authToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new SecurityException("Invalid token"));

        if (authToken.isRevoked()) {
            throw new SecurityException("Token is revoked");
        }

        if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SecurityException("Token is expired");
        }

        UserAccount user = authToken.getUser();

        ensureUserCanUseSystem(user);

        return user;
    }

    public UserAccount requireAnyRole(String authorizationHeader, RoleName... allowedRoles) {
        UserAccount user = getCurrentUser(authorizationHeader);

        boolean allowed = Arrays.asList(allowedRoles).contains(user.getRole());

        if (!allowed) {
            throw new SecurityException("Access denied");
        }

        return user;
    }

    private void ensureUserCanLogin(UserAccount user) {
        ApprovalStatus status = user.getApprovalStatus();

        if (status == ApprovalStatus.PENDING) {
            throw new SecurityException("Tài khoản đang chờ quản trị viên phê duyệt");
        }

        if (status == ApprovalStatus.REJECTED) {
            throw new SecurityException("Tài khoản đã bị từ chối phê duyệt");
        }

        if (!user.isActive()) {
            throw new SecurityException("User is disabled");
        }
    }

    private void ensureUserCanUseSystem(UserAccount user) {
        ApprovalStatus status = user.getApprovalStatus();

        if (status == ApprovalStatus.PENDING) {
            throw new SecurityException("Tài khoản đang chờ quản trị viên phê duyệt");
        }

        if (status == ApprovalStatus.REJECTED) {
            throw new SecurityException("Tài khoản đã bị từ chối phê duyệt");
        }

        if (!user.isActive()) {
            throw new SecurityException("User is disabled");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new SecurityException("Missing Authorization header");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new SecurityException("Authorization header must start with Bearer");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();

        if (token.isBlank()) {
            throw new SecurityException("Missing token");
        }

        return token;
    }

    public void validateAccessToDevice(String authorizationHeader, String deviceId) {
        UserAccount user = getCurrentUser(authorizationHeader);
        Device device = requireActiveRealDevice(deviceId);

        if (user.getRole() == RoleName.ADMIN) {
            return;
        }

        if (device.getPond() == null) {
            throw new SecurityException("Device is not assigned to any pond");
        }

        boolean hasAccess = userPondAccessRepository.existsByUserAndPond(user, device.getPond());
        if (!hasAccess) {
            throw new SecurityException("Access denied to pond of device: " + deviceId);
        }
    }


    public void validateWriteAccessToDevice(String authorizationHeader, String deviceId) {
        UserAccount user = getCurrentUser(authorizationHeader);
        Device device = requireActiveRealDevice(deviceId);

        if (user.getRole() == RoleName.ADMIN) {
            return;
        }

        if (device.getPond() == null) {
            throw new SecurityException("Device is not assigned to any pond");
        }

        UserPondAccess access = userPondAccessRepository.findByUserAndPond(user, device.getPond())
                .orElseThrow(() -> new SecurityException("Access denied to pond of device: " + deviceId));

        String accessType = access.getAccessType() == null ? "" : access.getAccessType().trim().toUpperCase();
        if (!(accessType.equals("OWNER") || accessType.equals("READ_WRITE") || accessType.equals("CONTROL"))) {
            throw new SecurityException("Write/control access denied to device: " + deviceId);
        }
    }

    public Device requireActiveRealDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceIdAndStatus(deviceId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Active device not found: " + deviceId));

        if (!isRealDeviceId(device.getDeviceId())) {
            throw new IllegalArgumentException("Invalid device id for physical device: " + deviceId);
        }

        return device;
    }

    public boolean isRealDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        String normalized = deviceId.trim().toLowerCase(Locale.ROOT);
        return !normalized.startsWith("pond_");
    }

    public void validateAccessToPond(String authorizationHeader, Long pondId) {
        UserAccount user = getCurrentUser(authorizationHeader);
        if (user.getRole() == RoleName.ADMIN) {
            return;
        }

        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));

        boolean hasAccess = userPondAccessRepository.existsByUserAndPond(user, pond);
        if (!hasAccess) {
            throw new SecurityException("Access denied to pond: " + pond.getName());
        }
    }
}
