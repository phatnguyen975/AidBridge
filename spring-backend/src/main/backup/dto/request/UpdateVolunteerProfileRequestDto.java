package com.drc.aidbridge.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating volunteer profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVolunteerProfileRequestDto {

    private String vehicleType;
}
