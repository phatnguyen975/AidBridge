package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record InventoryTransactionItemDto(
        UUID itemCategoryId,
        String name,
        Integer quantityDelta,
        Integer quantityAfter
) {
}
