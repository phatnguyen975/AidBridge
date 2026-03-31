package com.drc.aidbridge.modules.sos.internal.usecase;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.mission.MissionFacade;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.mapper.SosMapper;
import com.drc.aidbridge.modules.sos.internal.repository.SosJpaRepository;
import com.drc.aidbridge.modules.sos.internal.web.dto.CreateSosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateSosRequestUseCase {
    
    private final SosJpaRepository sosRequestRepository;
    private final UserFacade userFacade;
    private final MissionFacade missionFacade;
    private final SosMapper sosMapper;
    
    @Transactional
    public SosRequestResponse execute(UUID requesterId, CreateSosRequest createDto) {
        UserDTO requester = userFacade.getUserById(requesterId);
        
        // Create SOS request
        SosRequest sosRequest = SosRequest.builder()
                .requesterId(UUID.fromString(requester.getId()))
                .lat(createDto.getLat())
                .lng(createDto.getLng())
                .address(createDto.getAddress())
                .description(createDto.getDescription())
                .peopleCount(createDto.getPeopleCount() != null ? createDto.getPeopleCount() : 1)
            .urgencyLevel(createDto.getUrgencyLevel() != null ? createDto.getUrgencyLevel() : UrgencyLevel.MEDIUM)
                .imageUrl(createDto.getImageUrl())
                .status(SosStatus.PENDING)
                .build();

        SosRequest savedSos = sosRequestRepository.save(sosRequest);

        // Interact with mission module
        MissionDTO savedMission = missionFacade.createRescueMission(
            savedSos.getId(), 
            BigDecimal.valueOf(savedSos.getLat()), 
            BigDecimal.valueOf(savedSos.getLng())
        );

        return sosMapper.toResponse(savedSos, savedMission);
    }
}
