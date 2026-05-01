package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record InboundParentCategoryDto(
        UUID parentCategoryId,
        String parentCategoryName,
        String unit,
        List<InboundSubCategoryDto> availableSubCategories
) {
}
