package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateSosRequestUseCase {
    
    private final SosJpaRepository sosRequestRepository;
    private final UserFacade userFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final SosMapper sosMapper;
    private final SosSceneImageService sosSceneImageService;
    
    @Transactional
    public SosRequestResponse execute(UUID requesterId, CreateSosRequest createDto) {
        UserDTO requester = userFacade.getUserById(requesterId);
        String finalImageUrl = sosSceneImageService.resolveImageUrl(createDto.getImageUrl());
        
        // Create SOS request
        SosRequest sosRequest = SosRequest.builder()
                .requesterId(UUID.fromString(requester.getId()))
                .location(SosRequest.createPoint(createDto.getLat(), createDto.getLng()))
                .address(createDto.getAddress())
                .description(createDto.getDescription())
                .peopleCount(createDto.getPeopleCount() != null ? createDto.getPeopleCount() : 1)
                .urgencyLevel(createDto.getUrgencyLevel())
                .imageUrl(finalImageUrl)
                .status(SosStatus.PENDING)
                .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        // Publish event so mission module can create rescue mission
        eventPublisher.publishEvent(new SosRequestCreatedEvent(
                savedSos.getId(),
                savedSos.getLat(),
                savedSos.getLng()
        ));

        System.out.println("✅ END execute()");

        return sosMapper.toResponse(savedSos, null);
    }
}
