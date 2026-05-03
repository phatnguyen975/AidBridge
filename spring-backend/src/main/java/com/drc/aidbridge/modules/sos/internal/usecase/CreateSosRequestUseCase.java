package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.service.SosSceneImageService;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSosRequestUseCase {

    private static final long QUICK_SOS_DEDUPE_WINDOW_SECONDS = 30L;
    private static final double QUICK_SOS_DEDUPE_DISTANCE_METERS = 100.0d;

    private final SosJpaRepository sosRequestRepository;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final SosMapper sosMapper;
    private final SosSceneImageService sosSceneImageService;

    @Transactional
    public SosRequestResponse execute(UUID requesterId, CreateSosRequest createDto) {
        boolean quickSos = isQuickSos(createDto);
        Optional<SosRequest> existingByClientRequestId = findByClientRequestId(createDto.getClientRequestId());
        if (existingByClientRequestId.isPresent()) {
            log.info(
                "SMS_INGEST_DUPLICATE_CLIENT_REQUEST_ID clientRequestId={}",
                safeText(createDto.getClientRequestId())
            );
            return sosMapper.toResponse(existingByClientRequestId.get(), null);
        }

        UserDTO requester = userFacade.getUserById(requesterId);

        if (quickSos) {
            Optional<SosRequest> duplicate = findQuickSosDuplicate(requesterId, createDto);
            if (duplicate.isPresent()) {
                log.info(
                    "Suppressing duplicate quick SOS for requesterId={} clientRequestId={}",
                    requesterId,
                    safeText(createDto.getClientRequestId())
                );
                return sosMapper.toResponse(duplicate.get(), null);
            }
        }

        UrgencyLevel urgencyLevel = resolveUrgencyLevel(createDto, quickSos);
        String finalImageUrl = quickSos ? null : sosSceneImageService.resolveImageUrl(createDto.getImageUrl());

        SosRequest sosRequest = SosRequest.builder()
            .requesterId(requester.getId())
            .location(SosRequest.createPoint(createDto.getLat(), createDto.getLng()))
            .address(trimToNull(createDto.getAddress()))
            .description(quickSos ? null : trimToNull(createDto.getDescription()))
            .peopleCount(resolvePeopleCount(createDto, quickSos))
            .urgencyLevel(urgencyLevel)
            .imageUrl(finalImageUrl)
            .status(SosStatus.PENDING)
            .clientRequestId(trimToNull(createDto.getClientRequestId()))
            .source(firstNonBlank(createDto.getSource(), "APP"))
            .quickSos(quickSos)
            .accuracy(createDto.getAccuracy())
            .triggeredAt(createDto.getTriggeredAt())
            .locationCapturedAt(createDto.getLocationCapturedAt())
            .deviceInfo(trimToNull(createDto.getDeviceInfo()))
            .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        eventPublisher.publishEvent(new SosRequestCreatedEvent(
            savedSos.getId(),
            savedSos.getLat(),
            savedSos.getLng()
        ));
        log.info("SMS_INGEST_EVENT_PUBLISHED clientRequestId={}", safeText(createDto.getClientRequestId()));

        return sosMapper.toResponse(savedSos, null);
    }

    private Optional<SosRequest> findByClientRequestId(String clientRequestId) {
        String value = trimToNull(clientRequestId);
        if (value == null) {
            return Optional.empty();
        }
        Optional<SosRequest> existing = sosRequestRepository.findByClientRequestId(value);
        return existing != null ? existing : Optional.empty();
    }

    private boolean isQuickSos(CreateSosRequest request) {
        if (Boolean.TRUE.equals(request.getQuickSos())) {
            return true;
        }

        return isBlank(request.getDescription())
            && isBlank(request.getImageUrl())
            && (request.getPeopleCount() == null || request.getPeopleCount() <= 1);
    }

    private UrgencyLevel resolveUrgencyLevel(CreateSosRequest request, boolean quickSos) {
        if (request.getUrgencyLevel() != null) {
            return request.getUrgencyLevel();
        }
        if (quickSos) {
            return UrgencyLevel.CRITICAL;
        }
        throw new IllegalArgumentException("urgency_level is required");
    }

    private int resolvePeopleCount(CreateSosRequest request, boolean quickSos) {
        if (quickSos) {
            return 1;
        }
        return request.getPeopleCount() != null ? request.getPeopleCount() : 1;
    }

    private Optional<SosRequest> findQuickSosDuplicate(UUID requesterId, CreateSosRequest request) {
        if (request.getLat() == null || request.getLng() == null) {
            return Optional.empty();
        }

        Instant threshold = Instant.now().minusSeconds(QUICK_SOS_DEDUPE_WINDOW_SECONDS);
        List<SosRequest> recentRequests = sosRequestRepository
            .findByRequesterIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(requesterId, threshold);

        return recentRequests.stream()
            .filter(this::isActiveForDedupe)
            .filter(existing -> isWithinDistanceMeters(existing, request.getLat(), request.getLng()))
            .findFirst();
    }

    private boolean isActiveForDedupe(SosRequest sosRequest) {
        if (sosRequest == null || sosRequest.getStatus() == null) {
            return false;
        }

        return switch (sosRequest.getStatus()) {
            case PENDING, DISPATCHING, ASSIGNED, IN_PROGRESS -> true;
            case COMPLETED, CANCELLED -> false;
        };
    }

    private boolean isWithinDistanceMeters(SosRequest sosRequest, Double lat, Double lng) {
        if (sosRequest == null || sosRequest.getLocation() == null || lat == null || lng == null) {
            return false;
        }

        double distanceMeters = haversineMeters(
            sosRequest.getLocation().getY(),
            sosRequest.getLocation().getX(),
            lat,
            lng
        );
        return distanceMeters <= QUICK_SOS_DEDUPE_DISTANCE_METERS;
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusMeters = 6_371_000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0d) * Math.sin(dLat / 2.0d)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2.0d)
            * Math.sin(dLng / 2.0d);
        double c = 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
        return earthRadiusMeters * c;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    private String firstNonBlank(String first, String fallback) {
        String firstValue = trimToNull(first);
        return firstValue != null ? firstValue : fallback;
    }
}
