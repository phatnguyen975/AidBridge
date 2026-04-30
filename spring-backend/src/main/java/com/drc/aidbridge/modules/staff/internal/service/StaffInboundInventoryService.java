package com.drc.aidbridge.modules.staff.internal.service;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.donation.internal.entity.Donation;
import com.drc.aidbridge.modules.donation.internal.entity.DonationItem;
import com.drc.aidbridge.modules.donation.internal.repository.DonationItemRepository;
import com.drc.aidbridge.modules.donation.internal.repository.DonationRepository;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.entity.InventoryLog;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.hub.internal.repository.InventoryLogRepository;
import com.drc.aidbridge.modules.shared.enums.DonationStatus;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.ConflictException;
import com.drc.aidbridge.modules.shared.exception.ForbiddenOperationException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInboundInventoryItemRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInboundInventoryItemResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInboundInventoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInboundInventoryResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateInboundSubCategoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.CreateInboundSubCategoryResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.InboundDonationPreviewResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.InboundHubDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.InboundParentCategoryDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.InboundSubCategoryDto;
import com.drc.aidbridge.modules.staff.internal.web.dto.SearchInboundSubCategoriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffInboundInventoryService {

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    private static final String TYPE_INBOUND_DONATION_PREVIEW = "INBOUND_DONATION_PREVIEW";
    private static final String CHANGE_TYPE_INBOUND = "INBOUND";
    private static final String REFERENCE_DONATION = "DONATION";

    private final HubStaffRepository hubStaffRepository;
    private final HubRepository hubRepository;
    private final DonationRepository donationRepository;
    private final DonationItemRepository donationItemRepository;
    private final AidItemCategoryJpaRepository itemCategoryRepository;
    private final HubInventoryRepository hubInventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional(readOnly = true)
    public InboundDonationPreviewResponse previewInbound(String code, UUID currentUserId) {
        Hub staffHub = findActiveStaffHub(currentUserId);
        Donation donation = findDonationByCode(code);
        validateDonationBelongsToHub(donation, staffHub.getId());
        validateDonationReceivable(donation);

        List<InboundParentCategoryDto> parentCategories = buildRegisteredParentCategories(donation.getId());
        if (parentCategories.isEmpty()) {
            throw new BadRequestException("Donation does not have registered parent categories");
        }

        return new InboundDonationPreviewResponse(
                TYPE_INBOUND_DONATION_PREVIEW,
                donation.getId(),
                safeText(donation.getDonationCode()),
                donation.getStatus().name(),
                new InboundHubDto(staffHub.getId(), safeText(staffHub.getName()), safeText(staffHub.getAddress())),
                parentCategories,
                "Please verify actual items before confirming inbound inventory"
        );
    }

    @Transactional(readOnly = true)
    public SearchInboundSubCategoriesResponse searchSubCategories(UUID donationId,
                                                                  UUID parentCategoryId,
                                                                  String keyword,
                                                                  UUID currentUserId) {
        Hub staffHub = findActiveStaffHub(currentUserId);
        Donation donation = findDonationById(donationId);
        validateDonationBelongsToHub(donation, staffHub.getId());
        validateDonationReceivable(donation);
        AidItemCategory parent = validateRegisteredParentCategory(donation.getId(), parentCategoryId);

        List<InboundSubCategoryDto> items = itemCategoryRepository
                .searchLeafSubCategories(parentCategoryId, normalizeSearchKeyword(keyword))
                .stream()
                .map(this::toSubCategoryDto)
                .toList();

        return new SearchInboundSubCategoriesResponse(
                parent.getId(),
                safeText(parent.getName()),
                items
        );
    }

    @Transactional
    public CreateInboundSubCategoryResponse createSubCategoryForInbound(CreateInboundSubCategoryRequest request,
                                                                        UUID currentUserId) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        Hub staffHub = findActiveStaffHub(currentUserId);
        Donation donation = findDonationById(request.getDonationId());
        validateDonationBelongsToHub(donation, staffHub.getId());
        validateDonationReceivable(donation);
        AidItemCategory parent = validateRegisteredParentCategory(donation.getId(), request.getParentCategoryId());

        String name = requiredTrim(request.getName(), "name is required");
        String unit = requiredTrim(request.getUnit(), "unit is required");
        if (itemCategoryRepository.existsByParentIdAndNameIgnoreCase(parent.getId(), name)) {
            throw new ConflictException("Sub category already exists under selected parent category");
        }

        AidItemCategory subCategory = new AidItemCategory();
        subCategory.setId(UUID.randomUUID());
        subCategory.setParentId(parent.getId());
        subCategory.setName(name);
        subCategory.setUnit(unit);
        subCategory.setIconUrl(trimToNull(request.getIconUrl()));
        subCategory.setLeaf(true);
        subCategory.setSortOrder(itemCategoryRepository.findMaxSortOrderByParentId(parent.getId()) + 1);
        AidItemCategory saved = itemCategoryRepository.save(subCategory);

        return new CreateInboundSubCategoryResponse(
                saved.getId(),
                parent.getId(),
                safeText(parent.getName()),
                safeText(saved.getName()),
                safeText(saved.getUnit()),
                saved.isLeaf(),
                "Sub category created successfully"
        );
    }

    @Transactional
    public ConfirmInboundInventoryResponse confirmInbound(ConfirmInboundInventoryRequest request, UUID currentUserId) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        Hub staffHub = findActiveStaffHub(currentUserId);
        Donation donation = findDonationByIdOrCode(request.getDonationId(), request.getCode());
        validateDonationBelongsToHub(donation, staffHub.getId());
        validateDonationReceivable(donation);

        List<ConfirmInboundInventoryItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Inbound items cannot be empty");
        }

        Map<UUID, AidItemCategory> registeredParents = loadRegisteredParentCategories(donation.getId());
        List<ValidatedInboundItem> validatedItems = validateInboundItems(items, registeredParents);
        List<ConfirmInboundInventoryItemResponse> updatedItems = new ArrayList<>();

        for (ValidatedInboundItem item : validatedItems) {
            HubInventory inventory = hubInventoryRepository
                    .findByHubIdAndItemCategoryId(staffHub.getId(), item.itemCategory().getId())
                    .orElseGet(() -> HubInventory.builder()
                            .hubId(staffHub.getId())
                            .itemCategoryId(item.itemCategory().getId())
                            .currentQuantity(0)
                            .lowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD)
                            .build());

            int newQuantity = safeQuantity(inventory.getCurrentQuantity()) + item.quantity();
            inventory.setCurrentQuantity(newQuantity);
            inventory.setLastRestockedAt(Instant.now());
            HubInventory savedInventory = hubInventoryRepository.save(inventory);

            inventoryLogRepository.save(InventoryLog.builder()
                    .hubInventoryId(savedInventory.getId())
                    .changeType(CHANGE_TYPE_INBOUND)
                    .quantityDelta(item.quantity())
                    .quantityAfter(newQuantity)
                    .referenceType(REFERENCE_DONATION)
                    .referenceId(donation.getId())
                    .notes(resolveNote(item.note(), request.getGeneralNote()))
                    .createdBy(currentUserId)
                    .build());

            updatedItems.add(new ConfirmInboundInventoryItemResponse(
                    item.parentCategory().getId(),
                    safeText(item.parentCategory().getName()),
                    item.itemCategory().getId(),
                    safeText(item.itemCategory().getName()),
                    item.quantity(),
                    newQuantity
            ));
        }

        donation.setStatus(DonationStatus.RECEIVED);
        donation.setReceivedAt(Instant.now());
        donation.setReceivedBy(currentUserId);
        donationRepository.save(donation);

        return new ConfirmInboundInventoryResponse(
                "Inbound inventory completed successfully",
                donation.getId(),
                safeText(donation.getDonationCode()),
                updatedItems
        );
    }

    private Hub findActiveStaffHub(UUID currentUserId) {
        HubStaff assignment = hubStaffRepository
                .findFirstByUserIdAndIsAvailableTrueAndUnassignedAtIsNullOrderByAssignedAtDesc(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff is not assigned to any active hub"));

        return hubRepository.findById(assignment.getHubId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned hub not found"));
    }

    private Donation findDonationByCode(String rawCode) {
        String code = requiredTrim(rawCode, "code is required");
        return donationRepository.findFirstByQrCodeTokenOrDonationCode(code, code)
                .orElseThrow(() -> new ResourceNotFoundException("Donation code not found"));
    }

    private Donation findDonationById(UUID donationId) {
        if (donationId == null) {
            throw new BadRequestException("donationId is required");
        }
        return donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
    }

    private Donation findDonationByIdOrCode(UUID donationId, String rawCode) {
        if (donationId != null) {
            return donationRepository.findById(donationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
        }
        return findDonationByCode(rawCode);
    }

    private void validateDonationBelongsToHub(Donation donation, UUID staffHubId) {
        if (!Objects.equals(donation.getHubId(), staffHubId)) {
            throw new ForbiddenOperationException("This donation does not belong to your hub");
        }
    }

    private void validateDonationReceivable(Donation donation) {
        if (donation.getStatus() == DonationStatus.RECEIVED) {
            throw new ConflictException("Donation has already been received");
        }
        if (donation.getStatus() == DonationStatus.OUTDATED) {
            throw new ConflictException("Donation cannot be received");
        }
    }

    private AidItemCategory validateRegisteredParentCategory(UUID donationId, UUID parentCategoryId) {
        if (parentCategoryId == null) {
            throw new BadRequestException("parentCategoryId is required");
        }
        if (!donationItemRepository.existsByDonationIdAndItemCategoryId(donationId, parentCategoryId)) {
            throw new BadRequestException("Parent category was not registered by sponsor");
        }
        return itemCategoryRepository.findParentCategoryById(parentCategoryId)
                .orElseThrow(() -> new BadRequestException("Registered category is not a parent category"));
    }

    private List<InboundParentCategoryDto> buildRegisteredParentCategories(UUID donationId) {
        Map<UUID, AidItemCategory> parents = loadRegisteredParentCategories(donationId);
        return parents.values().stream()
                .map(parent -> new InboundParentCategoryDto(
                        parent.getId(),
                        safeText(parent.getName()),
                        safeText(parent.getUnit()),
                        itemCategoryRepository
                                .findByParentIdAndIsLeafTrueOrderBySortOrderAscNameAsc(parent.getId())
                                .stream()
                                .map(this::toSubCategoryDto)
                                .toList()
                ))
                .toList();
    }

    private Map<UUID, AidItemCategory> loadRegisteredParentCategories(UUID donationId) {
        List<DonationItem> donationItems = donationItemRepository.findAllByDonationId(donationId);
        Set<UUID> parentIds = donationItems.stream()
                .map(DonationItem::getItemCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        Map<UUID, AidItemCategory> parents = new LinkedHashMap<>();
        for (UUID parentId : parentIds) {
            AidItemCategory parent = itemCategoryRepository.findParentCategoryById(parentId)
                    .orElseThrow(() -> new BadRequestException("Donation item category is not a parent category"));
            parents.put(parent.getId(), parent);
        }
        return parents;
    }

    private List<ValidatedInboundItem> validateInboundItems(List<ConfirmInboundInventoryItemRequest> items,
                                                            Map<UUID, AidItemCategory> registeredParents) {
        List<ValidatedInboundItem> validatedItems = new ArrayList<>();
        for (ConfirmInboundInventoryItemRequest item : items) {
            if (item == null) {
                throw new BadRequestException("Inbound item is required");
            }
            if (item.getParentCategoryId() == null) {
                throw new BadRequestException("parentCategoryId is required");
            }
            AidItemCategory parent = registeredParents.get(item.getParentCategoryId());
            if (parent == null) {
                throw new BadRequestException("Parent category was not registered by sponsor");
            }
            if (item.getItemCategoryId() == null) {
                throw new BadRequestException("itemCategoryId is required");
            }
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero");
            }

            AidItemCategory category = itemCategoryRepository.findById(item.getItemCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item category not found"));
            if (!category.isLeaf()) {
                throw new BadRequestException("Cannot inbound parent category");
            }
            if (!Objects.equals(category.getParentId(), parent.getId())) {
                throw new BadRequestException("Sub category does not belong to selected parent category");
            }

            validatedItems.add(new ValidatedInboundItem(parent, category, quantity, item.getNote()));
        }
        return validatedItems;
    }

    private InboundSubCategoryDto toSubCategoryDto(AidItemCategory category) {
        return new InboundSubCategoryDto(
                category.getId(),
                safeText(category.getName()),
                safeText(category.getUnit())
        );
    }

    private String normalizeSearchKeyword(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed;
    }

    private String resolveNote(String itemNote, String generalNote) {
        String note = trimToNull(itemNote);
        return note != null ? note : trimToNull(generalNote);
    }

    private String requiredTrim(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(message);
        }
        return trimmed;
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

    private int safeQuantity(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private record ValidatedInboundItem(
            AidItemCategory parentCategory,
            AidItemCategory itemCategory,
            int quantity,
            String note
    ) {
    }
}
