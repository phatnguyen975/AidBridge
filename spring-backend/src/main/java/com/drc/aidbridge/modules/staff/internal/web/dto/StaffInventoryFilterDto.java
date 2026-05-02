package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record StaffInventoryFilterDto(
        UUID id,
        String name,
        String type
) {
}
