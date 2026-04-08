package com.drc.aidbridge.modules.hub.internal.mapper;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HubMapper {

    public HubDTO toDTO(Hub entity) {
        if (entity == null) return null;

        Point p = entity.getLocation();
        HubDTO.LocationDTO location = null;
        if (p != null) {
            BigDecimal lat = BigDecimal.valueOf(p.getY());
            BigDecimal lng = BigDecimal.valueOf(p.getX());
            location = HubDTO.LocationDTO.builder().lat(lat).lng(lng).build();
        }

        return HubDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(entity.getAddress())
                .phoneNumber(entity.getPhoneNumber())
                .imageUrl(entity.getImageUrl())
                .status(entity.getStatus())
                .operatingHours(entity.getOperatingHours())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .location(location)
                .build();
    }
}
