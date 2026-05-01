package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record InboundDonationPreviewResponse(
        String type,
        UUID donationId,
        String donationCode,
        String status,
        InboundHubDto hub,
        List<InboundParentCategoryDto> registeredParentCategories,
        String message
) {
}
