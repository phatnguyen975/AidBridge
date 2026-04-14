package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @JsonProperty("full_name")
    private String fullName;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    @JsonProperty("avatar_url")
    private String avatarUrl;
}
