package com.drc.aidbridge.modules.donation;

import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight donation history payload for list APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationHistorySummaryDTO {

    private UUID id;
    private String donationCode;
    private String hubName;
    private String itemSummary;
    private int totalQuantity;
    private DonationStatus status;
    private Instant createdAt;
    private String qrCodeToken;
}
