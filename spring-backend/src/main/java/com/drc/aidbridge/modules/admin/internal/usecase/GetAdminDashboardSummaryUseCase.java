package com.drc.aidbridge.modules.admin.internal.usecase;

import com.drc.aidbridge.modules.admin.internal.domain.InventoryDashboardStats;
import com.drc.aidbridge.modules.admin.internal.service.AdminDashboardService;
import com.drc.aidbridge.modules.admin.internal.web.dto.AdminDashboardSummaryResponse;
import com.drc.aidbridge.modules.aid.AidFacade;
import com.drc.aidbridge.modules.aid.AidItemCategoryDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.hub.HubInventoryDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.volunteer.VolunteerFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetAdminDashboardSummaryUseCase {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final HubFacade hubFacade;
    private final VolunteerFacade volunteerFacade;
    private final MissionFacade missionFacade;
    private final AidFacade aidFacade;
    private final AdminDashboardService adminDashboardService;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse execute() {
        long totalHubs = hubFacade.countTotalHubs();
        long totalVolunteers = volunteerFacade.countTotalVolunteers();
        long todayMissions = countTodayMissions();

        List<HubInventoryDTO> inventories = hubFacade.getAllInventories();
        InventoryDashboardStats inventoryStats = buildInventoryStats(inventories);

        // Outbound inventory logs are documented in the schema, but current Java flows only persist hub inventory.
        long distributedItemsFallback = inventoryStats.totalQuantity();

        return new AdminDashboardSummaryResponse(
                totalHubs,
                totalVolunteers,
                todayMissions,
                distributedItemsFallback,
                inventoryStats.itemCategoryStats()
        );
    }

    private long countTodayMissions() {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        Instant startOfDay = today.atStartOfDay(DASHBOARD_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(DASHBOARD_ZONE).minusNanos(1).toInstant();
        return missionFacade.countMissionsInPeriod(startOfDay, endOfDay);
    }

    private InventoryDashboardStats buildInventoryStats(List<HubInventoryDTO> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return adminDashboardService.buildInventoryStats(List.of(), Map.of(), Map.of());
        }

        Set<UUID> categoryIds = inventories.stream()
                .filter(Objects::nonNull)
                .map(HubInventoryDTO::itemCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, AidItemCategoryDTO> categoryById = toCategoryMap(aidFacade.findAllCategoriesById(categoryIds));
        Set<UUID> parentCategoryIds = categoryById.values().stream()
                .map(AidItemCategoryDTO::parentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, AidItemCategoryDTO> parentCategoryById = toCategoryMap(
                aidFacade.findAllCategoriesById(parentCategoryIds)
        );

        return adminDashboardService.buildInventoryStats(inventories, categoryById, parentCategoryById);
    }

    private Map<UUID, AidItemCategoryDTO> toCategoryMap(List<AidItemCategoryDTO> categories) {
        return categories.stream()
                .filter(c -> c != null && c.id() != null)
                .collect(Collectors.toMap(
                        AidItemCategoryDTO::id,
                        Function.identity(),
                        (left, right) -> left
                ));
    }
}
