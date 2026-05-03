package com.drc.aidbridge.modules.admin.internal.web.dto;

import java.util.List;

public record AdminDashboardSummaryResponse(
        long totalHubs,
        long totalVolunteers,
        long todayMissions,
        long distributedItems,
        List<AdminDashboardCategoryStatResponse> itemCategoryStats
) {
}
