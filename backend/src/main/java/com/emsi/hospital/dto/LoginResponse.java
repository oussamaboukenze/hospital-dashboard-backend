package com.emsi.hospital.dto;

public record LoginResponse(
        UserResponse user,
        String token
) {
}
