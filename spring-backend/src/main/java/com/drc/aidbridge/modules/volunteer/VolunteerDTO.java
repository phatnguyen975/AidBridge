package com.drc.aidbridge.modules.volunteer;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerDTO {
    private UUID id;
    private UUID userId;
    private boolean isOnline;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private String vehicleType;
    private Integer totalTasksCompleted;
    private BigDecimal avgRating;
    private Integer avgResponseSeconds;
}
