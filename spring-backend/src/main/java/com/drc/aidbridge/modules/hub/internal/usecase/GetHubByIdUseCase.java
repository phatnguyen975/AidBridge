package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetHubByIdUseCase {

    private static final String GROUP_FOOD = "Thực phẩm";
    private static final String GROUP_WATER = "Nước uống";
    private static final String GROUP_CLOTHES = "Quần áo";
    private static final String GROUP_MEDICINE = "Thuốc";
    private static final String GROUP_OTHER = "Nhu yếu phẩm khác";

    private static final List<String> GROUP_ORDER = List.of(
            GROUP_FOOD,
            GROUP_WATER,
            GROUP_CLOTHES,
            GROUP_MEDICINE,
            GROUP_OTHER);

    private final HubRepository hubRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private final HubMapper hubMapper;

    public HubDTO execute(UUID id) {
        return hubRepository.findById(id)
                .map(hub -> {
                    HubDTO dto = hubMapper.toDTO(hub);
                    if (dto == null) {
                        return null;
                    }

                    dto.setInventoryGroups(buildEmptyInventoryGroups());

                    List<HubInventory> inventories = hubInventoryRepository.findAllByHubId(hub.getId());
                    if (inventories == null || inventories.isEmpty()) {
                        return dto;
                    }

                    List<UUID> categoryIds = inventories.stream()
                            .map(HubInventory::getItemCategoryId)
                            .filter(categoryId -> categoryId != null)
                            .distinct()
                            .toList();

                    List<AidItemCategory> leafCategories = aidItemCategoryJpaRepository.findAllById(categoryIds);
                    Map<UUID, AidItemCategory> categoryById = new HashMap<>();
                    for (AidItemCategory category : leafCategories) {
                        categoryById.put(category.getId(), category);
                    }

                    List<UUID> parentIds = leafCategories.stream()
                            .map(AidItemCategory::getParentId)
                            .filter(parentId -> parentId != null)
                            .distinct()
                            .toList();

                    Map<UUID, AidItemCategory> parentCategoryById = new HashMap<>();
                    if (!parentIds.isEmpty()) {
                        List<AidItemCategory> parentCategories = aidItemCategoryJpaRepository.findAllById(parentIds);
                        for (AidItemCategory parentCategory : parentCategories) {
                            parentCategoryById.put(parentCategory.getId(), parentCategory);
                        }
                    }

                    Map<String, List<HubDTO.InventoryItemDTO>> groupedItems = initializeGroupedItems();

                    for (HubInventory inventory : inventories) {
                        if (inventory == null || inventory.getItemCategoryId() == null) {
                            continue;
                        }

                        AidItemCategory leafCategory = categoryById.get(inventory.getItemCategoryId());
                        String groupName = resolveGroupName(leafCategory, parentCategoryById);
                        groupedItems.get(groupName).add(buildInventoryItemDto(inventory, leafCategory));
                    }

                    List<HubDTO.ParentCategoryInventoryDTO> groupedInventory = new ArrayList<>();
                    for (String groupName : GROUP_ORDER) {
                        groupedInventory.add(HubDTO.ParentCategoryInventoryDTO.builder()
                                .parentCategoryName(groupName)
                                .items(groupedItems.get(groupName))
                                .build());
                    }

                    dto.setInventoryGroups(groupedInventory);
                    return dto;
                })
                .orElse(null);
    }

    private List<HubDTO.ParentCategoryInventoryDTO> buildEmptyInventoryGroups() {
        List<HubDTO.ParentCategoryInventoryDTO> groups = new ArrayList<>();
        for (String groupName : GROUP_ORDER) {
            groups.add(HubDTO.ParentCategoryInventoryDTO.builder()
                    .parentCategoryName(groupName)
                    .items(new ArrayList<>())
                    .build());
        }
        return groups;
    }

    private Map<String, List<HubDTO.InventoryItemDTO>> initializeGroupedItems() {
        Map<String, List<HubDTO.InventoryItemDTO>> groupedItems = new LinkedHashMap<>();
        for (String groupName : GROUP_ORDER) {
            groupedItems.put(groupName, new ArrayList<>());
        }
        return groupedItems;
    }

    private String resolveGroupName(AidItemCategory leafCategory, Map<UUID, AidItemCategory> parentCategoryById) {
        String candidate = "";
        if (leafCategory != null) {
            UUID parentId = leafCategory.getParentId();
            if (parentId != null) {
                AidItemCategory parent = parentCategoryById.get(parentId);
                if (parent != null && parent.getName() != null) {
                    candidate = parent.getName();
                }
            }

            if (candidate.trim().isEmpty() && leafCategory.getName() != null) {
                candidate = leafCategory.getName();
            }
        }

        String normalized = normalize(candidate);
        if (normalized.contains("thuoc") || normalized.contains("medicine") || normalized.contains("medical")) {
            return GROUP_MEDICINE;
        }
        if (normalized.contains("nuoc") || normalized.contains("water") || normalized.contains("drink")) {
            return GROUP_WATER;
        }
        if (normalized.contains("quan ao") || normalized.contains("clothes") || normalized.contains("garment")
                || normalized.contains("chan man") || normalized.contains("raincoat")) {
            return GROUP_CLOTHES;
        }
        if (normalized.contains("thuc pham") || normalized.contains("food") || normalized.contains("gao")
                || normalized.contains("mi") || normalized.contains("do hop") || normalized.contains("rice")
                || normalized.contains("noodle") || normalized.contains("canned")) {
            return GROUP_FOOD;
        }
        return GROUP_OTHER;
    }

    private HubDTO.InventoryItemDTO buildInventoryItemDto(HubInventory inventory, AidItemCategory leafCategory) {
        String itemName = leafCategory != null && leafCategory.getName() != null
                ? leafCategory.getName()
                : "";
        String unit = leafCategory != null && leafCategory.getUnit() != null
                ? leafCategory.getUnit()
                : "";

        return HubDTO.InventoryItemDTO.builder()
                .itemCategoryId(inventory.getItemCategoryId())
                .itemCategoryName(itemName)
                .unit(unit)
                .currentQuantity(inventory.getCurrentQuantity() != null ? inventory.getCurrentQuantity() : 0)
                .lowStockThreshold(inventory.getLowStockThreshold() != null ? inventory.getLowStockThreshold() : 0)
                .lastRestockedAt(inventory.getLastRestockedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
