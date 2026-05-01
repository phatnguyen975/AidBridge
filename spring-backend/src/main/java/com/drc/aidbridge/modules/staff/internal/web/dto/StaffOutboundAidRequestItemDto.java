package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

/**
 * Requested item detail for outbound inventory preview.
 */
public record StaffOutboundAidRequestItemDto(
        UUID itemCategoryId,
        String name,
        String unit,
        Integer requestedQuantity
) {
}
