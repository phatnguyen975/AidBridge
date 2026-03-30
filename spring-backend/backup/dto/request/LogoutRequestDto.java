package com.drc.aidbridge.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for logout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequestDto {

    // Optional: refresh token to blacklist
    private String refreshToken;
}
