package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record InboundSubCategoryDto(
        UUID itemCategoryId,
        String name,
        String unit
) {
}
