package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetHubByIdUseCaseTest {

    private HubRepository hubRepository;
    private HubInventoryRepository hubInventoryRepository;
    private AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private HubMapper hubMapper;
    private GetHubByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        hubRepository = mock(HubRepository.class);
        hubInventoryRepository = mock(HubInventoryRepository.class);
        aidItemCategoryJpaRepository = mock(AidItemCategoryJpaRepository.class);
        hubMapper = mock(HubMapper.class);
        useCase = new GetHubByIdUseCase(hubRepository, hubInventoryRepository, aidItemCategoryJpaRepository, hubMapper);
    }

    @Test
    void execute_ShouldReturnHubWithInventory_WhenHubExists() {
        UUID hubId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Hub hub = Hub.builder().id(hubId).name("Hub A").build();
        HubDTO dto = HubDTO.builder().id(hubId).name("Hub A").build();
        HubInventory inventory = HubInventory.builder()
                .hubId(hubId)
                .itemCategoryId(categoryId)
                .currentQuantity(25)
                .lowStockThreshold(5)
                .lastRestockedAt(Instant.now())
                .build();
        AidItemCategory category = mock(AidItemCategory.class);
        when(category.getId()).thenReturn(categoryId);
        when(category.getName()).thenReturn("Gao");

        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(hubMapper.toDTO(hub)).thenReturn(dto);
        when(hubInventoryRepository.findAllByHubId(hubId)).thenReturn(List.of(inventory));
        when(aidItemCategoryJpaRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));

        HubDTO result = useCase.execute(hubId);

        assertNotNull(result);
        assertNotNull(result.getInventory());
        assertEquals(1, result.getInventory().size());
        assertEquals(categoryId, result.getInventory().getFirst().getItemCategoryId());
        assertEquals("Gao", result.getInventory().getFirst().getItemCategoryName());
        assertEquals(25, result.getInventory().getFirst().getCurrentQuantity());
        assertEquals(5, result.getInventory().getFirst().getLowStockThreshold());
    }

    @Test
    void execute_ShouldReturnNull_WhenHubNotFound() {
        UUID hubId = UUID.randomUUID();
        when(hubRepository.findById(hubId)).thenReturn(Optional.empty());

        HubDTO result = useCase.execute(hubId);

        assertNull(result);
    }

    @Test
    void execute_ShouldReturnEmptyInventoryList_WhenHubHasNoInventoryRows() {
        UUID hubId = UUID.randomUUID();

        Hub hub = Hub.builder().id(hubId).name("Hub B").build();
        HubDTO dto = HubDTO.builder().id(hubId).name("Hub B").build();

        when(hubRepository.findById(hubId)).thenReturn(Optional.of(hub));
        when(hubMapper.toDTO(hub)).thenReturn(dto);
        when(hubInventoryRepository.findAllByHubId(hubId)).thenReturn(List.of());

        HubDTO result = useCase.execute(hubId);

        assertNotNull(result);
        assertNotNull(result.getInventory());
        assertTrue(result.getInventory().isEmpty());
    }
}
