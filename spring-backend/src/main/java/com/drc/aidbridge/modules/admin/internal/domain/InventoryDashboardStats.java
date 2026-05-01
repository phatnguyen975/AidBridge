package com.drc.aidbridge.modules.admin.internal.domain;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminDashboardCategoryStatResponse;
import java.util.List;

public record InventoryDashboardStats(
        long totalQuantity,
        List<AdminDashboardCategoryStatResponse> itemCategoryStats
) {
}
