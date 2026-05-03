package com.drc.aidbridge.modules.staff.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmInboundInventoryRequest {

    private UUID donationId;

    private String code;

    @NotEmpty(message = "Inbound items cannot be empty")
    @Valid
    private List<ConfirmInboundInventoryItemRequest> items;

    private String generalNote;
}
