package com.example.shrimpiot.service;

import com.example.shrimpiot.dto.ApproveUserRequest;
import com.example.shrimpiot.dto.CreateUserRequest;
import com.example.shrimpiot.dto.UserResponse;
import com.example.shrimpiot.dto.UpdateProfileRequest;
import com.example.shrimpiot.dto.UserProfileResponse;
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.repository.AuthTokenRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.UserAccountRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PondRepository pondRepository;
    private final DeviceRepository deviceRepository;
    private final UserPondAccessRepository userPondAccessRepository;
    private final AuthTokenRepository authTokenRepository;

    public UserService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            PondRepository pondRepository,
            DeviceRepository deviceRepository,
            UserPondAccessRepository userPondAccessRepository,
            AuthTokenRepository authTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pondRepository = pondRepository;
        this.deviceRepository = deviceRepository;
        this.userPondAccessRepository = userPondAccessRepository;
        this.authTokenRepository = authTokenRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::new)
                .toList();
    }

    public List<UserResponse> getPendingUsers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getApprovalStatus() == ApprovalStatus.PENDING || (!user.isActive() && user.getApprovalStatus() == null))
                .map(UserResponse::new)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        String email = normalizeEmail(request.getEmail());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        RoleName role = parseRole(request.getRole());

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setActive(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedBy("ADMIN_CREATED");
        user.setApprovedAt(LocalDateTime.now());

        return new UserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse approveUser(Long userId, ApproveUserRequest request, String approvedBy) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        RoleName role = request.getRole() == null || request.getRole().isBlank()
                ? RoleName.USER
                : parseRole(request.getRole());

        user.setRole(role);
        user.setActive(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedBy(approvedBy);
        user.setApprovedAt(LocalDateTime.now());

        UserAccount saved = userRepository.save(user);

        String accessType = request.getAccessType() == null || request.getAccessType().isBlank()
                ? (role == RoleName.TECHNICIAN ? "READ_WRITE" : "OWNER")
                : request.getAccessType();

        if (request.getPondIds() != null) {
            for (Long pondId : request.getPondIds()) {
                assignUserToPond(saved.getUsername(), pondId, accessType);
            }
        }

        if (request.getDeviceIds() != null) {
            for (String deviceId : request.getDeviceIds()) {
                deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
                    if (device.getPond() != null) {
                        assignUserToPond(saved.getUsername(), device.getPond().getId(), accessType);
                    }
                });
            }
        }

        return new UserResponse(saved);
    }

    @Transactional
    public UserResponse rejectUser(Long userId, String rejectedBy) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setActive(false);
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        user.setApprovedBy(rejectedBy);
        user.setApprovedAt(LocalDateTime.now());

        revokeActiveTokens(user);

        return new UserResponse(userRepository.save(user));
    }

    public UserResponse updateRole(Long userId, String roleValue) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setRole(parseRole(roleValue));
        return new UserResponse(userRepository.save(user));
    }

    @Transactional
    public UserProfileResponse updateOwnProfile(UserAccount currentUser, UpdateProfileRequest request) {
        UserAccount user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUser.getId()));

        String fullName = normalizeRequiredText(request.getFullName(), "fullName");
        String email = normalizeEmail(request.getEmail());
        String phone = normalizeOptionalText(request.getPhone());

        if (email != null) {
            userRepository.findByEmail(email).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Email already exists");
                }
            });
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);

        return new UserProfileResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(Long userId, UserAccount currentAdmin, String reason) {
        UserAccount targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (targetUser.getId().equals(currentAdmin.getId())) {
            throw new IllegalArgumentException("Admin cannot deactivate own account");
        }

        if (targetUser.getRole() == RoleName.ADMIN && targetUser.isActive()) {
            long activeAdminCount = userRepository.countByRoleAndActiveTrue(RoleName.ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalArgumentException("Cannot deactivate the last active admin");
            }
        }

        targetUser.setActive(false);

        // Giữ approvalStatus = APPROVED nếu tài khoản đã được duyệt trước đó.
        // active=false thể hiện trạng thái bị khóa/vô hiệu hóa, không phải bị từ chối đăng ký.
        if (targetUser.getApprovalStatus() == null) {
            targetUser.setApprovalStatus(ApprovalStatus.APPROVED);
        }

        revokeActiveTokens(targetUser);

        UserAccount saved = userRepository.save(targetUser);
        return new UserResponse(saved);
    }

    @Transactional
    public UserResponse activateUser(Long userId, UserAccount currentAdmin, String reason) {
        UserAccount targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (targetUser.getApprovalStatus() == ApprovalStatus.REJECTED) {
            // Mở khóa lại tài khoản từng bị từ chối nghĩa là admin đã chấp nhận cho dùng hệ thống.
            targetUser.setApprovalStatus(ApprovalStatus.APPROVED);
            targetUser.setApprovedBy(currentAdmin.getUsername());
            targetUser.setApprovedAt(LocalDateTime.now());
        }

        if (targetUser.getApprovalStatus() == ApprovalStatus.PENDING || targetUser.getApprovalStatus() == null) {
            targetUser.setApprovalStatus(ApprovalStatus.APPROVED);
            targetUser.setApprovedBy(currentAdmin.getUsername());
            targetUser.setApprovedAt(LocalDateTime.now());
        }

        targetUser.setActive(true);

        UserAccount saved = userRepository.save(targetUser);
        return new UserResponse(saved);
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void revokeActiveTokens(UserAccount user) {
        authTokenRepository.findByUserAndRevokedFalse(user)
                .forEach(token -> token.setRevoked(true));
    }

    private RoleName parseRole(String value) {
        try {
            return RoleName.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("role must be ADMIN, USER, or TECHNICIAN");
        }
    }

    @Transactional
    public void lockUnlockUser(String username, boolean active) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setActive(active);
        if (!active) {
            revokeActiveTokens(user);
        }
        if (!active && user.getApprovalStatus() == null) {
            user.setApprovalStatus(ApprovalStatus.REJECTED);
        }
        if (active && user.getApprovalStatus() == ApprovalStatus.PENDING) {
            user.setApprovalStatus(ApprovalStatus.APPROVED);
            user.setApprovedBy("ADMIN_ENABLED");
            user.setApprovedAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    public String resetPassword(String username) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        String temp = java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
        user.setPasswordHash(passwordEncoder.encode(temp));
        userRepository.save(user);
        return temp;
    }

    public void assignUserToPond(String username, Long pondId, String accessType) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));
        userPondAccessRepository.findByUserAndPond(user, pond).ifPresentOrElse(existing -> {
            if (accessType != null && !accessType.isBlank()) existing.setAccessType(accessType);
            userPondAccessRepository.save(existing);
        }, () -> {
            UserPondAccess access = new UserPondAccess();
            access.setUser(user);
            access.setPond(pond);
            if (accessType != null && !accessType.isBlank()) access.setAccessType(accessType);
            userPondAccessRepository.save(access);
        });
    }

    public void assignTechnicianToPond(String username, Long pondId) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setRole(RoleName.TECHNICIAN);
        userRepository.save(user);
        assignUserToPond(username, pondId, "READ_WRITE");
    }
}
