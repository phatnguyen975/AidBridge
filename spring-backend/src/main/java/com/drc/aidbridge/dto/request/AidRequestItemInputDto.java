package com.drc.aidbridge.dto.request;

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
public class AidRequestItemInputDto {

    @NotNull
    private UUID itemCategoryId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String description;
}
