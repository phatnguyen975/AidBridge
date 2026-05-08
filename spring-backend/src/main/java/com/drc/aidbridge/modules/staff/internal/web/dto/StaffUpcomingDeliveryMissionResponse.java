package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record StaffUpcomingDeliveryMissionResponse(
        UUID id,
        String missionCode,
        String volunteerName,
        String volunteerPhone
) {
}
