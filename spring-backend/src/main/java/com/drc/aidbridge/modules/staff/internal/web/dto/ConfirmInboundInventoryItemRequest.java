package com.drc.aidbridge.modules.staff.internal.web.dto;

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
public class ConfirmInboundInventoryItemRequest {

    @NotNull(message = "parentCategoryId is required")
    private UUID parentCategoryId;

    @NotNull(message = "itemCategoryId is required")
    private UUID itemCategoryId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be > 0")
    private Integer quantity;

    private String note;
}
