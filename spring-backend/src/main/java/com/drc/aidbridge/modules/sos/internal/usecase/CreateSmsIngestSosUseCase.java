package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.web.dto.SmsIngestSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSmsIngestSosUseCase {

    private final SosJpaRepository sosRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SosMapper sosMapper;

    @Transactional
    public SosRequestResponse execute(SmsIngestSosRequest request) {
        String clientRequestId = trimToNull(request.getClientRequestId());
        Optional<SosRequest> existing = findByClientRequestId(clientRequestId);
        if (existing.isPresent()) {
            log.info("SMS_INGEST_DUPLICATE_CLIENT_REQUEST_ID clientRequestId={}", clientRequestId);
            return sosMapper.toResponse(existing.get(), null);
        }

        log.info("SMS_INGEST_CREATE_NEW clientRequestId={}", clientRequestId);
        SosRequest sosRequest = SosRequest.builder()
            .requesterId(null)
            .location(SosRequest.createPoint(request.getLatitude(), request.getLongitude()))
            .address(null)
            .description("Created from SMS fallback")
            .peopleCount(request.getPeopleCount() != null ? Math.max(1, request.getPeopleCount()) : 1)
            .urgencyLevel(UrgencyLevel.CRITICAL)
            .imageUrl(null)
            .status(SosStatus.PENDING)
            .clientRequestId(clientRequestId)
            .source("SMS")
            .quickSos(true)
            .accuracy(request.getAccuracy())
            .triggeredAt(toInstant(request.getTriggeredAtMillis()))
            .locationCapturedAt(toInstant(request.getLocationCapturedAtMillis()))
            .senderPhone(trimToNull(request.getSenderPhone()))
            .rawMessage(trimToNull(request.getRawMessage()))
            .receivedAtGatewayMillis(request.getReceivedAtGatewayMillis())
            .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        eventPublisher.publishEvent(new SosRequestCreatedEvent(
            savedSos.getId(),
            savedSos.getLat(),
            savedSos.getLng()
        ));
        log.info("SMS_INGEST_EVENT_PUBLISHED clientRequestId={}", clientRequestId);

        return sosMapper.toResponse(savedSos, null);
    }

    private Optional<SosRequest> findByClientRequestId(String clientRequestId) {
        if (clientRequestId == null) {
            return Optional.empty();
        }
        Optional<SosRequest> existing = sosRequestRepository.findByClientRequestId(clientRequestId);
        return existing != null ? existing : Optional.empty();
    }

    private Instant toInstant(Long timestampMillis) {
        if (timestampMillis == null || timestampMillis <= 0L) {
            return null;
        }
        return Instant.ofEpochMilli(timestampMillis);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
