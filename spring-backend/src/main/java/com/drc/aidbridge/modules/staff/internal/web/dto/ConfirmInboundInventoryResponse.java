package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record ConfirmInboundInventoryResponse(
        String message,
        UUID donationId,
        String donationCode,
        List<ConfirmInboundInventoryItemResponse> updatedItems
) {
}
