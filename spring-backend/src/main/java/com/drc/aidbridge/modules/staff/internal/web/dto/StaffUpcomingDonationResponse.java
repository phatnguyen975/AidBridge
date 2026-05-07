package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record StaffUpcomingDonationResponse(
        UUID id,
        String donationCode,
        String name,
        String phoneNumber
) {
}
