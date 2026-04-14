package com.drc.aidbridge.modules.donation.internal.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateDonationRequest {

    @NotNull(message = "hubId is required")
    private UUID hubId;

    private String notes;

    @NotEmpty(message = "items is required")
    @Valid
    private List<CreateDonationItemRequest> items;
}
