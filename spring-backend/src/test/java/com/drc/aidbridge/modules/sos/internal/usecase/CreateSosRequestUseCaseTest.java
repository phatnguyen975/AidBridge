package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.service.SosSceneImageService;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateGuestSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateSosRequestUseCaseTest {

    private SosJpaRepository sosJpaRepository;
    private UserFacade userFacade;
    private ApplicationEventPublisher eventPublisher;
    private SosMapper sosMapper;
    private SosSceneImageService sosSceneImageService;

    private CreateSosRequestUseCase createSosRequestUseCase;
    private CreateGuestSosRequestUseCase createGuestSosRequestUseCase;

    @BeforeEach
    void setUp() {
        sosJpaRepository = mock(SosJpaRepository.class);
        userFacade = mock(UserFacade.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        sosMapper = mock(SosMapper.class);
        sosSceneImageService = mock(SosSceneImageService.class);

        createSosRequestUseCase = new CreateSosRequestUseCase(
                sosJpaRepository,
                userFacade,
                eventPublisher,
                sosMapper,
                sosSceneImageService
        );

        createGuestSosRequestUseCase = new CreateGuestSosRequestUseCase(
                sosJpaRepository,
                eventPublisher,
                sosMapper,
                sosSceneImageService
        );

        when(sosSceneImageService.resolveImageUrl(any())).thenReturn("https://res.cloudinary.com/demo/image/upload/sos.jpg");
        when(sosMapper.toResponse(any(), isNull())).thenReturn(SosRequestResponse.builder().build());
        when(sosJpaRepository.save(any(SosRequest.class))).thenAnswer(invocation -> {
            SosRequest request = invocation.getArgument(0);
            if (request.getId() == null) {
                request.setId(UUID.randomUUID());
            }
            if (request.getStatus() == null) {
                request.setStatus(SosStatus.PENDING);
            }
            return request;
        });
    }

    @Test
    void execute_ShouldPersistCriticalUrgency_ForAuthenticatedRequest() {
        UUID requesterId = UUID.randomUUID();
        when(userFacade.getUserById(requesterId)).thenReturn(UserDTO.builder().id(requesterId.toString()).build());

        CreateSosRequest request = CreateSosRequest.builder()
                .lat(10.123)
                .lng(106.456)
                .address("Address")
                .description("Description")
                .peopleCount(2)
                .urgencyLevel(UrgencyLevel.CRITICAL)
                .imageUrl("https://example.com/scene.jpg")
                .build();

        createSosRequestUseCase.execute(requesterId, request);

        ArgumentCaptor<SosRequest> captor = ArgumentCaptor.forClass(SosRequest.class);
        org.mockito.Mockito.verify(sosJpaRepository).save(captor.capture());
        assertEquals(UrgencyLevel.CRITICAL, captor.getValue().getUrgencyLevel());
    }

    @Test
    void execute_ShouldPersistCriticalUrgency_ForGuestRequest() {
        CreateGuestSosRequest request = CreateGuestSosRequest.builder()
                .lat(10.123)
                .lng(106.456)
                .address("Address")
                .description("Description")
                .peopleCount(2)
                .urgencyLevel(UrgencyLevel.CRITICAL)
                .imageUrl("https://example.com/scene.jpg")
                .build();

        createGuestSosRequestUseCase.execute(request);

        ArgumentCaptor<SosRequest> captor = ArgumentCaptor.forClass(SosRequest.class);
        org.mockito.Mockito.verify(sosJpaRepository).save(captor.capture());
        assertEquals(UrgencyLevel.CRITICAL, captor.getValue().getUrgencyLevel());
    }
}
