package com.drc.aidbridge.modules.routing.internal.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Standard GeoJSON geometry structure supporting Polygon type with coordinates as [lng, lat] pairs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonGeometry {

    @NotNull(message = "Geometry type is required")
    @JsonProperty("type")
    private String type; // Only "Polygon" supported

    @NotNull(message = "Coordinates are required")
    @JsonProperty("coordinates")
    private List<List<List<Double>>> coordinates; // [[[lng, lat], ...], ...]
}
