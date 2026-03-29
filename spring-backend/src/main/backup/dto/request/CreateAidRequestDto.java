package com.drc.aidbridge.dto.request;

import com.drc.aidbridge.entity.enums.UrgencyLevel;

import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAidRequestDto {

    private UUID sosRequestId;
    
    @NotNull
    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @NotNull
    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    private String address;

    @Min(0)
    @Builder.Default
    private Integer adultsCount = 0;

    @Min(0)
    @Builder.Default
    private Integer elderlyCount = 0;

    @Min(0)
    @Builder.Default
    private Integer childrenCount = 0;

    private String notes;

    private UrgencyLevel urgencyLevel;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<AidRequestItemInputDto> items;
}
