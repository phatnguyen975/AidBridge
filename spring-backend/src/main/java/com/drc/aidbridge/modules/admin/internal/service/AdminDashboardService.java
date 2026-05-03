package com.drc.aidbridge.modules.admin.internal.service;

import com.drc.aidbridge.modules.admin.internal.domain.DashboardItemCategory;
import com.drc.aidbridge.modules.admin.internal.domain.InventoryDashboardStats;
import com.drc.aidbridge.modules.admin.internal.web.dto.AdminDashboardCategoryStatResponse;
import com.drc.aidbridge.modules.aid.AidItemCategoryDTO;
import com.drc.aidbridge.modules.hub.HubInventoryDTO;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminDashboardService {

    public InventoryDashboardStats buildInventoryStats(
            List<HubInventoryDTO> inventories,
            Map<UUID, AidItemCategoryDTO> categoryById,
            Map<UUID, AidItemCategoryDTO> parentCategoryById
    ) {
        Map<DashboardItemCategory, Long> quantitiesByCategory = new EnumMap<>(DashboardItemCategory.class);
        for (DashboardItemCategory category : DashboardItemCategory.values()) {
            quantitiesByCategory.put(category, 0L);
        }

        if (inventories == null || inventories.isEmpty()) {
            return new InventoryDashboardStats(0L, toResponseStats(quantitiesByCategory));
        }

        long totalQuantity = 0L;
        for (HubInventoryDTO inventory : inventories) {
            if (inventory == null) continue;

            long quantity = safeQuantity(inventory.currentQuantity());
            totalQuantity += quantity;

            AidItemCategoryDTO category = categoryById.get(inventory.itemCategoryId());
            DashboardItemCategory dashboardCategory = resolveDashboardCategory(category, parentCategoryById);
            quantitiesByCategory.merge(dashboardCategory, quantity, Long::sum);
        }

        return new InventoryDashboardStats(totalQuantity, toResponseStats(quantitiesByCategory));
    }

    private List<AdminDashboardCategoryStatResponse> toResponseStats(
            Map<DashboardItemCategory, Long> quantitiesByCategory
    ) {
        List<AdminDashboardCategoryStatResponse> stats = new ArrayList<>();
        for (DashboardItemCategory category : DashboardItemCategory.values()) {
            stats.add(new AdminDashboardCategoryStatResponse(
                    category.getLabel(),
                    quantitiesByCategory.getOrDefault(category, 0L)
            ));
        }
        return stats;
    }

    private DashboardItemCategory resolveDashboardCategory(
            AidItemCategoryDTO category,
            Map<UUID, AidItemCategoryDTO> parentCategoryById
    ) {
        if (category == null) {
            return DashboardItemCategory.OTHER;
        }

        StringBuilder searchableText = new StringBuilder();
        searchableText.append(safeText(category.name()));

        AidItemCategoryDTO parentCategory = parentCategoryById.get(category.parentId());
        if (parentCategory != null) {
            searchableText.append(' ').append(safeText(parentCategory.name()));
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
}
