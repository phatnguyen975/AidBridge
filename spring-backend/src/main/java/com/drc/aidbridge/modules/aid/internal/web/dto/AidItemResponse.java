package com.drc.aidbridge.modules.aid.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidItemResponse {

    private UUID id;
    private UUID aidRequestId;
    private UUID itemCategoryId;
    private Integer quantity;
    private String description;
    private Instant createdAt;
}
