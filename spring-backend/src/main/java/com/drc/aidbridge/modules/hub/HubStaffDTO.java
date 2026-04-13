package com.drc.aidbridge.modules.hub;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubStaffDTO {
    private UUID id;
    private UUID hubId;
    private UUID userId;
    private boolean isAvailable;
    private Instant assignedAt;
    private Instant unassignedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
