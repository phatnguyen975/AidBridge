package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;

public record StaffInventoryResponse(
        StaffInventoryHubDto hub,
        List<StaffInventoryFilterDto> filters,
        List<StaffInventoryItemDto> items,
        long totalItems
) {
}
