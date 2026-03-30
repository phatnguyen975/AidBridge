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
public class ToggleVolunteerStatusRequest {
    @NotNull
    private Boolean isOnline;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    
    public boolean isOnline() {
        return Boolean.TRUE.equals(isOnline);
    }
}
