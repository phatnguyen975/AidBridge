package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVolunteerLocationRequest {
    @NotNull
    private BigDecimal currentLat;
    @NotNull
    private BigDecimal currentLng;
}
