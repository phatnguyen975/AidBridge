package com.drc.aidbridge.modules.staff.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOutboundInventoryRequest {

    @NotBlank(message = "code is required")
    private String code;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<ConfirmInventoryItemRequest> items;

    private String note;
}
