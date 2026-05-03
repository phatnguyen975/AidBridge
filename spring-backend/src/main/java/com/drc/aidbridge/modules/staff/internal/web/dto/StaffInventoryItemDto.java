package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.time.Instant;
import java.util.UUID;

public record StaffInventoryItemDto(
        UUID inventoryId,
        UUID itemCategoryId,
        String name,
        String unit,
        String iconUrl,
        UUID parentCategoryId,
        String parentCategoryName,
        Integer currentQuantity,
        Integer lowStockThreshold,
        boolean isLowStock,
        Instant lastRestockedAt
) {
}
