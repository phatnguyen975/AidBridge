package com.drc.aidbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPickupRequestDto {

    /**
     * QR code token scanned at the hub for verification.
     * Optional but recommended for delivery missions.
     */
    private String qrCodeToken;
}
