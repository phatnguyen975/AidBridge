package com.drc.aidbridge.modules.hub.internal.web.dto;

import com.drc.aidbridge.modules.shared.enums.HubStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHubRequest {

    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @Size(max = 500, message = "address must be at most 500 characters")
    private String address;

    @Size(max = 50, message = "phoneNumber must be at most 50 characters")
    private String phoneNumber;

    @Size(max = 500, message = "imageUrl must be at most 500 characters")
    private String imageUrl;

    private HubStatus status;

    @Size(max = 255, message = "operatingHours must be at most 255 characters")
    private String operatingHours;

    @DecimalMin(value = "-90.0", message = "lat must be >= -90")
    @DecimalMax(value = "90.0", message = "lat must be <= 90")
    @JsonAlias({"latitude"})
    private BigDecimal lat;

    @DecimalMin(value = "-180.0", message = "lng must be >= -180")
    @DecimalMax(value = "180.0", message = "lng must be <= 180")
    @JsonAlias({"longitude", "lon"})
    private BigDecimal lng;
}
