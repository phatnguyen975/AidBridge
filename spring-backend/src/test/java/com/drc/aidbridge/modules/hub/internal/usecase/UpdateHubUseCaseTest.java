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
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateHubUseCaseTest {

    private HubRepository hubRepository;
    private HubInventoryRepository hubInventoryRepository;
    private AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private HubMapper hubMapper;
    private UpdateHubUseCase useCase;

    @BeforeEach
    void setUp() {
        hubRepository = mock(HubRepository.class);
        hubInventoryRepository = mock(HubInventoryRepository.class);
        aidItemCategoryJpaRepository = mock(AidItemCategoryJpaRepository.class);
        hubMapper = mock(HubMapper.class);
        useCase = new UpdateHubUseCase(hubRepository, hubInventoryRepository, aidItemCategoryJpaRepository, hubMapper);
    }

    @Test
    void execute_ShouldUpdateHub_WhenHubExists() {
        UUID id = UUID.randomUUID();
        Hub existing = Hub.builder().id(id).name("Old Name").status(HubStatus.ACTIVE).build();
        UpdateHubRequest request = UpdateHubRequest.builder().name("New Name").status(HubStatus.INACTIVE).build();
        HubDTO response = HubDTO.builder().id(id).name("New Name").status(HubStatus.INACTIVE).build();

        when(hubRepository.findById(id)).thenReturn(Optional.of(existing));
        when(hubRepository.save(existing)).thenReturn(existing);
        when(hubMapper.toDTO(existing)).thenReturn(response);

        HubDTO result = useCase.execute(id, request);

        verify(hubMapper).patchEntity(existing, request);
        assertEquals("New Name", result.getName());
        assertEquals(HubStatus.INACTIVE, result.getStatus());
    }

    @Test
    void execute_ShouldThrow_WhenHubNotFound() {
        UUID id = UUID.randomUUID();
        UpdateHubRequest request = UpdateHubRequest.builder().name("x").build();
        when(hubRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(id, request));
    }

    @Test
    void execute_ShouldThrow_WhenRequestNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.randomUUID(), null));
        assertEquals("request is null", ex.getMessage());
    }

        @Test
        void execute_ShouldAddQuantity_WhenInventoryExists() {
        UUID hubId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Hub existingHub = Hub.builder().id(hubId).name("Hub A").build();
        HubInventory inventory = HubInventory.builder()
            .id(UUID.randomUUID())
            .hubId(hubId)
            .itemCategoryId(categoryId)
            .currentQuantity(10)
            .lowStockThreshold(3)
            .build();

        UpdateHubRequest request = UpdateHubRequest.builder()
            .elements(List.of(HubInventoryElementRequest.builder()
                .itemCategoryId(categoryId)
                .quantity(7)
                .lowStockThreshold(4)
                .build()))
            .build();

        HubDTO response = HubDTO.builder().id(hubId).name("Hub A").build();

        when(hubRepository.findById(hubId)).thenReturn(Optional.of(existingHub));
        when(hubRepository.save(existingHub)).thenReturn(existingHub);
        when(aidItemCategoryJpaRepository.existsById(categoryId)).thenReturn(true);
        when(hubInventoryRepository.findByHubIdAndItemCategoryId(hubId, categoryId)).thenReturn(Optional.of(inventory));
        when(hubMapper.toDTO(existingHub)).thenReturn(response);

        useCase.execute(hubId, request);

        verify(hubInventoryRepository).save(inventory);
        assertEquals(17, inventory.getCurrentQuantity());
        assertEquals(4, inventory.getLowStockThreshold());
        }
}