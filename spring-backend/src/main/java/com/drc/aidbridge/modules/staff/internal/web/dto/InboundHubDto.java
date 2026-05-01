package com.drc.aidbridge.modules.staff.internal.web.dto;

import java.util.UUID;

public record InboundHubDto(
        UUID id,
        String name,
        String address
) {
}
