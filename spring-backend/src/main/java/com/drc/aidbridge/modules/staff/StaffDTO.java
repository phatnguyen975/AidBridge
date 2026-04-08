package com.drc.aidbridge.modules.staff;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {
    private UUID id;
    private UUID userId;
    private LocalDate startDate;
    private Instant createdAt;
    private Instant updatedAt;
}
