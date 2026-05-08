package com.emsi.hospital.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @Email String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String role
) {
}
