package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record ConfirmInboundInventoryItemResponse(
        UUID parentCategoryId,
        String parentCategoryName,
        UUID itemCategoryId,
        String itemName,
        Integer quantityDelta,
        Integer quantityAfter
) {
}
