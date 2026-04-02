package com.drc.aidbridge.modules.user.internal.web.dto;

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
    private String deviceId;

    @NotBlank(message = "FCM token is required")
    private String fcmToken;
}
