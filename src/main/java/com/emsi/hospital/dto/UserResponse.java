package com.emsi.hospital.dto;

public record UserResponse(
        String id,
        String email,
        String name,
        String role
) {
}
