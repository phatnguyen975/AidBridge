package com.drc.aidbridge.modules.admin.internal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAdminStaffRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,

        @NotBlank(message = "Phone number is required")
        @Size(max = 15, message = "Phone number must be at most 15 characters")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotNull(message = "Hub is required")
        UUID hubId
) {
}
