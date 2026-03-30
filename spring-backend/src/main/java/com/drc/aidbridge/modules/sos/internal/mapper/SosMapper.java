package com.drc.aidbridge.modules.sos.internal.mapper;

import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.sos.SosDTO;
import com.drc.aidbridge.modules.sos.internal.entity.SosRequest;
import com.drc.aidbridge.modules.sos.internal.web.dto.SosRequestResponse;
import org.springframework.stereotype.Component;

@Component
public class SosMapper {

    public SosDTO toDTO(SosRequest entity) {
        return SosDTO.builder()
                .id(entity.getId())
                .requesterId(entity.getRequesterId())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .peopleCount(entity.getPeopleCount())
                .urgencyLevel(entity.getUrgencyLevel())
                .status(entity.getStatus())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SosRequestResponse toResponse(SosRequest entity, MissionDTO mission) {
        SosRequestResponse response = SosRequestResponse.builder()
                .id(entity.getId())
                .requesterId(entity.getRequesterId())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .peopleCount(entity.getPeopleCount())
                .urgencyLevel(entity.getUrgencyLevel())
                .status(entity.getStatus())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        if (mission != null) {
            response.setMissionId(mission.getId());
            response.setMissionType(mission.getMissionType());
            response.setMissionStatus(mission.getStatus());
        }

        return response;
    }
}
