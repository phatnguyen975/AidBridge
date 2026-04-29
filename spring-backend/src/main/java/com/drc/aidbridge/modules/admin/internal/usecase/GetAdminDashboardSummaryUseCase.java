package com.drc.aidbridge.modules.admin.internal.usecase;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminDashboardCategoryStatResponse;
import com.drc.aidbridge.modules.admin.internal.web.dto.AdminDashboardSummaryResponse;
import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    private final HubRepository hubRepository;
    private final VolunteerJpaRepository volunteerRepository;
    private final MissionJpaRepository missionRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse execute() {
        long totalHubs = hubRepository.count();
        long totalVolunteers = volunteerRepository.count();
        long todayMissions = countTodayMissions();

        InventoryDashboardStats inventoryStats = buildInventoryStats(hubInventoryRepository.findAll());
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
        return missionRepository.countByCreatedAtBetween(startOfDay, endOfDay);
    }

    private InventoryDashboardStats buildInventoryStats(List<HubInventory> inventories) {
        Map<DashboardItemCategory, Long> quantitiesByCategory = new EnumMap<>(DashboardItemCategory.class);
        for (DashboardItemCategory category : DashboardItemCategory.values()) {
            quantitiesByCategory.put(category, 0L);
        }

        if (inventories == null || inventories.isEmpty()) {
            return new InventoryDashboardStats(0L, toResponseStats(quantitiesByCategory));
        }

        Set<UUID> categoryIds = inventories.stream()
                .filter(Objects::nonNull)
                .map(HubInventory::getItemCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, AidItemCategory> categoryById = toCategoryMap(aidItemCategoryJpaRepository.findAllById(categoryIds));
        Set<UUID> parentCategoryIds = categoryById.values().stream()
                .map(AidItemCategory::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, AidItemCategory> parentCategoryById = toCategoryMap(
                aidItemCategoryJpaRepository.findAllById(parentCategoryIds)
        );

        long totalQuantity = 0L;
        for (HubInventory inventory : inventories) {
            if (inventory == null) {
                continue;
            }

            long quantity = safeQuantity(inventory.getCurrentQuantity());
            totalQuantity += quantity;

            AidItemCategory category = categoryById.get(inventory.getItemCategoryId());
            DashboardItemCategory dashboardCategory = resolveDashboardCategory(category, parentCategoryById);
            quantitiesByCategory.merge(dashboardCategory, quantity, Long::sum);
        }

        return new InventoryDashboardStats(totalQuantity, toResponseStats(quantitiesByCategory));
    }

    private Map<UUID, AidItemCategory> toCategoryMap(Iterable<AidItemCategory> categories) {
        List<AidItemCategory> categoryList = new ArrayList<>();
        for (AidItemCategory category : categories) {
            if (category != null && category.getId() != null) {
                categoryList.add(category);
            }
        }

        return categoryList.stream()
                .collect(Collectors.toMap(
                        AidItemCategory::getId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private List<AdminDashboardCategoryStatResponse> toResponseStats(
            Map<DashboardItemCategory, Long> quantitiesByCategory
    ) {
        List<AdminDashboardCategoryStatResponse> stats = new ArrayList<>();
        for (DashboardItemCategory category : DashboardItemCategory.values()) {
            stats.add(new AdminDashboardCategoryStatResponse(
                    category.label,
                    quantitiesByCategory.getOrDefault(category, 0L)
            ));
        }
        return stats;
    }

    private DashboardItemCategory resolveDashboardCategory(
            AidItemCategory category,
            Map<UUID, AidItemCategory> parentCategoryById
    ) {
        if (category == null) {
            return DashboardItemCategory.OTHER;
        }

        StringBuilder searchableText = new StringBuilder();
        searchableText.append(safeText(category.getName()));

        AidItemCategory parentCategory = parentCategoryById.get(category.getParentId());
        if (parentCategory != null) {
            searchableText.append(' ').append(safeText(parentCategory.getName()));
        }

        String normalized = normalize(searchableText.toString());
        if (containsAny(normalized, "water", "drink", "nuoc", "uong", "sua", "milk", "dien giai")) {
            return DashboardItemCategory.WATER;
        }
        if (containsAny(normalized, "clothing", "clothes", "cloth", "quan ao", "quan", "blanket", "raincoat", "chan man")) {
            return DashboardItemCategory.CLOTHING;
        }
        if (containsAny(normalized, "food", "thuc pham", "thuc an", "gao", "rice", "noodle", "mi tom", "canned", "do hop")) {
            return DashboardItemCategory.FOOD;
        }
        if (containsAny(normalized, "medicine", "medical", "medic", "thuoc", "y te", "bandage", "bang gac", "fever", "digestive", "tieu hoa")) {
            return DashboardItemCategory.MEDICINE;
        }
        return DashboardItemCategory.OTHER;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(safeText(value), Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private long safeQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            return 0L;
        }
        return quantity.longValue();
    }

    private enum DashboardItemCategory {
        WATER("N\u01b0\u1edbc u\u1ed1ng"),
        CLOTHING("Qu\u1ea7n \u00e1o"),
        FOOD("Th\u1ef1c ph\u1ea9m"),
        MEDICINE("Thu\u1ed1c men"),
        OTHER("Nhu y\u1ebfu ph\u1ea9m kh\u00e1c");

        private final String label;

        DashboardItemCategory(String label) {
            this.label = label;
        }
    }

    private record InventoryDashboardStats(
            long totalQuantity,
            List<AdminDashboardCategoryStatResponse> itemCategoryStats
    ) {
    }
}
