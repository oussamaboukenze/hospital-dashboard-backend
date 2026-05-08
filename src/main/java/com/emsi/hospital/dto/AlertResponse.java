package com.emsi.hospital.dto;

import java.time.Instant;

public record AlertResponse(
        Long id,
        String deviceId,
        String type,
        String severity,
        String message,
        Instant createdAt,
        boolean resolved
) {
}
