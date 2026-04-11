package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateHubUseCaseTest {

    private HubRepository hubRepository;
    private HubMapper hubMapper;
    private CreateHubUseCase useCase;

    @BeforeEach
    void setUp() {
        hubRepository = mock(HubRepository.class);
        hubMapper = mock(HubMapper.class);
        useCase = new CreateHubUseCase(hubRepository, hubMapper);
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
}