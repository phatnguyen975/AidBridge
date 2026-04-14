package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Extended fields
    @JsonProperty("device_id")
    @JsonAlias("deviceId")
    private String deviceId;

    @JsonProperty("fcm_token")
    @JsonAlias("fcmToken")
    private String fcmToken;
}
