package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record StaffInventoryHubDto(
        UUID id,
        String name,
        String address
) {
}
