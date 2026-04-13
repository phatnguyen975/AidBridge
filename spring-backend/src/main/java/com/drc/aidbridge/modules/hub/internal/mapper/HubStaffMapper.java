package com.drc.aidbridge.modules.hub.internal.mapper;

import com.drc.aidbridge.modules.hub.HubStaffDTO;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import org.springframework.stereotype.Component;

@Component
public class HubStaffMapper {

    public HubStaffDTO toDTO(HubStaff entity) {
        if (entity == null) return null;
        return HubStaffDTO.builder()
                .id(entity.getId())
                .hubId(entity.getHubId())
                .userId(entity.getUserId())
                .isAvailable(entity.isAvailable())
                .assignedAt(entity.getAssignedAt())
                .unassignedAt(entity.getUnassignedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public HubStaff toEntity(HubStaffDTO dto) {
        if (dto == null) return null;
        return HubStaff.builder()
                .id(dto.getId())
                .hubId(dto.getHubId())
                .userId(dto.getUserId())
                .isAvailable(dto.isAvailable())
                .assignedAt(dto.getAssignedAt())
                .unassignedAt(dto.getUnassignedAt())
                .build();
    }
}
