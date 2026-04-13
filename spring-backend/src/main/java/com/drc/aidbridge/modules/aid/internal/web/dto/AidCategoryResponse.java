package com.drc.aidbridge.modules.aid.internal.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Parent category response with nested child items for aid request form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AidCategoryResponse {

    private UUID id;
    private String name;
    private List<AidCategoryItemResponse> items;
}
