package com.emsi.hospital.dto;

import java.util.List;

public record UserResponse(
        String id,
        String email,
        String name,
        String role,
        List<String> assignedFridgeIds
) {
}
