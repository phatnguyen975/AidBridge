package com.drc.aidbridge.modules.hub.internal.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubInventoryElementRequest {

    @NotNull(message = "itemCategoryId is required")
    private UUID itemCategoryId;

    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be >= 0")
    private Integer quantity;

    @Min(value = 0, message = "lowStockThreshold must be >= 0")
    private Integer lowStockThreshold;
}
