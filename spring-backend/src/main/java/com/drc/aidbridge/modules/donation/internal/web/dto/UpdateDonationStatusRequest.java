package com.drc.aidbridge.modules.donation.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDonationStatusRequest {

    @NotNull(message = "status is required")
    private DonationStatus status;
}
