package com.example.shrimpiot.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shrimpiot.dto.ClaimCodeIssueResponse;
import com.example.shrimpiot.dto.DeviceClaimRequest;
import com.example.shrimpiot.dto.DeviceClaimResponse;
import com.example.shrimpiot.dto.DeviceProvisioningStatusResponse;
import com.example.shrimpiot.exception.DeviceProvisioningException;
import com.example.shrimpiot.model.Device;
import com.example.shrimpiot.model.DeviceClaimStatus;
import com.example.shrimpiot.model.DeviceProvisioning;
import com.example.shrimpiot.model.Pond;
import com.example.shrimpiot.model.RoleName;
import com.example.shrimpiot.model.UserAccount;
import com.example.shrimpiot.model.UserPondAccess;
import com.example.shrimpiot.repository.DeviceProvisioningRepository;
import com.example.shrimpiot.repository.DeviceRepository;
import com.example.shrimpiot.repository.PondRepository;
import com.example.shrimpiot.repository.UserPondAccessRepository;

@Service
public class DeviceProvisioningService {
    private static final int CONTRACT_VERSION = 1;
    private static final int MIN_EXPIRATION_MINUTES = 5;
    private static final int MAX_EXPIRATION_MINUTES = 7 * 24 * 60;
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{2,63}$");
    private static final Set<String> TECHNICIAN_CLAIM_ACCESS = Set.of("OWNER", "READ_WRITE", "CONTROL");
    private static final ZoneId CLOUD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DeviceRepository deviceRepository;
    private final PondRepository pondRepository;
    private final UserPondAccessRepository accessRepository;
    private final DeviceProvisioningRepository provisioningRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${provisioning.claim.default-expiration-minutes:1440}")
    private int defaultExpirationMinutes = 1440;

    public DeviceProvisioningService(
            DeviceRepository deviceRepository,
            PondRepository pondRepository,
            UserPondAccessRepository accessRepository,
            DeviceProvisioningRepository provisioningRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.pondRepository = pondRepository;
        this.accessRepository = accessRepository;
        this.provisioningRepository = provisioningRepository;
    }

