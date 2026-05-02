package com.drc.aidbridge.modules.staff.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateInboundSubCategoryRequest {

    @NotNull(message = "donationId is required")
    private UUID donationId;

    @NotNull(message = "parentCategoryId is required")
    private UUID parentCategoryId;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "unit is required")
    private String unit;

    private String iconUrl;
}
