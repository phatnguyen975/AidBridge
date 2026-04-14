package com.drc.aidbridge.modules.aid.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAidRequestVoiceInput {

    @NotNull
    private BigDecimal lat;

    @NotNull
    private BigDecimal lng;

    private String address;

    @Min(0)
    private Integer adultsCount = 0;

    @Min(0)
    private Integer elderlyCount = 0;

    @Min(0)
    private Integer childrenCount = 0;

    private String notes;

    private UrgencyLevel urgencyLevel;
}
