package com.drc.aidbridge.modules.sos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class SosRequestCreatedEvent {
    private final UUID sosRequestId;
    private final Double lat;
    private final Double lng;
}
