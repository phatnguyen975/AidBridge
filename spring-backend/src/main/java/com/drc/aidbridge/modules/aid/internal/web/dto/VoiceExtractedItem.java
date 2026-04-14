package com.drc.aidbridge.modules.aid.internal.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VoiceExtractedItem {
    private String name;
    private Integer quantity;
    private UUID matchedCategoryId;
    private String matchedCategoryName;
    private boolean matched;
}
