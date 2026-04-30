package com.drc.aidbridge.modules.admin.internal.web.dto;

import java.util.UUID;

public record AdminStaffResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phoneNumber,
        UUID hubId,
        String hubName
) {
}
