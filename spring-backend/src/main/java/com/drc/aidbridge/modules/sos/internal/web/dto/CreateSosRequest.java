package com.drc.aidbridge.modules.sos.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSosRequest {
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @JsonProperty("lat")
    private Double lat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @JsonProperty("lng")
    private Double lng;

    @Size(max = 500)
    @JsonProperty("address")
    private String address;

    @JsonProperty("description")
    private String description;

    @Min(value = 1)
    @Builder.Default
    @JsonProperty("people_count")
    @JsonAlias("peopleCount")
    private Integer peopleCount = 1;

    @NotNull(message = "urgency_level is required")
    @JsonProperty("urgency_level")
    @JsonAlias("urgencyLevel")
    private UrgencyLevel urgencyLevel;

    @JsonProperty("image_url")
    @JsonAlias("imageUrl")
    private String imageUrl;
}
