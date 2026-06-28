package com.example.shrimpiot.dto;

import java.time.OffsetDateTime;

public record DeviceProvisioningStatusResponse(
        String deviceId,
        String claimStatus,
        String connectionStatus,
        OffsetDateTime lastSeenAt
) {
}
