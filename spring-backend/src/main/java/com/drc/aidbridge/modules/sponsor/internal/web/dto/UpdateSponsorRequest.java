package com.drc.aidbridge.modules.sponsor.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSponsorRequest {
    private String organizationName;
    private String organizationType;
}
