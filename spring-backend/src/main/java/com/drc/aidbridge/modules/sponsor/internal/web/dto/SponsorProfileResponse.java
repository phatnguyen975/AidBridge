package com.drc.aidbridge.modules.sponsor.internal.web.dto;
import com.drc.aidbridge.modules.user.UserDTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AllArgsConstructor;


import com.drc.aidbridge.modules.sponsor.SponsorDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorProfileResponse {
    UserDTO user;
    SponsorDTO sponsor;
}
