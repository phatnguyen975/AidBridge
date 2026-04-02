package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho login.
 * Khớp với LoginRequest schema trong api.yaml.
 * Có thể login bằng email HOẶC phone_number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Email(message = "Invalid email format")
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;

    // Extended fields (không có trong api.yaml nhưng hữu ích cho mobile app)
    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("fcm_token")
    private String fcmToken;
}
