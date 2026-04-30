package com.drc.aidbridge.modules.staff.internal.mapper;

import com.drc.aidbridge.modules.staff.StaffDTO;
import com.drc.aidbridge.modules.user.internal.entity.User;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

    public StaffDTO toDTO(User user) {
        if (user == null) return null;
        return StaffDTO.builder()
                .id(user.getId())
                .userId(user.getId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
