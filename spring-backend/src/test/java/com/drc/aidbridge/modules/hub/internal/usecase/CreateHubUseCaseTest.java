package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubInventory;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubInventoryRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.HubInventoryElementRequest;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateHubUseCaseTest {

    private HubRepository hubRepository;
    private HubInventoryRepository hubInventoryRepository;
    private AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private HubMapper hubMapper;
    private CreateHubUseCase useCase;

    @BeforeEach
    void setUp() {
        hubRepository = mock(HubRepository.class);
        hubInventoryRepository = mock(HubInventoryRepository.class);
        aidItemCategoryJpaRepository = mock(AidItemCategoryJpaRepository.class);
        hubMapper = mock(HubMapper.class);
        useCase = new CreateHubUseCase(hubRepository, hubInventoryRepository, aidItemCategoryJpaRepository, hubMapper);
    }

    @Test
    void execute_ShouldCreateHub_WhenRequestValid() {
        CreateHubRequest request = CreateHubRequest.builder()
                .name("District 1 Hub")
                .address("1 Nguyen Hue, HCMC")
                .status(HubStatus.ACTIVE)
                .lat(BigDecimal.valueOf(10.775))
                .lng(BigDecimal.valueOf(106.700))
                .build();

        Hub hubEntity = Hub.builder().name("District 1 Hub").build();
        Hub saved = Hub.builder().id(UUID.randomUUID()).name("District 1 Hub").status(HubStatus.ACTIVE).build();
        HubDTO response = HubDTO.builder().id(saved.getId()).name("District 1 Hub").status(HubStatus.ACTIVE).build();

        when(hubMapper.toEntity(request)).thenReturn(hubEntity);
        when(hubRepository.save(hubEntity)).thenReturn(saved);
        when(hubMapper.toDTO(saved)).thenReturn(response);

        HubDTO result = useCase.execute(request);

        assertEquals(saved.getId(), result.getId());
        assertEquals("District 1 Hub", result.getName());
        assertEquals(HubStatus.ACTIVE, result.getStatus());
    }

    @Test
    void execute_ShouldThrow_WhenRequestNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
        assertEquals("request is null", ex.getMessage());
    }

    @Test
    void execute_ShouldInsertInventory_WhenElementsProvided() {
        UUID hubId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        CreateHubRequest request = CreateHubRequest.builder()
                .name("District 1 Hub")
                .lat(BigDecimal.valueOf(10.775))
                .lng(BigDecimal.valueOf(106.700))
                .elements(List.of(HubInventoryElementRequest.builder()
                        .itemCategoryId(categoryId)
                        .quantity(15)
                        .lowStockThreshold(5)
                        .build()))
                .build();

        Hub hubEntity = Hub.builder().name("District 1 Hub").build();
        Hub saved = Hub.builder().id(hubId).name("District 1 Hub").build();
        HubDTO response = HubDTO.builder().id(hubId).name("District 1 Hub").build();

        when(hubMapper.toEntity(request)).thenReturn(hubEntity);
        when(hubRepository.save(hubEntity)).thenReturn(saved);
        when(aidItemCategoryJpaRepository.existsById(categoryId)).thenReturn(true);
        when(hubInventoryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(hubMapper.toDTO(saved)).thenReturn(response);

        HubDTO result = useCase.execute(request);

        ArgumentCaptor<List<HubInventory>> captor = ArgumentCaptor.forClass(List.class);
        verify(hubInventoryRepository).saveAll(captor.capture());
        List<HubInventory> savedInventories = captor.getValue();

        assertEquals(1, savedInventories.size());
        assertEquals(hubId, savedInventories.getFirst().getHubId());
        assertEquals(categoryId, savedInventories.getFirst().getItemCategoryId());
        assertEquals(15, savedInventories.getFirst().getCurrentQuantity());
        assertEquals(5, savedInventories.getFirst().getLowStockThreshold());
        assertEquals(hubId, result.getId());
    }
}