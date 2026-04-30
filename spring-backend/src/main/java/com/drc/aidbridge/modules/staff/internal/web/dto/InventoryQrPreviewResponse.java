package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.List;
import java.util.UUID;

public record InventoryQrPreviewResponse(
        String type,
        UUID donationId,
        String donationCode,
        UUID missionId,
        String missionCode,
        String status,
        UUID hubId,
        String hubName,
        List<InventoryQrPreviewItemDto> items,
        Boolean canConfirm,
        String message
) {
}
