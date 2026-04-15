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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetHubByIdUseCase {

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
                    dto.setInventory(new ArrayList<>());

                    List<HubInventory> inventories = hubInventoryRepository.findAllByHubId(hub.getId());
                    if (inventories == null || inventories.isEmpty()) {
                        return dto;
                    }

                    Map<UUID, String> categoryNameById = new HashMap<>();
                    List<UUID> categoryIds = inventories.stream()
                            .map(HubInventory::getItemCategoryId)
                            .filter(categoryId -> categoryId != null)
                            .distinct()
                            .toList();
                    List<AidItemCategory> categories = aidItemCategoryJpaRepository.findAllById(categoryIds);
                    for (AidItemCategory category : categories) {
                        categoryNameById.put(category.getId(), category.getName());
                    }

                    List<HubDTO.InventoryItemDTO> inventoryItems = inventories.stream()
                            .map(inventory -> HubDTO.InventoryItemDTO.builder()
                                    .itemCategoryId(inventory.getItemCategoryId())
                                    .itemCategoryName(categoryNameById.get(inventory.getItemCategoryId()))
                                    .currentQuantity(inventory.getCurrentQuantity())
                                    .lowStockThreshold(inventory.getLowStockThreshold())
                                    .lastRestockedAt(inventory.getLastRestockedAt())
                                    .build())
                            .toList();
                    dto.setInventory(inventoryItems);
                    return dto;
                })
                .orElse(null);
    }
}
