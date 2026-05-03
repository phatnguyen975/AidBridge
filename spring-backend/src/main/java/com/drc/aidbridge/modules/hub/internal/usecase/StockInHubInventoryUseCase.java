package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.HubInventoryElementRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.StockInHubInventoryRequest;
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
public class StockInHubInventoryUseCase {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final HubRepository hubRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private final HubMapper hubMapper;

    @Transactional
    public HubDTO execute(UUID hubId, StockInHubInventoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }

        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub not found: " + hubId));

        Map<UUID, InventoryAggregate> aggregateMap = aggregateElements(request.getElements());
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

        return hubMapper.toDTO(hub);
    }

    private Map<UUID, InventoryAggregate> aggregateElements(List<HubInventoryElementRequest> elements) {
        Map<UUID, InventoryAggregate> aggregateMap = new LinkedHashMap<>();
        for (HubInventoryElementRequest element : elements) {
            UUID itemCategoryId = element.getItemCategoryId();
            InventoryAggregate current = aggregateMap.get(itemCategoryId);

            int quantity = element.getQuantity() != null ? element.getQuantity() : 0;
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
