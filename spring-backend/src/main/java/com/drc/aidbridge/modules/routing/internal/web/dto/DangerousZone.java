package com.drc.aidbridge.modules.routing.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a dangerous area to avoid or reduce speed in routing calculations using GeoJSON polygon.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DangerousZone {

    @NotNull(message = "Zone name is required")
    private String name;

    @Builder.Default
    private Double priority = 0.0; // Priority multiplier: 0.0 (bypass) to 1.0 (normal)

    @Valid
    @NotNull(message = "Zone geometry is required")
    private GeoJsonGeometry geometry; // GeoJSON polygon definition
}
