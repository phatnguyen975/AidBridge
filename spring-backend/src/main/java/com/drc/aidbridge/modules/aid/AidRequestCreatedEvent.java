package com.drc.aidbridge.modules.aid;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class AidRequestCreatedEvent {
    private final UUID aidRequestId;
    private final BigDecimal lat;
    private final BigDecimal lng;
}
