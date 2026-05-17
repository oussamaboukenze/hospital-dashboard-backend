package com.emsi.hospital.dto;

import java.time.Instant;

public record ReadingResponse(
        Long id,
        String deviceId,
        double temperature,
        double humidity,
        boolean doorOpen,
        int battery,
        Instant recordedAt
) {
}
