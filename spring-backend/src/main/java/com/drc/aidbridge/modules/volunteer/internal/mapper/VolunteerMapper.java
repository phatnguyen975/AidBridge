package com.drc.aidbridge.modules.volunteer.internal.mapper;

import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerProfileInfo;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class VolunteerMapper {

    public VolunteerDTO toDTO(Volunteer entity) {
        return VolunteerDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .isOnline(entity.isOnline())
                .currentLat(entity.getCurrentLat())
                .currentLng(entity.getCurrentLng())
                .vehicleType(entity.getVehicleType() != null ? entity.getVehicleType().name() : null)
                .totalTasksCompleted(entity.getTotalTasksCompleted())
                .avgRating(entity.getAvgRating())
                .avgResponseSeconds(entity.getAvgResponseSeconds())
                .build();
    }

    public VolunteerProfileInfo toProfileInfo(Volunteer entity) {
        return VolunteerProfileInfo.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .isOnline(entity.isOnline())
                .currentLat(entity.getCurrentLat())
                .currentLng(entity.getCurrentLng())
                .vehicleType(entity.getVehicleType() != null ? entity.getVehicleType().name() : null)
                .totalTasksCompleted(entity.getTotalTasksCompleted())
                .avgRating(entity.getAvgRating())
                .avgResponseSeconds(entity.getAvgResponseSeconds())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public VolunteerProfileResponse toResponse(Volunteer entity, UserDTO userDTO) {
        return VolunteerProfileResponse.builder()
                .profile(toProfileInfo(entity))
                .user(userDTO)
                .build();
    }
}
