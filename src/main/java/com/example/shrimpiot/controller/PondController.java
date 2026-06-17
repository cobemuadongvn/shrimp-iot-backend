package com.example.shrimpiot.controller;

import com.example.shrimpiot.dto.ApiResponse;
import com.example.shrimpiot.model.*;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.UserAccountRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;
import com.example.shrimpiot.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ponds")
public class PondController {

    private final PondRepository pondRepository;
    private final UserAccountRepository userRepository;
    private final UserPondAccessRepository userPondAccessRepository;
    private final AuthService authService;

    public PondController(
            PondRepository pondRepository,
            UserAccountRepository userRepository,
            UserPondAccessRepository userPondAccessRepository,
            AuthService authService
    ) {
        this.pondRepository = pondRepository;
        this.userRepository = userRepository;
        this.userPondAccessRepository = userPondAccessRepository;
        this.authService = authService;
    }

    // 1. Tạo ao nuôi mới (ADMIN only)
    @PostMapping
    public ResponseEntity<ApiResponse<Pond>> createPond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Pond pond
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond saved = pondRepository.save(pond);
        return ResponseEntity.ok(ApiResponse.ok("Pond created successfully", saved));
    }

    // 2. Lấy danh sách ao nuôi (ADMIN xem tất cả; USER/TECHNICIAN xem ao được phân quyền)
    @GetMapping
    public ResponseEntity<ApiResponse<List<Pond>>> getPonds(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        UserAccount user = authService.getCurrentUser(authorization);
        List<Pond> ponds;
        if (user.getRole() == RoleName.ADMIN) {
            ponds = pondRepository.findAll();
        } else {
            ponds = userPondAccessRepository.findByUser(user)
                    .stream()
                    .map(UserPondAccess::getPond)
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.ok("Ponds retrieved successfully", ponds));
    }

    // 3. Xem chi tiết ao nuôi (Phân quyền kiểm tra validateAccessToPond)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Pond>> getPondById(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        authService.validateAccessToPond(authorization, id);
        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + id));
        return ResponseEntity.ok(ApiResponse.ok("Pond details", pond));
    }

    // 4. Cập nhật thông tin ao nuôi (ADMIN only)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Pond>> updatePond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Pond pondDetails
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + id));

        pond.setName(pondDetails.getName());
        pond.setLocation(pondDetails.getLocation());
        pond.setAreaSquareMeters(pondDetails.getAreaSquareMeters());
        pond.setSpeciesType(pondDetails.getSpeciesType());
        pond.setPondType(pondDetails.getPondType());
        pond.setWaterVolumeCubicMeters(pondDetails.getWaterVolumeCubicMeters());
        pond.setRegion(pondDetails.getRegion());
        pond.setStatus(pondDetails.getStatus());
        pond.setLatitude(pondDetails.getLatitude());
        pond.setLongitude(pondDetails.getLongitude());
        pond.setDescription(pondDetails.getDescription());

        Pond updated = pondRepository.save(pond);
        return ResponseEntity.ok(ApiResponse.ok("Pond updated successfully", updated));
    }

    // 5. Xóa ao nuôi (ADMIN only)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + id));

        pond.setStatus("INACTIVE");
        pondRepository.save(pond);
        return ResponseEntity.ok(ApiResponse.ok("Pond deactivated successfully", null));
    }


    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Pond>> activatePond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + id));
        pond.setStatus("ACTIVE");
        return ResponseEntity.ok(ApiResponse.ok("Pond activated", pondRepository.save(pond)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Pond>> deactivatePond(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond pond = pondRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + id));
        pond.setStatus("INACTIVE");
        return ResponseEntity.ok(ApiResponse.ok("Pond deactivated", pondRepository.save(pond)));
    }

    // ================= Phân quyền truy cập ao nuôi =================

    // 6. Cấp quyền truy cập ao nuôi cho người dùng (ADMIN only)
    @PostMapping("/{pondId}/access")
    public ResponseEntity<ApiResponse<UserPondAccess>> grantAccess(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId,
            @RequestParam String username,
            @RequestParam(defaultValue = "READ_WRITE") String accessType
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));

        UserAccount targetUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Kiểm tra xem đã có quyền chưa
        UserPondAccess access = userPondAccessRepository.findByUserAndPond(targetUser, pond)
                .orElse(new UserPondAccess());

        access.setUser(targetUser);
        access.setPond(pond);
        access.setAccessType(accessType.toUpperCase());

        UserPondAccess saved = userPondAccessRepository.save(access);
        return ResponseEntity.ok(ApiResponse.ok("Access granted successfully to " + username, saved));
    }

    // 7. Thu hồi quyền truy cập ao (ADMIN only)
    @DeleteMapping("/{pondId}/access/{userId}")
    public ResponseEntity<ApiResponse<Void>> revokeAccess(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId,
            @PathVariable Long userId
    ) {
        authService.requireAnyRole(authorization, RoleName.ADMIN);
        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));

        UserAccount targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserPondAccess access = userPondAccessRepository.findByUserAndPond(targetUser, pond)
                .orElseThrow(() -> new IllegalArgumentException("Access mapping not found for user: " + targetUser.getUsername()));

        userPondAccessRepository.delete(access);
        return ResponseEntity.ok(ApiResponse.ok("Access revoked successfully for " + targetUser.getUsername(), null));
    }

    // 8. Xem danh sách quyền truy cập của một ao nuôi (ADMIN hoặc người dùng có quyền)
    @GetMapping("/{pondId}/access")
    public ResponseEntity<ApiResponse<List<UserPondAccess>>> getPondAccessList(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long pondId
    ) {
        authService.validateAccessToPond(authorization, pondId);
        Pond pond = pondRepository.findById(pondId)
                .orElseThrow(() -> new IllegalArgumentException("Pond not found: " + pondId));

        List<UserPondAccess> accessList = userPondAccessRepository.findByPond(pond);
        return ResponseEntity.ok(ApiResponse.ok("Pond access list", accessList));
    }
}
