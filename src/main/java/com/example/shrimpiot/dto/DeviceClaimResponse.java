package com.example.shrimpiot.dto;

public record DeviceClaimResponse(
        String deviceId,
        Long pondId,
        String claimStatus,
        String connectionStatus
) {
}
