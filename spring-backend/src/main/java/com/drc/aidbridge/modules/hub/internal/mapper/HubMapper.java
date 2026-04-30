package com.drc.aidbridge.modules.hub.internal.mapper;

import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.web.dto.CreateHubRequest;
import com.drc.aidbridge.modules.hub.internal.web.dto.UpdateHubRequest;
import com.drc.aidbridge.modules.shared.enums.HubStatus;
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
                .latitude(p != null ? p.getY() : null)
                .longitude(p != null ? p.getX() : null)
                .build();
    }

    public HubDTO toDTO(com.drc.aidbridge.modules.hub.internal.repository.projection.HubSearchResultProjection projection) {
        if (projection == null) return null;

        HubDTO.LocationDTO location = HubDTO.LocationDTO.builder()
                .lat(projection.getLatitude() != null ? BigDecimal.valueOf(projection.getLatitude()) : null)
                .lng(projection.getLongitude() != null ? BigDecimal.valueOf(projection.getLongitude()) : null)
                .build();

        HubStatus status = null;
        try {
            if (projection.getStatus() != null) {
                status = HubStatus.valueOf(projection.getStatus());
            }
        } catch (IllegalArgumentException ignored) {}

        return HubDTO.builder()
                .id(projection.getId())
                .name(projection.getName())
                .address(projection.getAddress())
                .phoneNumber(projection.getPhoneNumber())
                .imageUrl(projection.getImageUrl())
                .status(status)
                .operatingHours(projection.getOperatingHours())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .location(location)
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .distanceInMeters(projection.getDistanceInMeters())
                .build();
    }

    public Hub toEntity(CreateHubRequest request) {
        if (request == null) {
            return null;
        }

        HubStatus status = request.getStatus() != null ? request.getStatus() : HubStatus.ACTIVE;
        Point location = Hub.createPoint(request.getLat().doubleValue(), request.getLng().doubleValue());

        return Hub.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .imageUrl(request.getImageUrl())
                .status(status)
                .operatingHours(request.getOperatingHours())
                .location(location)
                .build();
    }

    public void patchEntity(Hub entity, UpdateHubRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getAddress() != null) {
            entity.setAddress(request.getAddress());
        }
        if (request.getPhoneNumber() != null) {
            entity.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getImageUrl() != null) {
            entity.setImageUrl(request.getImageUrl());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getOperatingHours() != null) {
            entity.setOperatingHours(request.getOperatingHours());
        }
        if (request.getLat() != null || request.getLng() != null) {
            Point currentLocation = entity.getLocation();
            if (currentLocation == null && (request.getLat() == null || request.getLng() == null)) {
                throw new IllegalArgumentException("Both lat and lng are required when hub has no location");
            }
            double lat = request.getLat() != null
                    ? request.getLat().doubleValue()
                    : (currentLocation != null ? currentLocation.getY() : 0.0d);
            double lng = request.getLng() != null
                    ? request.getLng().doubleValue()
                    : (currentLocation != null ? currentLocation.getX() : 0.0d);
            entity.setLocation(Hub.createPoint(lat, lng));
        }
    }
}
