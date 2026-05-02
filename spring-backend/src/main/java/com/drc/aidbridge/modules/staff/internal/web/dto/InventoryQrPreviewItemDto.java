package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record InventoryQrPreviewItemDto(
        UUID itemCategoryId,
        String name,
        String unit,
        UUID parentCategoryId,
        String parentCategoryName,
        Integer quantity,
        Integer requiredQuantity,
        Integer currentQuantity,
        Boolean isEnoughStock
) {
}