    @Transactional
    public ClaimCodeIssueResponse issueClaimCode(String rawDeviceId, Integer requestedExpirationMinutes) {
        String deviceId = normalizeAndValidateDeviceId(rawDeviceId);
        Device device = requireDevice(deviceId);
        int expirationMinutes = requestedExpirationMinutes == null
                ? defaultExpirationMinutes
                : requestedExpirationMinutes;
        if (expirationMinutes < MIN_EXPIRATION_MINUTES || expirationMinutes > MAX_EXPIRATION_MINUTES) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "expiresInMinutes must be between 5 and 10080");
        }

        DeviceProvisioning provisioning = provisioningRepository.findByDeviceIdForUpdate(deviceId)
                .orElseGet(() -> {
                    DeviceProvisioning created = new DeviceProvisioning();
                    created.setDevice(device);
                    return created;
                });
        if (provisioning.getClaimStatus() == DeviceClaimStatus.CLAIMED) {
            throw error(HttpStatus.CONFLICT, "DEVICE_ALREADY_CLAIMED", "Device is already claimed");
        }

        String claimCode = generateClaimCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);
        provisioning.setClaimStatus(DeviceClaimStatus.UNCLAIMED);
        provisioning.setClaimCodeHash(hashClaimCode(claimCode));
        provisioning.setClaimCodeExpiresAt(expiresAt);
        provisioning.setIssuedAt(now);
        provisioning.setClaimedBy(null);
        provisioning.setClaimedPond(null);
        provisioning.setClaimedAt(null);
        provisioning.setRevokedAt(null);
        provisioningRepository.save(provisioning);

        return new ClaimCodeIssueResponse(deviceId, claimCode, toOffset(expiresAt));
    }

    @Transactional
    public DeviceClaimResponse claim(DeviceClaimRequest request, UserAccount actor) {
        validateClaimRequest(request);
        String deviceId = normalizeAndValidateDeviceId(request.getDeviceId());
        Device device = requireDevice(deviceId);
        Pond pond = pondRepository.findById(request.getPondId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "POND_NOT_FOUND", "Pond not found"));
        requireClaimPermission(actor, pond);

        DeviceProvisioning provisioning = provisioningRepository.findByDeviceIdForUpdate(deviceId)
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_CLAIM_CODE", "Claim code is invalid"));

        if (provisioning.getClaimStatus() == DeviceClaimStatus.CLAIMED) {
            if (sameUser(provisioning.getClaimedBy(), actor)
                    && provisioning.getClaimedPond() != null
                    && provisioning.getClaimedPond().getId().equals(pond.getId())) {
                return toClaimResponse(device, provisioning);
            }
            throw error(HttpStatus.CONFLICT, "DEVICE_ALREADY_CLAIMED", "Device is already claimed by another owner or pond");
        }

        LocalDateTime now = LocalDateTime.now();
        if (provisioning.getClaimStatus() == DeviceClaimStatus.REVOKED
                || provisioning.getClaimCodeExpiresAt() == null
                || !provisioning.getClaimCodeExpiresAt().isAfter(now)) {
            throw error(HttpStatus.GONE, "CLAIM_CODE_EXPIRED", "Claim code has expired or was revoked");
        }
        if (provisioning.getClaimCodeHash() == null
                || !claimCodeMatches(request.getClaimCode(), provisioning.getClaimCodeHash())) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_CLAIM_CODE", "Claim code is invalid");
        }

        device.setPond(pond);
        deviceRepository.save(device);

        provisioning.setClaimStatus(DeviceClaimStatus.CLAIMED);
        provisioning.setClaimedBy(actor);
        provisioning.setClaimedPond(pond);
        provisioning.setClaimedAt(now);
        provisioning.setClaimCodeHash(null);
        provisioning.setClaimCodeExpiresAt(null);
        provisioningRepository.save(provisioning);

        return toClaimResponse(device, provisioning);
    }

    @Transactional(readOnly = true)
    public DeviceProvisioningStatusResponse getStatus(String rawDeviceId, UserAccount actor) {
        String deviceId = normalizeAndValidateDeviceId(rawDeviceId);
        Device device = requireDevice(deviceId);
        requireStatusPermission(actor, device);
        DeviceClaimStatus claimStatus = provisioningRepository.findByDeviceDeviceId(deviceId)
                .map(DeviceProvisioning::getClaimStatus)
                .orElse(DeviceClaimStatus.UNCLAIMED);
        return new DeviceProvisioningStatusResponse(
                deviceId,
                claimStatus.name(),
                normalizeConnectionStatus(device.getConnectionStatus()),
                toOffset(device.getLastSeenAt())
        );
    }

    private void validateClaimRequest(DeviceClaimRequest request) {
        if (request == null) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is required");
        }
        if (request.getVersion() == null || request.getVersion() != CONTRACT_VERSION) {
            throw error(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVISIONING_VERSION", "Only provisioning version 1 is supported");
        }
        if (request.getPondId() == null || request.getPondId() <= 0) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "pondId must be a positive number");
        }
        String claimCode = request.getClaimCode();
        if (claimCode == null || claimCode.length() < 20 || claimCode.length() > 128) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_CLAIM_CODE", "Claim code is invalid");
        }
    }

    private String normalizeAndValidateDeviceId(String rawDeviceId) {
        String deviceId = rawDeviceId == null ? "" : rawDeviceId.trim();
        if (!DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "deviceId format is invalid");
        }
        return deviceId;
    }

    private Device requireDevice(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Device not found"));
        if (device.getDeviceId() == null || device.getDeviceId().toLowerCase(Locale.ROOT).startsWith("pond_")) {
            throw error(HttpStatus.NOT_FOUND, "DEVICE_NOT_FOUND", "Device not found");
        }
        return device;
    }

    private void requireClaimPermission(UserAccount actor, Pond pond) {
        if (actor.getRole() == RoleName.ADMIN) return;
        UserPondAccess access = accessRepository.findByUserAndPond(actor, pond)
                .orElseThrow(() -> error(HttpStatus.FORBIDDEN, "POND_ACCESS_DENIED", "Access denied to target pond"));
        String accessType = normalizeAccessType(access.getAccessType());
        if (actor.getRole() == RoleName.USER && !"OWNER".equals(accessType)) {
            throw error(HttpStatus.FORBIDDEN, "POND_ACCESS_DENIED", "USER needs OWNER access to claim a device");
        }
        if (actor.getRole() == RoleName.TECHNICIAN && !TECHNICIAN_CLAIM_ACCESS.contains(accessType)) {
            throw error(HttpStatus.FORBIDDEN, "POND_ACCESS_DENIED", "TECHNICIAN needs OWNER, READ_WRITE or CONTROL access");
        }
    }

    private void requireStatusPermission(UserAccount actor, Device device) {
        if (actor.getRole() == RoleName.ADMIN) return;
        if (device.getPond() == null || !accessRepository.existsByUserAndPond(actor, device.getPond())) {
            throw error(HttpStatus.FORBIDDEN, "POND_ACCESS_DENIED", "Access denied to device pond");
        }
    }

    private String normalizeAccessType(String accessType) {
        return accessType == null ? "" : accessType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean sameUser(UserAccount left, UserAccount right) {
        if (left == null || right == null) return false;
        if (left.getId() != null && right.getId() != null) return left.getId().equals(right.getId());
        return left.getUsername() != null && left.getUsername().equals(right.getUsername());
    }

    private DeviceClaimResponse toClaimResponse(Device device, DeviceProvisioning provisioning) {
        Pond pond = provisioning.getClaimedPond() != null ? provisioning.getClaimedPond() : device.getPond();
        return new DeviceClaimResponse(
                device.getDeviceId(),
                pond == null ? null : pond.getId(),
                provisioning.getClaimStatus().name(),
                normalizeConnectionStatus(device.getConnectionStatus())
        );
    }

    private String normalizeConnectionStatus(String status) {
        if (status == null) return "UNKNOWN";
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return Set.of("ONLINE", "OFFLINE").contains(normalized) ? normalized : "UNKNOWN";
    }

    private String generateClaimCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashClaimCode(String claimCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(claimCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private boolean claimCodeMatches(String claimCode, String expectedHash) {
        byte[] actual = hashClaimCode(claimCode).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(CLOUD_ZONE).toOffsetDateTime();
    }

    private DeviceProvisioningException error(HttpStatus status, String code, String message) {
        return new DeviceProvisioningException(status, code, message);
    }
}
