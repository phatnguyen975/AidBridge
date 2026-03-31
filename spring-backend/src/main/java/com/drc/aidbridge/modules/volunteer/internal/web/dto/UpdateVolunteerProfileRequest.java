package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVolunteerProfileRequest {
    private String vehicleType;
}
