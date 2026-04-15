package com.drc.aidbridge.modules.donation.internal.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDonationItemRequest {

    @NotBlank(message = "itemName is required")
    @Size(max = 255, message = "itemName must be at most 255 characters")
    private String itemName;

    private UUID itemCategoryId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be > 0")
    private Integer quantity;

    @Size(max = 100, message = "unit must be at most 100 characters")
    private String unit;

    private String description;

    private LocalDate expiryDate;

    @Size(max = 500, message = "imageUrl must be at most 500 characters")
    private String imageUrl;
}
