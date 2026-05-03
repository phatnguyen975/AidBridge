package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record SearchInboundSubCategoriesResponse(
        UUID parentCategoryId,
        String parentCategoryName,
        List<InboundSubCategoryDto> items
) {
}
