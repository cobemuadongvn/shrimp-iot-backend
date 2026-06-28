package com.example.shrimpiot.dto;

import java.time.OffsetDateTime;

public record ClaimCodeIssueResponse(
        String deviceId,
        String claimCode,
        OffsetDateTime expiresAt
) {
}
