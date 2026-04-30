package com.drc.aidbridge.modules.staff.internal.service;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.entity.InventoryLog;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.hub.internal.repository.InventoryLogRepository;
import com.drc.aidbridge.modules.mission.internal.entity.Mission;
import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.shared.enums.MissionStatus;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.ConflictException;
import com.drc.aidbridge.modules.shared.exception.ForbiddenOperationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInventoryItemRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmOutboundInventoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryQrPreviewItemDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryQrPreviewResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryTransactionItemDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryTransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffInventoryTransactionService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    private static final String TYPE_OUTBOUND_MISSION = "OUTBOUND_MISSION";
    private static final String CHANGE_TYPE_INBOUND = "INBOUND";
    private static final String CHANGE_TYPE_OUTBOUND = "OUTBOUND";
    private static final String REFERENCE_MISSION = "MISSION";

    private final HubStaffRepository hubStaffRepository;
    private final HubRepository hubRepository;
    private final MissionJpaRepository missionRepository;
    private final AidRequestItemJpaRepository aidRequestItemRepository;
    private final AidItemCategoryJpaRepository itemCategoryRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional(readOnly = true)
    public InventoryQrPreviewResponse previewOutbound(String code, UUID currentUserId) {
        Hub staffHub = findActiveStaffHub(currentUserId);
        Mission mission = findMissionByCode(code);
        validateMissionBelongsToHub(mission, staffHub.getId());
        validateMissionProcessable(mission);

        List<ItemAggregate> aggregates = aggregateAidRequestItems(mission.getAidRequestId());
        if (aggregates.isEmpty()) {
            throw new BadRequestException("Mission does not have aid request items");
        }

        List<InventoryQrPreviewItemDto> items = toOutboundPreviewItems(staffHub.getId(), aggregates);
        boolean canConfirm = items.stream().allMatch(item -> Boolean.TRUE.equals(item.isEnoughStock()));

        return new InventoryQrPreviewResponse(
                TYPE_OUTBOUND_MISSION,
                null,
                null,
                mission.getId(),
                safeText(resolveMissionCode(mission)),
                mission.getStatus().name(),
                staffHub.getId(),
                safeText(staffHub.getName()),
                items,
                canConfirm,
                canConfirm
                        ? "Mission is valid for outbound inventory"
                        : "Some items do not have enough stock");
    }

    @Transactional
    public InventoryTransactionResponse confirmOutbound(ConfirmOutboundInventoryRequest request, UUID currentUserId) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        Hub staffHub = findActiveStaffHub(currentUserId);
        Mission mission = findMissionByCode(request.getCode());
        validateMissionBelongsToHub(mission, staffHub.getId());
        validateMissionProcessable(mission);

        List<ItemAggregate> requiredItems = aggregateAidRequestItems(mission.getAidRequestId());
        if (requiredItems.isEmpty()) {
            throw new BadRequestException("Mission does not have aid request items");
        }

        Map<UUID, Integer> requiredQuantities = requiredItems.stream()
                .collect(Collectors.toMap(
                        ItemAggregate::itemCategoryId,
                        ItemAggregate::quantity,
                        Integer::sum,
                        LinkedHashMap::new));
        Map<UUID, Integer> requestedQuantities = aggregateRequestItems(request.getItems());
        validateOutboundRequestedQuantities(requestedQuantities, requiredQuantities);

        // Pre-validate all stock before mutating anything so the transaction rolls back
        // cleanly on conflict.
        Map<UUID, CategoryMeta> metaByCategoryId = loadCategoryMeta(requestedQuantities.keySet());
        for (Map.Entry<UUID, Integer> entry : requestedQuantities.entrySet()) {
            UUID itemCategoryId = entry.getKey();
            int quantity = entry.getValue();
            HubInventory inventory = hubInventoryRepository
                    .findByHubIdAndItemCategoryId(staffHub.getId(), itemCategoryId)
                    .orElseThrow(() -> new ConflictException("Inventory item not found"));

            int currentQuantity = safeQuantity(inventory.getCurrentQuantity());
            if (currentQuantity < quantity) {
                CategoryMeta meta = metaByCategoryId.get(itemCategoryId);
                throw new ConflictException("Not enough stock for item: " + safeText(meta != null ? meta.name() : ""));
            }
        }

        List<InventoryTransactionItemDto> updatedItems = applyInventoryDelta(
                staffHub.getId(),
                requestedQuantities,
                false,
                REFERENCE_MISSION,
                mission.getId(),
                request.getNote(),
                currentUserId);

        mission.setStatus(MissionStatus.PICKED_UP);
        mission.setPickedUpAt(Instant.now());
        missionRepository.save(mission);

        return new InventoryTransactionResponse(
                "Outbound inventory completed successfully",
                null,
                null,
                mission.getId(),
                safeText(resolveMissionCode(mission)),
                updatedItems);
    }

    private Hub findActiveStaffHub(UUID currentUserId) {
        HubStaff assignment = hubStaffRepository
                .findFirstByUserIdAndIsAvailableTrueAndUnassignedAtIsNullOrderByAssignedAtDesc(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff is not assigned to any active hub"));

        return hubRepository.findById(assignment.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned hub not found"));
    }

    private Mission findMissionByCode(String rawCode) {
        String code = normalizeCode(rawCode);
        Optional<Mission> byQrToken = missionRepository.findByQrCodeToken(code);
        if (byQrToken.isPresent()) {
            return byQrToken.get();
        }

        try {
            return missionRepository.findById(UUID.fromString(code))
                    .orElseThrow(() -> new ResourceNotFoundException("Mission QR code not found"));
        } catch (IllegalArgumentException ignored) {
            throw new ResourceNotFoundException("Mission QR code not found");
        }
    }

    private String normalizeCode(String rawCode) {
        String code = rawCode != null ? rawCode.trim() : "";
        if (code.isEmpty()) {
            throw new BadRequestException("code is required");
        }
        return code;
    }

    private void validateMissionBelongsToHub(Mission mission, UUID staffHubId) {
        if (!Objects.equals(mission.getHubId(), staffHubId)) {
            throw new ForbiddenOperationException("This mission does not belong to your hub");
        }
    }

    private void validateMissionProcessable(Mission mission) {
        if (mission.getAidRequestId() == null) {
            throw new BadRequestException("Mission does not have aid request items");
        }

        if (mission.getStatus() == MissionStatus.PICKED_UP
                || mission.getStatus() == MissionStatus.IN_TRANSIT
                || mission.getStatus() == MissionStatus.COMPLETED
                || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new ConflictException("Mission has already been processed");
        }
    }

    private List<ItemAggregate> aggregateAidRequestItems(UUID aidRequestId) {
        if (aidRequestId == null) {
            return List.of();
        }

        List<AidRequestItem> sourceItems = aidRequestItemRepository.findByAidRequestId(aidRequestId);
        Map<UUID, Integer> quantityByCategoryId = new LinkedHashMap<>();
        for (AidRequestItem item : sourceItems) {
            if (item == null || item.getItemCategoryId() == null) {
                continue;
            }
            quantityByCategoryId.merge(item.getItemCategoryId(), 1, Integer::sum);
        }
        return toCategoryAggregates(quantityByCategoryId);
    }

    private List<ItemAggregate> toCategoryAggregates(Map<UUID, Integer> quantityByCategoryId) {
        if (quantityByCategoryId.isEmpty()) {
            return List.of();
        }

        Map<UUID, CategoryMeta> metaByCategoryId = loadCategoryMeta(quantityByCategoryId.keySet());
        List<ItemAggregate> aggregates = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : quantityByCategoryId.entrySet()) {
            CategoryMeta meta = metaByCategoryId.get(entry.getKey());
            if (meta == null) {
                continue;
            }
            aggregates.add(new ItemAggregate(entry.getKey(), entry.getValue(), meta));
        }
        return aggregates;
    }

    private List<InventoryQrPreviewItemDto> toOutboundPreviewItems(UUID hubId, List<ItemAggregate> aggregates) {
        return aggregates.stream()
                .map(aggregate -> {
                    CategoryMeta meta = aggregate.meta();
                    int requiredQuantity = aggregate.quantity();
                    int currentQuantity = hubInventoryRepository
                            .findByHubIdAndItemCategoryId(hubId, aggregate.itemCategoryId())
                            .map(HubInventory::getCurrentQuantity)
                            .map(this::safeQuantity)
                            .orElse(0);

                    return new InventoryQrPreviewItemDto(
                            aggregate.itemCategoryId(),
                            safeText(meta.name()),
                            safeText(meta.unit()),
                            meta.parentCategoryId(),
                            safeText(meta.parentCategoryName()),
                            null,
                            requiredQuantity,
                            currentQuantity,
                            currentQuantity >= requiredQuantity);
                })
                .toList();
    }

    private Map<UUID, Integer> aggregateRequestItems(List<ConfirmInventoryItemRequest> requestItems) {
        if (requestItems == null || requestItems.isEmpty()) {
            throw new BadRequestException("items must not be empty");
        }

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (ConfirmInventoryItemRequest item : requestItems) {
            if (item == null || item.getItemCategoryId() == null) {
                throw new BadRequestException("itemCategoryId is required");
            }
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantity <= 0) {
                throw new BadRequestException("quantity must be > 0");
            }
            quantities.merge(item.getItemCategoryId(), quantity, Integer::sum);
        }
        return quantities;
    }

    private void ensureRequestedItemsBelongToSource(Set<UUID> requestedIds, Set<UUID> allowedIds, String message) {
        for (UUID requestedId : requestedIds) {
            if (!allowedIds.contains(requestedId)) {
                throw new BadRequestException(message);
            }
        }
    }

    private void validateOutboundRequestedQuantities(Map<UUID, Integer> requestedQuantities,
            Map<UUID, Integer> requiredQuantities) {
        ensureRequestedItemsBelongToSource(requestedQuantities.keySet(), requiredQuantities.keySet(),
                "Item does not belong to mission aid request");

        for (Map.Entry<UUID, Integer> required : requiredQuantities.entrySet()) {
            Integer requestedQuantity = requestedQuantities.get(required.getKey());
            if (requestedQuantity == null) {
                throw new BadRequestException("Missing required item for outbound confirmation");
            }
            if (!requestedQuantity.equals(required.getValue())) {
                throw new BadRequestException("Outbound quantity must match required quantity");
            }
        }
    }

    private List<InventoryTransactionItemDto> applyInventoryDelta(UUID hubId,
            Map<UUID, Integer> quantities,
            boolean inbound,
            String referenceType,
            UUID referenceId,
            String note,
            UUID currentUserId) {
        Map<UUID, CategoryMeta> metaByCategoryId = loadCategoryMeta(quantities.keySet());
        List<InventoryTransactionItemDto> updatedItems = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            UUID itemCategoryId = entry.getKey();
            int quantity = entry.getValue();
            CategoryMeta meta = metaByCategoryId.get(itemCategoryId);
            if (meta == null) {
                throw new ResourceNotFoundException("Item category not found: " + itemCategoryId);
            }

            HubInventory inventory = hubInventoryRepository.findByHubIdAndItemCategoryId(hubId, itemCategoryId)
                    .orElseGet(() -> {
                        if (!inbound) {
                            throw new ConflictException("Inventory item not found");
                        }
                        return HubInventory.builder()
                                .hubId(hubId)
                                .itemCategoryId(itemCategoryId)
                                .currentQuantity(0)
                                .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                                .build();
                    });

            int oldQuantity = safeQuantity(inventory.getCurrentQuantity());
            int delta = inbound ? quantity : -quantity;
            int newQuantity = oldQuantity + delta;
            if (newQuantity < 0) {
                throw new ConflictException("Not enough stock for item: " + safeText(meta.name()));
            }

            inventory.setCurrentQuantity(newQuantity);
            if (inbound) {
                inventory.setLastRestockedAt(Instant.now());
            }
            HubInventory savedInventory = hubInventoryRepository.save(inventory);

            inventoryLogRepository.save(InventoryLog.builder()
                    .hubInventoryId(savedInventory.getId())
                    .changeType(inbound ? CHANGE_TYPE_INBOUND : CHANGE_TYPE_OUTBOUND)
                    .quantityDelta(delta)
                    .quantityAfter(newQuantity)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .notes(trimToNull(note))
                    .createdBy(currentUserId)
                    .build());

            updatedItems.add(new InventoryTransactionItemDto(
                    itemCategoryId,
                    safeText(meta.name()),
                    delta,
                    newQuantity));
        }

        return updatedItems;
    }

    private Map<UUID, CategoryMeta> loadCategoryMeta(Set<UUID> itemCategoryIds) {
        if (itemCategoryIds == null || itemCategoryIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, AidItemCategory> childById = itemCategoryRepository.findAllById(itemCategoryIds).stream()
                .collect(Collectors.toMap(AidItemCategory::getId, Function.identity()));

        Set<UUID> parentIds = childById.values().stream()
                .map(AidItemCategory::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, AidItemCategory> parentById = parentIds.isEmpty()
                ? Map.of()
                : itemCategoryRepository.findAllById(parentIds).stream()
                        .collect(Collectors.toMap(AidItemCategory::getId, Function.identity()));

        Map<UUID, CategoryMeta> result = new LinkedHashMap<>();
        for (AidItemCategory child : childById.values()) {
            AidItemCategory parent = child.getParentId() != null ? parentById.get(child.getParentId()) : null;
            result.put(child.getId(), new CategoryMeta(
                    child.getId(),
                    safeText(child.getName()),
                    safeText(child.getUnit()),
                    parent != null ? parent.getId() : child.getId(),
                    parent != null ? safeText(parent.getName()) : safeText(child.getName()),
                    child.isLeaf()));
        }
        return result;
    }

    private String resolveMissionCode(Mission mission) {
        return mission.getQrCodeToken() != null && !mission.getQrCodeToken().isBlank()
                ? mission.getQrCodeToken()
                : mission.getId().toString();
    }

    private int safeQuantity(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private record ItemAggregate(UUID itemCategoryId, int quantity, CategoryMeta meta) {
    }

    private record CategoryMeta(
            UUID itemCategoryId,
            String name,
            String unit,
            UUID parentCategoryId,
            String parentCategoryName,
            boolean isLeaf) {
    }
}
