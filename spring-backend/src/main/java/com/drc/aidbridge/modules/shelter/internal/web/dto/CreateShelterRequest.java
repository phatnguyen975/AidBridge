package com.drc.aidbridge.modules.shelter.internal.web.dto;

import lombok.*;


import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShelterRequest {
    private UUID hubId;


    private String name;

    private String address;


    private Integer maxCapacity;

    private Integer currentCapacity;

    private String phoneNumber;

    private String imageUrl;

    private BigDecimal lat;


    private BigDecimal lng;

    private Boolean isActive;
}
