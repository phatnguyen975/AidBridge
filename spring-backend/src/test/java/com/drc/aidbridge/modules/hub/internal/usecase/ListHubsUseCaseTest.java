package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListHubsUseCaseTest {

    private HubRepository hubRepository;
    private HubMapper hubMapper;
    private ListHubsUseCase useCase;

    @BeforeEach
    void setUp() {
        hubRepository = mock(HubRepository.class);
        hubMapper = mock(HubMapper.class);
        useCase = new ListHubsUseCase(hubRepository, hubMapper);
    }

    @Test
    void execute_ShouldListAll_WhenStatusNull() {
        Hub active = Hub.builder().id(UUID.randomUUID()).name("A").status(HubStatus.ACTIVE).build();
        Hub inactive = Hub.builder().id(UUID.randomUUID()).name("B").status(HubStatus.INACTIVE).build();

        when(hubRepository.searchHubs(null, null)).thenReturn(List.of(active, inactive));
        when(hubMapper.toDTO(active)).thenReturn(HubDTO.builder().id(active.getId()).name("A").build());
        when(hubMapper.toDTO(inactive)).thenReturn(HubDTO.builder().id(inactive.getId()).name("B").build());

        List<HubDTO> result = useCase.execute(null);

        assertEquals(2, result.size());
    }

    @Test
    void execute_ShouldFilterByStatus_WhenStatusProvided() {
        Hub active = Hub.builder().id(UUID.randomUUID()).name("A").status(HubStatus.ACTIVE).build();

        when(hubRepository.searchHubs(HubStatus.ACTIVE, null)).thenReturn(List.of(active));
        when(hubMapper.toDTO(active)).thenReturn(HubDTO.builder().id(active.getId()).name("A").build());

        List<HubDTO> result = useCase.execute(HubStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getName());
    }

    @Test
    void execute_ShouldSearchByKeyword_WhenKeywordProvided() {
        Hub hub = Hub.builder().id(UUID.randomUUID()).name("Quan 1").status(HubStatus.ACTIVE).build();

        when(hubRepository.searchHubs(null, "quan 1")).thenReturn(List.of(hub));
        when(hubMapper.toDTO(hub)).thenReturn(HubDTO.builder().id(hub.getId()).name("Quan 1").build());

        List<HubDTO> result = useCase.execute(null, "  quan 1  ");

        assertEquals(1, result.size());
        assertEquals("Quan 1", result.get(0).getName());
    }
}
