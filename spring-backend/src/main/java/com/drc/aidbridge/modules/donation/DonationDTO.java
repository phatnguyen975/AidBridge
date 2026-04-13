package com.drc.aidbridge.modules.donation;

import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationDTO {
    private UUID id;
    private UUID sponsorId;
    private UUID hubId;
    private String qrCodeToken;
    private DonationStatus status;
    private String notes;
    private Instant receivedAt;
    private UUID receivedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
