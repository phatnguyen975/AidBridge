package com.drc.aidbridge.dto.request;

import com.drc.aidbridge.entity.enums.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSosRequestDto {

    @NotBlank
    @Size(max = 100)
    private String requesterName;

    @NotBlank
    @Size(max = 15)
    private String requesterPhone;

    @Size(max = 100)
    private String victimName;

    @Size(max = 15)
    private String victimPhone;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double victimLat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double victimLng;

    @Size(max = 255)
    private String victimAddress;

    private String description;

    @Min(1)
    private Integer peopleCount = 1;

    private Boolean isOnBehalf = false;

    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;

    @Size(max = 500)
    private String imageUrl;
}
