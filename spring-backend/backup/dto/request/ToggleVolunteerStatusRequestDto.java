package com.drc.aidbridge.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for toggling volunteer online status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleVolunteerStatusRequestDto {

    private boolean isOnline;

    private BigDecimal currentLat;

    private BigDecimal currentLng;
}
