package com.drc.aidbridge.modules.hub;

import java.util.UUID;

public record HubInventoryDTO(
        UUID id,
        UUID hubId,
        UUID itemCategoryId,
        Integer currentQuantity
) {
}
