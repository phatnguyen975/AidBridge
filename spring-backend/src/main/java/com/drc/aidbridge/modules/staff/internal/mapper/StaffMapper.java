package com.drc.aidbridge.modules.staff.internal.mapper;

import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.staff.internal.entity.Staff;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

    public StaffDTO toDTO(Staff entity) {
        if (entity == null) return null;
        return StaffDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .startDate(entity.getStartDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Staff toEntity(StaffDTO dto) {
        if (dto == null) return null;
        return Staff.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .startDate(dto.getStartDate())
                .build();
    }
}
