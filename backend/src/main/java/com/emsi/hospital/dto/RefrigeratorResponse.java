package com.emsi.hospital.dto;

import java.time.Instant;

public record RefrigeratorResponse(
        Long id,
        String deviceId,
        String label,
        String location,
        double minTemperature,
        double maxTemperature,
        Double lastTemperature,
        Double lastHumidity,
        Boolean doorOpen,
        Integer battery,
        Instant lastSeenAt,
        String status
) {
}
