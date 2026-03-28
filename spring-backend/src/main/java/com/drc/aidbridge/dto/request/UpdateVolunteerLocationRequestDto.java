package com.drc.aidbridge.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating volunteer current location.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVolunteerLocationRequestDto {

    private BigDecimal currentLat;

    private BigDecimal currentLng;
}
