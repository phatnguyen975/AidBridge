package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record CreateInboundSubCategoryResponse(
        UUID itemCategoryId,
        UUID parentCategoryId,
        String parentCategoryName,
        String name,
        String unit,
        boolean isLeaf,
        String message
) {
}
