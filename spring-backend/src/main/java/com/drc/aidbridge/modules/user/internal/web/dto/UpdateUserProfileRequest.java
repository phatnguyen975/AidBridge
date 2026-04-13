package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+()\\-\\s]{8,20}$", message = "Phone number format is invalid")
    @JsonProperty("phone")
    private String phone;

    @JsonProperty("address")
    private String address;
}
