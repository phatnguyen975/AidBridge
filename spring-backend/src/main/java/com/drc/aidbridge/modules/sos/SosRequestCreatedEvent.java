package com.drc.aidbridge.modules.sos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;
import java.math.BigDecimal;
@Getter
@Builder
@AllArgsConstructor
public class SosRequestCreatedEvent {
    private final UUID sosRequestId;
    private final BigDecimal lat;
    private final BigDecimal lng;
}
