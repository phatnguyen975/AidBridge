package com.drc.aidbridge.modules.volunteer.internal.web.dto;

import com.drc.aidbridge.modules.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerProfileResponse {
    private VolunteerProfileInfo profile;
    private UserDTO user;
}
