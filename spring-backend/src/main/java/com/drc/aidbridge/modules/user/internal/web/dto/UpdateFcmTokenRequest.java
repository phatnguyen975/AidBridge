package com.drc.aidbridge.modules.user.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFcmTokenRequest {

    @NotBlank(message = "Device ID is required")
    @JsonProperty("device_id")
    @JsonAlias("deviceId")
    private String deviceId;

    @NotBlank(message = "FCM token is required")
    @JsonProperty("fcm_token")
    @JsonAlias("fcmToken")
    private String fcmToken;
}
