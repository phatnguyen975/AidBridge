package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.sos.SosRequestCreatedEvent;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.service.SosSceneImageService;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateGuestSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateGuestSosRequestUseCase {

    private final SosJpaRepository sosRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SosMapper sosMapper;
    private final SosSceneImageService sosSceneImageService;

    @Transactional
    public SosRequestResponse execute(CreateGuestSosRequest createDto) {
        boolean quickSos = isQuickSos(createDto);
        UrgencyLevel urgencyLevel = resolveUrgencyLevel(createDto, quickSos);
        String finalImageUrl = quickSos ? null : sosSceneImageService.resolveImageUrl(createDto.getImageUrl());

        SosRequest sosRequest = SosRequest.builder()
            .requesterId(null)
            .location(SosRequest.createPoint(createDto.getLat(), createDto.getLng()))
            .address(trimToNull(createDto.getAddress()))
            .description(quickSos ? null : trimToNull(createDto.getDescription()))
            .peopleCount(quickSos ? 1 : (createDto.getPeopleCount() != null ? createDto.getPeopleCount() : 1))
            .urgencyLevel(urgencyLevel)
            .imageUrl(finalImageUrl)
            .status(SosStatus.PENDING)
            .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        eventPublisher.publishEvent(new SosRequestCreatedEvent(
            savedSos.getId(),
            savedSos.getLat(),
            savedSos.getLng()
        ));

        return sosMapper.toResponse(savedSos, null);
    }

    private boolean isQuickSos(CreateGuestSosRequest request) {
        return isBlank(request.getDescription())
            && isBlank(request.getImageUrl())
            && (request.getPeopleCount() == null || request.getPeopleCount() <= 1);
    }

    private UrgencyLevel resolveUrgencyLevel(CreateGuestSosRequest request, boolean quickSos) {
        if (request.getUrgencyLevel() != null) {
            return request.getUrgencyLevel();
        }
        if (quickSos) {
            return UrgencyLevel.CRITICAL;
        }
        throw new IllegalArgumentException("urgency_level is required");
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
}
