package com.example.shrimpiot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.shrimpiot.dto.ClaimCodeIssueResponse;
import com.example.shrimpiot.dto.DeviceClaimRequest;
import com.example.shrimpiot.dto.DeviceClaimResponse;
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

@ExtendWith(MockitoExtension.class)
class DeviceProvisioningServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private PondRepository pondRepository;
    @Mock
    private UserPondAccessRepository accessRepository;
    @Mock
    private DeviceProvisioningRepository provisioningRepository;

    private DeviceProvisioningService service;
    private Device device;
    private Pond pond;
    private UserAccount owner;

    @BeforeEach
    void setUp() {
        service = new DeviceProvisioningService(
                deviceRepository,
                pondRepository,
                accessRepository,
                provisioningRepository
        );
        pond = new Pond();
        pond.setId(1L);
        pond.setName("Test pond");

        device = new Device();
        device.setDeviceId("device_01");
        device.setName("Test device");
        device.setConnectionStatus("ONLINE");

        owner = new UserAccount();
        owner.setUsername("owner");
        owner.setRole(RoleName.USER);
    }

    @Test
    void issueClaimAndIdempotentRetryDoNotStorePlainCode() {
        stubClaimContext();
        when(provisioningRepository.findByDeviceIdForUpdate("device_01"))
                .thenReturn(Optional.empty());

        ClaimCodeIssueResponse issued = service.issueClaimCode("device_01", 60);
        assertEquals(43, issued.claimCode().length());

        ArgumentCaptor<DeviceProvisioning> savedCaptor = ArgumentCaptor.forClass(DeviceProvisioning.class);
        verify(provisioningRepository).save(savedCaptor.capture());
        DeviceProvisioning stored = savedCaptor.getValue();
        assertEquals(64, stored.getClaimCodeHash().length());
        assertNotEquals(issued.claimCode(), stored.getClaimCodeHash());
        assertFalse(stored.getClaimCodeHash().contains(issued.claimCode()));

        when(provisioningRepository.findByDeviceIdForUpdate("device_01"))
                .thenReturn(Optional.of(stored));

        DeviceClaimResponse claimed = service.claim(claimRequest(issued.claimCode(), 1L), owner);
        assertEquals("CLAIMED", claimed.claimStatus());
        assertEquals(1L, claimed.pondId());
        assertEquals(pond, device.getPond());
        assertNull(stored.getClaimCodeHash());
        assertNull(stored.getClaimCodeExpiresAt());

        DeviceClaimResponse retried = service.claim(claimRequest("wrong-code-000000000000", 1L), owner);
        assertEquals("CLAIMED", retried.claimStatus());
    }

    @Test
    void rejectsInvalidClaimCodeWithStableError() {
        stubClaimContext();
        DeviceProvisioning stored = unclaimedProvisioning("correct-code-0000000000", LocalDateTime.now().plusMinutes(30));
        when(provisioningRepository.findByDeviceIdForUpdate("device_01"))
                .thenReturn(Optional.of(stored));

        DeviceProvisioningException exception = assertThrows(
                DeviceProvisioningException.class,
                () -> service.claim(claimRequest("wrong-code-000000000000", 1L), owner)
        );
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, exception.getStatus());
        assertEquals("INVALID_CLAIM_CODE", exception.getCode());
    }

    @Test
    void rejectsExpiredClaimCode() {
        stubClaimContext();
        DeviceProvisioning stored = unclaimedProvisioning("expired-code-000000000", LocalDateTime.now().minusSeconds(1));
        when(provisioningRepository.findByDeviceIdForUpdate("device_01"))
                .thenReturn(Optional.of(stored));

        DeviceProvisioningException exception = assertThrows(
                DeviceProvisioningException.class,
                () -> service.claim(claimRequest("expired-code-000000000", 1L), owner)
        );
        assertEquals(HttpStatus.GONE, exception.getStatus());
        assertEquals("CLAIM_CODE_EXPIRED", exception.getCode());
    }

    @Test
    void userNeedsOwnerAccessToClaim() {
        when(deviceRepository.findByDeviceId("device_01")).thenReturn(Optional.of(device));
        when(pondRepository.findById(1L)).thenReturn(Optional.of(pond));
        UserPondAccess readOnly = new UserPondAccess();
        readOnly.setUser(owner);
        readOnly.setPond(pond);
        readOnly.setAccessType("READ_ONLY");
        when(accessRepository.findByUserAndPond(owner, pond)).thenReturn(Optional.of(readOnly));

        DeviceProvisioningException exception = assertThrows(
                DeviceProvisioningException.class,
                () -> service.claim(claimRequest("some-claim-code-00000000", 1L), owner)
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("POND_ACCESS_DENIED", exception.getCode());
    }

    @Test
    void statusUsesClaimAndConnectionState() {
        when(deviceRepository.findByDeviceId("device_01")).thenReturn(Optional.of(device));
        device.setPond(pond);
        device.setLastSeenAt(LocalDateTime.now());
        when(accessRepository.existsByUserAndPond(owner, pond)).thenReturn(true);
        DeviceProvisioning provisioning = new DeviceProvisioning();
        provisioning.setDevice(device);
        provisioning.setClaimStatus(DeviceClaimStatus.CLAIMED);
        when(provisioningRepository.findByDeviceDeviceId("device_01"))
                .thenReturn(Optional.of(provisioning));

        var status = service.getStatus("device_01", owner);
        assertEquals("CLAIMED", status.claimStatus());
        assertEquals("ONLINE", status.connectionStatus());
        assertTrue(status.lastSeenAt().getOffset().getTotalSeconds() == 7 * 3600);
    }

    private DeviceProvisioning unclaimedProvisioning(String code, LocalDateTime expiresAt) {
        DeviceProvisioning provisioning = new DeviceProvisioning();
        provisioning.setDevice(device);
        provisioning.setClaimStatus(DeviceClaimStatus.UNCLAIMED);
        provisioning.setClaimCodeHash(sha256(code));
        provisioning.setClaimCodeExpiresAt(expiresAt);
        return provisioning;
    }

    private void stubClaimContext() {
        UserPondAccess access = new UserPondAccess();
        access.setUser(owner);
        access.setPond(pond);
        access.setAccessType("OWNER");
        when(deviceRepository.findByDeviceId("device_01")).thenReturn(Optional.of(device));
        when(pondRepository.findById(1L)).thenReturn(Optional.of(pond));
        when(accessRepository.findByUserAndPond(owner, pond)).thenReturn(Optional.of(access));
    }

    private DeviceClaimRequest claimRequest(String code, Long pondId) {
        DeviceClaimRequest request = new DeviceClaimRequest();
        request.setVersion(1);
        request.setDeviceId("device_01");
        request.setClaimCode(code);
        request.setPondId(pondId);
        return request;
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
