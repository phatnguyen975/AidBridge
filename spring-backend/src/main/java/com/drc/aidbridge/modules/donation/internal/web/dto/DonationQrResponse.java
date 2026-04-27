package com.drc.aidbridge.modules.donation.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationQrResponse {
    private UUID donationId;
    private String donationCode;
    private String qrCodeToken;
}
