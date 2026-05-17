package com.emsi.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateUserRequest(
        @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String role,
        List<String> assignedFridgeIds
) {
}
