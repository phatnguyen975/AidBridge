package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.HubInventoryElementRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateHubUseCase {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final HubRepository hubRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private final HubMapper hubMapper;

    @Transactional
    public HubDTO execute(UUID id, UpdateHubRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        Hub hub = hubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hub not found: " + id));

        hubMapper.patchEntity(hub, request);
        Hub saved = hubRepository.save(hub);
        upsertInventories(saved.getId(), request.getElements());
        return hubMapper.toDTO(saved);
    }

    private void upsertInventories(UUID hubId, List<HubInventoryElementRequest> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }

        Map<UUID, InventoryAggregate> aggregateMap = aggregateElements(elements);

        for (Map.Entry<UUID, InventoryAggregate> entry : aggregateMap.entrySet()) {
            UUID itemCategoryId = entry.getKey();
            if (!aidItemCategoryJpaRepository.existsById(itemCategoryId)) {
                throw new ResourceNotFoundException("Item category not found: " + itemCategoryId);
            }

            InventoryAggregate aggregate = entry.getValue();
            HubInventory inventory = hubInventoryRepository.findByHubIdAndItemCategoryId(hubId, itemCategoryId)
                    .orElseGet(() -> HubInventory.builder()
                            .hubId(hubId)
                            .itemCategoryId(itemCategoryId)
                            .currentQuantity(0)
                            .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                            .build());

            inventory.setCurrentQuantity(inventory.getCurrentQuantity() + aggregate.quantity());
            inventory.setLowStockThreshold(aggregate.lowStockThreshold());
            if (aggregate.quantity() > 0) {
                inventory.setLastRestockedAt(Instant.now());
            }

            hubInventoryRepository.save(inventory);
        }
    }

    private Map<UUID, InventoryAggregate> aggregateElements(List<HubInventoryElementRequest> elements) {
        Map<UUID, InventoryAggregate> aggregateMap = new LinkedHashMap<>();

        for (HubInventoryElementRequest element : elements) {
            UUID itemCategoryId = element.getItemCategoryId();
            InventoryAggregate current = aggregateMap.get(itemCategoryId);

            int quantity = element.getQuantity();
            int threshold = element.getLowStockThreshold() != null
                    ? element.getLowStockThreshold()
                    : DEFAULT_LOW_STOCK_THRESHOLD;

            if (current == null) {
                aggregateMap.put(itemCategoryId, new InventoryAggregate(quantity, threshold));
            } else {
                aggregateMap.put(itemCategoryId, new InventoryAggregate(
                        current.quantity() + quantity,
                        threshold
                ));
            }
        }

        return aggregateMap;
    }

    private record InventoryAggregate(int quantity, int lowStockThreshold) {
    }
}