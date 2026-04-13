package com.drc.aidbridge.modules.routing.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

/**
 * Request body for POST /api/routing/calculate with strategy support.
 * Supports single strategy or combined strategies with custom dangerous zones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRequest {

    @NotNull(message = "startLat is required")
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    private Double startLat;

    @NotNull(message = "startLon is required")
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    private Double startLon;

    @NotNull(message = "endLat is required")
    @Min(value = -90, message = "Latitude must be >= -90")
    @Max(value = 90, message = "Latitude must be <= 90")
    private Double endLat;

    @NotNull(message = "endLon is required")
    @Min(value = -180, message = "Longitude must be >= -180")
    @Max(value = 180, message = "Longitude must be <= 180")
    private Double endLon;

    @Builder.Default
    private String strategy = "urgent_response"; // Primary strategy: urgent_response, disaster_safe, heavy_aid, community_delivery, offroad_terrain

    @Valid
    @Builder.Default
    private List<DangerousZone> dangerousZones = List.of(); // Custom zones to avoid/bypass
}
