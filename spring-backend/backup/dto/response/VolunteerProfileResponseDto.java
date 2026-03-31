package com.drc.aidbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Volunteer profile response combining volunteer profile and user data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerProfileResponseDto {

    private VolunteerProfileDto profile;
    private UserDto user;
}
