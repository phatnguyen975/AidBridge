package com.drc.aidbridge.modules.donation.internal.web.dto;

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
public class CreateDonationItemRequest {

    @NotNull(message = "itemCategoryId is required")
    private UUID itemCategoryId;
}
