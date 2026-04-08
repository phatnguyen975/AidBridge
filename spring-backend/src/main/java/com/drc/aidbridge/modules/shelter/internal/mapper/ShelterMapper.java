package com.drc.aidbridge.modules.shelter.internal.mapper;

import com.drc.aidbridge.modules.shelter.ShelterDTO;
import com.drc.aidbridge.modules.shelter.internal.entity.Shelter;
import com.drc.aidbridge.modules.shelter.internal.web.dto.CreateShelterRequest;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ShelterMapper {

    public ShelterDTO toDTO(Shelter entity) {
        if (entity == null) return null;

        Point p = entity.getLocation();
        ShelterDTO.LocationDTO location = null;
        if (p != null) {
            BigDecimal lat = BigDecimal.valueOf(p.getY());
            BigDecimal lng = BigDecimal.valueOf(p.getX());
            location = ShelterDTO.LocationDTO.builder().lat(lat).lng(lng).build();
        }

        return ShelterDTO.builder()
                .id(entity.getId())
                .hubId(entity.getHubId())
                .name(entity.getName())
                .address(entity.getAddress())
                .maxCapacity(entity.getMaxCapacity())
                .currentCapacity(entity.getCurrentCapacity())
                .phoneNumber(entity.getPhoneNumber())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .location(location)
                .build();
    }

    public Shelter toEntity(CreateShelterRequest req) {
        if (req == null) return null;
        Shelter.ShelterBuilder builder = Shelter.builder()
                .hubId(req.getHubId())
                .name(req.getName())
                .address(req.getAddress())
                .maxCapacity(req.getMaxCapacity())
                .currentCapacity(req.getCurrentCapacity() == null ? 0 : req.getCurrentCapacity())
                .phoneNumber(req.getPhoneNumber())
                .imageUrl(req.getImageUrl())
                .isActive(req.getIsActive() == null ? true : req.getIsActive());

        if (req.getLat() != null && req.getLng() != null) {
            builder.location(Shelter.createPoint(req.getLat(), req.getLng()));
        }

        return builder.build();
    }
}
