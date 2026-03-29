package com.drc.aidbridge.modules.aid;

import com.drc.aidbridge.entity.enums.AidStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidRequestDTO {

    private UUID id;
    private UUID requesterId;
    private AidStatus status;
    private BigDecimal lat;
    private BigDecimal lng;
    private String address;
    private String description;
    private Integer numberAdult;
    private Integer numberElderly;
    private Integer numberChildren;
    private Instant createdAt;
    private Instant updatedAt;
}
