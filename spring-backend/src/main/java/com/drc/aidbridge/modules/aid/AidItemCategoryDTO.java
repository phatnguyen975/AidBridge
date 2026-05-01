package com.drc.aidbridge.modules.aid;

import java.util.UUID;

public record AidItemCategoryDTO(
        UUID id,
        String name,
        UUID parentId,
        String unit,
        boolean isLeaf
) {
}
