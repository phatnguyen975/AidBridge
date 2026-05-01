package com.drc.aidbridge.modules.staff.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
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
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.exception.ConflictException;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmInventoryItemRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.ConfirmOutboundInventoryRequest;
import com.drc.aidbridge.modules.staff.internal.web.dto.InventoryQrPreviewResponse;
import com.drc.aidbridge.modules.staff.internal.web.dto.StaffOutboundAidRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffOutboundInventoryUseCaseTest {

    private HubStaffRepository hubStaffRepository;
    private HubRepository hubRepository;
    private MissionJpaRepository missionRepository;
    private AidRequestJpaRepository aidRequestRepository;
    private AidRequestItemJpaRepository aidRequestItemRepository;
    private AidItemCategoryJpaRepository itemCategoryRepository;
    private HubInventoryRepository hubInventoryRepository;
    private InventoryLogRepository inventoryLogRepository;

    private StaffOutboundInventoryUseCase useCase;

    @BeforeEach
    void setUp() {
        hubStaffRepository = mock(HubStaffRepository.class);
        hubRepository = mock(HubRepository.class);
        missionRepository = mock(MissionJpaRepository.class);
        aidRequestRepository = mock(AidRequestJpaRepository.class);
        aidRequestItemRepository = mock(AidRequestItemJpaRepository.class);
        itemCategoryRepository = mock(AidItemCategoryJpaRepository.class);
        hubInventoryRepository = mock(HubInventoryRepository.class);
        inventoryLogRepository = mock(InventoryLogRepository.class);

        useCase = new StaffOutboundInventoryUseCase(
                hubStaffRepository,
                hubRepository,
                missionRepository,
                aidRequestRepository,
                aidRequestItemRepository,
                itemCategoryRepository,
                hubInventoryRepository,
                inventoryLogRepository
        );
    }

    @Test
    void previewOutbound_ShouldIncludeAidRequestDetail() {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID aidRequestId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(hubStaffRepository.findFirstByUserIdAndIsAvailableTrueAndUnassignedAtIsNullOrderByAssignedAtDesc(userId))
                .thenReturn(Optional.of(HubStaff.builder().hubId(hubId).build()));
        when(hubRepository.findById(hubId))
                .thenReturn(Optional.of(Hub.builder().id(hubId).name("Hub A").build()));
        when(missionRepository.findByCodeNameIgnoreCase("MIS-2026-001"))
                .thenReturn(Optional.of(Mission.builder()
                        .id(missionId)
                        .hubId(hubId)
                        .aidRequestId(aidRequestId)
                        .status(MissionStatus.PENDING)
                        .missionType(MissionType.DELIVERY)
                        .codeName("MIS-2026-001")
                        .build()));

        AidRequest aidRequest = AidRequest.builder()
                .id(aidRequestId)
                .description("Need supplies")
                .numberAdult(2)
                .numberElderly(1)
                .numberChildren(3)
                .build();
        when(aidRequestRepository.findById(aidRequestId)).thenReturn(Optional.of(aidRequest));

        when(aidRequestItemRepository.findByAidRequestId(aidRequestId)).thenReturn(List.of(
                AidRequestItem.builder().itemCategoryId(categoryId).build()
        ));
        when(aidRequestItemRepository.findDetailRowsByAidRequestId(aidRequestId)).thenReturn(List.of(
                detailRow(categoryId, "Rice", "kg", 2L)
        ));

        AidItemCategory category = new AidItemCategory();
        category.setId(categoryId);
        category.setName("Rice");
        category.setUnit("kg");
        category.setLeaf(true);
        when(itemCategoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));
        when(hubInventoryRepository.findByHubIdAndItemCategoryId(hubId, categoryId))
                .thenReturn(Optional.of(HubInventory.builder().currentQuantity(5).build()));

        InventoryQrPreviewResponse response = useCase.previewOutbound("MIS-2026-001", userId);

        assertNotNull(response);
        StaffOutboundAidRequestDto detail = response.aidRequestDetail();
        assertNotNull(detail);
        assertEquals(aidRequestId, detail.id());
        assertEquals(2, detail.numberAdult());
        assertEquals(1, detail.numberElderly());
        assertEquals(3, detail.numberChildren());
        assertEquals(1, detail.items().size());
    }

    @Test
    void confirmOutbound_ShouldThrow_WhenOverStock() {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID aidRequestId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        mockStaffHub(userId, hubId);
        when(missionRepository.findByCodeNameIgnoreCase("MIS-2026-002"))
                .thenReturn(Optional.of(Mission.builder()
                        .id(missionId)
                        .hubId(hubId)
                        .aidRequestId(aidRequestId)
                        .status(MissionStatus.PENDING)
                        .missionType(MissionType.DELIVERY)
                        .codeName("MIS-2026-002")
                        .build()));

        when(hubInventoryRepository.findByHubIdAndItemCategoryId(hubId, categoryId))
                .thenReturn(Optional.of(HubInventory.builder().currentQuantity(1).build()));
        AidItemCategory category = new AidItemCategory();
        category.setId(categoryId);
        category.setName("Rice");
        category.setUnit("kg");
        category.setLeaf(true);
        when(itemCategoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));

        ConfirmOutboundInventoryRequest request = ConfirmOutboundInventoryRequest.builder()
                .code("MIS-2026-002")
                .items(List.of(ConfirmInventoryItemRequest.builder()
                        .itemCategoryId(categoryId)
                        .quantity(5)
                        .build()))
                .note("note")
                .build();

        assertThrows(ConflictException.class, () -> useCase.confirmOutbound(request, userId));
    }

    @Test
    void confirmOutbound_ShouldWriteOutboundLog() {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        UUID aidRequestId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        mockStaffHub(userId, hubId);
        when(missionRepository.findByCodeNameIgnoreCase("MIS-2026-003"))
                .thenReturn(Optional.of(Mission.builder()
                        .id(missionId)
                        .hubId(hubId)
                        .aidRequestId(aidRequestId)
                        .status(MissionStatus.PENDING)
                        .missionType(MissionType.DELIVERY)
                        .codeName("MIS-2026-003")
                        .build()));

        HubInventory inventory = HubInventory.builder()
                .id(UUID.randomUUID())
                .hubId(hubId)
                .itemCategoryId(categoryId)
                .currentQuantity(10)
                .build();
        when(hubInventoryRepository.findByHubIdAndItemCategoryId(hubId, categoryId))
                .thenReturn(Optional.of(inventory));
        when(hubInventoryRepository.save(inventory)).thenReturn(inventory);

        AidItemCategory category = new AidItemCategory();
        category.setId(categoryId);
        category.setName("Rice");
        category.setUnit("kg");
        category.setLeaf(true);
        when(itemCategoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));

        ConfirmOutboundInventoryRequest request = ConfirmOutboundInventoryRequest.builder()
                .code("MIS-2026-003")
                .items(List.of(ConfirmInventoryItemRequest.builder()
                        .itemCategoryId(categoryId)
                        .quantity(3)
                        .build()))
                .note("note")
                .build();

        useCase.confirmOutbound(request, userId);

        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        InventoryLog log = logCaptor.getValue();
        assertEquals("OUTBOUND", log.getChangeType());
        assertEquals("OUTBOUND", log.getReferenceType());
        assertEquals(missionId, log.getReferenceId());
        assertEquals(-3, log.getQuantityDelta());
        assertEquals(7, inventory.getCurrentQuantity());
    }

    private void mockStaffHub(UUID userId, UUID hubId) {
        when(hubStaffRepository.findFirstByUserIdAndIsAvailableTrueAndUnassignedAtIsNullOrderByAssignedAtDesc(userId))
                .thenReturn(Optional.of(HubStaff.builder().hubId(hubId).build()));
        when(hubRepository.findById(hubId)).thenReturn(Optional.of(Hub.builder().id(hubId).name("Hub A").build()));
    }

    private AidRequestItemJpaRepository.AidRequestItemDetailProjection detailRow(UUID categoryId,
            String name,
            String unit,
            Long count) {
        return new AidRequestItemJpaRepository.AidRequestItemDetailProjection() {
            @Override
            public UUID getItemCategoryId() {
                return categoryId;
            }

            @Override
            public String getCategoryName() {
                return name;
            }

            @Override
            public String getUnit() {
                return unit;
            }

            @Override
            public Long getItemCount() {
                return count;
            }
        };
    }
}
