package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record InventoryTransactionResponse(
        String message,
        UUID donationId,
        String donationCode,
        UUID missionId,
        String missionCode,
        List<InventoryTransactionItemDto> updatedItems
) {
}
