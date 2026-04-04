package com.drc.aidbridge.modules.sos;

import java.time.Instant;
import java.util.UUID;

import com.drc.aidbridge.modules.shared.enums.SosStatus;
import com.drc.aidbridge.modules.shared.enums.UrgencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosDTO {
    private UUID id;
    private UUID requesterId;
    private BigDecimal lat;
    private BigDecimal lng;
    private String address;
    private String description;
    private Integer peopleCount;
    private UrgencyLevel urgencyLevel;
    private SosStatus status;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
