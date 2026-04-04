package com.drc.aidbridge.modules.volunteer.internal.mapper;

import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerProfileInfo;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerProfileResponse;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VolunteerMapper {

    public VolunteerDTO toDTO(Volunteer entity) {
        Point p = entity.getCurrentLocation();
        VolunteerDTO.LocationDTO location = null;
        if (p != null) {
            BigDecimal lat = BigDecimal.valueOf(p.getY());
            BigDecimal lng = BigDecimal.valueOf(p.getX());
            location = new VolunteerDTO.LocationDTO(lat, lng);
        }

        return VolunteerDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .isOnline(entity.isOnline())
                .currentLocation(location)
                .vehicleType(entity.getVehicleType() != null ? entity.getVehicleType().name() : null)
                .totalTasksCompleted(entity.getTotalTasksCompleted())
                .avgRating(entity.getAvgRating())
                .avgResponseSeconds(entity.getAvgResponseSeconds())
                .build();
    }

    public VolunteerProfileInfo toProfileInfo(Volunteer entity) {
        Point p = entity.getCurrentLocation();
        BigDecimal lat = null;
        BigDecimal lng = null;
        if (p != null) {
            lat = BigDecimal.valueOf(p.getY());
            lng = BigDecimal.valueOf(p.getX());
        }

        return VolunteerProfileInfo.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .isOnline(entity.isOnline())
                .currentLat(lat)
                .currentLng(lng)
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
