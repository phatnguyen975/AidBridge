package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.web.dto.SmsIngestSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateSmsIngestSosUseCaseTest {

    private SosJpaRepository sosJpaRepository;
    private ApplicationEventPublisher eventPublisher;
    private CreateSmsIngestSosUseCase useCase;

    @BeforeEach
    void setUp() {
        sosJpaRepository = mock(SosJpaRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        useCase = new CreateSmsIngestSosUseCase(sosJpaRepository, eventPublisher, new SosMapper());
    }

    @Test
    void execute_ShouldCreateSmsSourceSosAndPublishEvent() {
        when(sosJpaRepository.findByClientRequestId("TEST-123")).thenReturn(Optional.empty());
        when(sosJpaRepository.save(any(SosRequest.class))).thenAnswer(invocation -> {
            SosRequest request = invocation.getArgument(0);
            request.setId(UUID.randomUUID());
            request.setStatus(SosStatus.PENDING);
            return request;
        });

        SosRequestResponse response = useCase.execute(sampleRequest());

        ArgumentCaptor<SosRequest> requestCaptor = ArgumentCaptor.forClass(SosRequest.class);
        verify(sosJpaRepository).save(requestCaptor.capture());
        SosRequest saved = requestCaptor.getValue();
        assertEquals("TEST-123", saved.getClientRequestId());
        assertEquals("SMS", saved.getSource());
        assertEquals(Boolean.TRUE, saved.getQuickSos());
        assertEquals(UrgencyLevel.CRITICAL, saved.getUrgencyLevel());
        assertEquals(1, saved.getPeopleCount());
        assertEquals(12.5, saved.getAccuracy());
        assertEquals("0901234567", saved.getSenderPhone());
        assertNotNull(saved.getTriggeredAt());

        verify(eventPublisher).publishEvent(any(SosRequestCreatedEvent.class));
        assertEquals("TEST-123", response.getClientRequestId());
        assertEquals("SMS", response.getSource());
    }

    @Test
    void execute_ShouldReturnExistingSosWithoutPublishingEvent_WhenClientRequestIdExists() {
        SosRequest existing = SosRequest.builder()
            .id(UUID.randomUUID())
            .location(SosRequest.createPoint(10.762622, 106.660172))
            .status(SosStatus.PENDING)
            .urgencyLevel(UrgencyLevel.CRITICAL)
            .peopleCount(1)
            .clientRequestId("TEST-123")
            .source("SMS")
            .quickSos(true)
            .build();

        when(sosJpaRepository.findByClientRequestId("TEST-123")).thenReturn(Optional.of(existing));

        SosRequestResponse response = useCase.execute(sampleRequest());

        verify(sosJpaRepository, never()).save(any(SosRequest.class));
        verify(eventPublisher, never()).publishEvent(any());
        assertEquals(existing.getId(), response.getId());
        assertEquals("TEST-123", response.getClientRequestId());
    }

    private SmsIngestSosRequest sampleRequest() {
        return SmsIngestSosRequest.builder()
            .clientRequestId("TEST-123")
            .senderPhone("0901234567")
            .latitude(10.762622)
            .longitude(106.660172)
            .accuracy(12.5)
            .triggeredAtMillis(1710000000000L)
            .locationCapturedAtMillis(1710000000000L)
            .peopleCount(1)
            .quickSos(true)
            .rawMessage("AIDBRIDGE_SOS|id=TEST-123|lat=10.762622|lng=106.660172")
            .receivedAtGatewayMillis(1710000000100L)
            .build();
    }
}
