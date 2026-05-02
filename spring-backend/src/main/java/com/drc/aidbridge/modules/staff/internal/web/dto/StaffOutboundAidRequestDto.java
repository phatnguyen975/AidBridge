package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Aid request detail for outbound inventory preview.
 */
public record StaffOutboundAidRequestDto(
        UUID id,
        String description,
        Integer numberAdult,
        Integer numberElderly,
        Integer numberChildren,
        List<StaffOutboundAidRequestItemDto> items
) {
}
