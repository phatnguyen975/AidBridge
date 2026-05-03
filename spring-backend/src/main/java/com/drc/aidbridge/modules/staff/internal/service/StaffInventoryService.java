package com.drc.aidbridge.modules.staff.internal.service;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffInventoryFilterDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffInventoryHubDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffInventoryItemDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffInventoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffInventoryService {

    private static final String FILTER_TYPE_ALL = "ALL";
    private static final String FILTER_TYPE_PARENT_CATEGORY = "PARENT_CATEGORY";
    private static final List<String> STAFF_PARENT_FILTER_NAMES = List.of(
            "N\u01b0\u1edbc u\u1ed1ng",
            "Nhu y\u1ebfu ph\u1ea9m kh\u00e1c",
            "Qu\u1ea7n \u00e1o",
            "Thu\u1ed1c",
            "Th\u1ee9c \u0103n"
    );

    private final HubStaffRepository hubStaffRepository;
    private final HubRepository hubRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final AidItemCategoryJpaRepository itemCategoryRepository;

    @Transactional(readOnly = true)
    public StaffInventoryResponse getMyHubInventory(UUID currentUserId,
                                                    UUID parentCategoryId,
                                                    String parentCategoryName,
                                                    String keyword,
                                                    int page,
                                                    int size) {
        HubStaff assignment = hubStaffRepository
                .findFirstByUserIdAndIsAvailableTrueAndUnassignedAtIsNullOrderByAssignedAtDesc(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff is not assigned to any active hub"));

        Hub hub = hubRepository.findById(assignment.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned hub not found"));

        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        Page<HubInventoryRepository.StaffHubInventoryRow> inventoryPage =
                hubInventoryRepository.searchStaffHubInventory(
                        hub.getId(),
                        parentCategoryId,
                        normalizeText(parentCategoryName),
                        normalizeText(keyword),
                        pageable
                );

        List<StaffInventoryItemDto> items = inventoryPage.getContent().stream()
                .map(this::toItemDto)
                .toList();

        return new StaffInventoryResponse(
                new StaffInventoryHubDto(hub.getId(), safeText(hub.getName()), safeText(hub.getAddress())),
                buildFilters(),
                items,
                inventoryPage.getTotalElements()
        );
    }

    private StaffInventoryItemDto toItemDto(HubInventoryRepository.StaffHubInventoryRow row) {
        int currentQuantity = safeQuantity(row.getCurrentQuantity());
        int lowStockThreshold = safeQuantity(row.getLowStockThreshold());
        return new StaffInventoryItemDto(
                row.getInventoryId(),
                row.getItemCategoryId(),
                safeText(row.getName()),
                safeText(row.getUnit()),
                safeText(row.getIconUrl()),
                row.getParentCategoryId(),
                safeText(row.getParentCategoryName()),
                currentQuantity,
                lowStockThreshold,
                currentQuantity <= lowStockThreshold,
                row.getLastRestockedAt()
        );
    }

    private List<StaffInventoryFilterDto> buildFilters() {
        Map<String, AidItemCategory> categoryByNormalizedName = itemCategoryRepository
                .findParentCategoriesByNames(STAFF_PARENT_FILTER_NAMES)
                .stream()
                .collect(Collectors.toMap(
                        category -> normalizeKey(category.getName()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<StaffInventoryFilterDto> parentFilters = STAFF_PARENT_FILTER_NAMES.stream()
                .map(name -> {
                    AidItemCategory category = categoryByNormalizedName.get(normalizeKey(name));
                    return new StaffInventoryFilterDto(
                            category != null ? category.getId() : null,
                            name,
                            FILTER_TYPE_PARENT_CATEGORY
                    );
                })
                .toList();

        List<StaffInventoryFilterDto> filters = new ArrayList<>();
        filters.add(new StaffInventoryFilterDto(null, "T\u1ea5t c\u1ea3", FILTER_TYPE_ALL));
        filters.addAll(parentFilters);
        return filters;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 50;
        }
        return Math.min(size, 100);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeKey(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private int safeQuantity(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }
}
