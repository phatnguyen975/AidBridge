package com.drc.aidbridge.modules.staff.internal.web.dto;

import lombok.*;


import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffRequest {

    private UUID userId;
    private LocalDate startDate;
}
