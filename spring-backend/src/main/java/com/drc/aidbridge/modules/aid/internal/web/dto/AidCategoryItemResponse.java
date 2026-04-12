package com.drc.aidbridge.modules.aid.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Child item category response for aid request category tree.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidCategoryItemResponse {

    private UUID id;
    private String name;
    private String unit;
}
