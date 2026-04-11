package com.drc.aidbridge.modules.hub.internal.usecase;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.mapper.HubMapper;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private HubMapper hubMapper;
    private UpdateHubUseCase useCase;

    @BeforeEach
    void setUp() {
        hubRepository = mock(HubRepository.class);
        hubMapper = mock(HubMapper.class);
        useCase = new UpdateHubUseCase(hubRepository, hubMapper);
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
}