package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Heartbeat payload: location update from volunteer mobile app
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PingVolunteerHeartbeatRequest {

    // Latitude (-90 to 90)
    @NotNull(message = "Latitude không được để trống")
    private Double lat;

    // Longitude (-180 to 180)
    @NotNull(message = "Longitude không được để trống")
    private Double lng;
}
