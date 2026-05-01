package com.drc.aidbridge.modules.routing.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DangerousZoneCreateRequest {
    @NotNull(message = "Name is required")
    private String name;
    
    @Valid
    @NotNull(message = "Geometry is required")
    private GeoJsonGeometry geometry;
    
    private UUID adminId;
}
