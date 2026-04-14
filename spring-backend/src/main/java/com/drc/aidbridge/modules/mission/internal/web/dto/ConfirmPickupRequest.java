package com.drc.aidbridge.modules.mission.internal.web.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class ConfirmPickupRequest {
    // Empty body - confirmation is done via mission ID in path
}
