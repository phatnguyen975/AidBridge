package com.drc.aidbridge.modules.hub.internal.web.dto;

import jakarta.validation.Valid;
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
public class StockInHubInventoryRequest {

    @Valid
    @NotEmpty(message = "elements is required")
    private List<HubInventoryElementRequest> elements;
}
