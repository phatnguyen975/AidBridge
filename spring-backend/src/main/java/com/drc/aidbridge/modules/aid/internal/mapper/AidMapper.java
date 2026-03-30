package com.drc.aidbridge.modules.aid.internal.mapper;

import com.drc.aidbridge.modules.aid.AidRequestDTO;
import com.drc.aidbridge.modules.mission.MissionDTO;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidItemInput;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidItemResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AidMapper {

    // Entity → Public module DTO (for cross-module communication)
    public AidRequestDTO toDTO(AidRequest entity) {
        return AidRequestDTO.builder()
                .id(entity.getId())
                .requesterId(entity.getRequesterId())
                .status(entity.getStatus())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .numberAdult(entity.getNumberAdult())
                .numberElderly(entity.getNumberElderly())
                .numberChildren(entity.getNumberChildren())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Entity + items + mission → API response DTO
    public AidRequestResponse toResponse(AidRequest entity, List<AidRequestItem> items, MissionDTO mission) {
        return AidRequestResponse.builder()
                .id(entity.getId())
                .requesterId(entity.getRequesterId())
                .sosRequestId(null)
                .status(entity.getStatus())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .numberAdult(entity.getNumberAdult())
                .numberElderly(entity.getNumberElderly())
                .numberChildren(entity.getNumberChildren())
                .urgencyLevel(null)
                .items(items.stream().map(this::toItemResponse).collect(Collectors.toList()))
                .missionId(mission != null ? mission.getId() : null)
                .missionType(mission != null ? mission.getMissionType() : null)
                .missionStatus(mission != null ? mission.getStatus() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Item input DTO → item entity
    public AidRequestItem toItemEntity(AidItemInput dto) {
        return AidRequestItem.builder()
                .itemCategoryId(dto.getItemCategoryId())
                .quantity(dto.getQuantity())
                .description(dto.getDescription())
                .build();
    }

    public AidItemResponse toItemResponse(AidRequestItem item) {
        return AidItemResponse.builder()
                .id(item.getId())
                .aidRequestId(item.getAidRequest().getId())
                .itemCategoryId(item.getItemCategoryId())
                .quantity(item.getQuantity())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
