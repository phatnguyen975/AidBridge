package com.drc.aidbridge.dto.request;

import com.drc.aidbridge.entity.enums.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGuestSosRequestDto {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double lng;

    @Size(max = 500)
    private String address;

    private String description;

    @Min(value = 1)
    @Builder.Default
    private Integer peopleCount = 1;

    @Builder.Default
    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;

    @Size(max = 500)
    private String imageUrl;
}
