package com.drc.aidbridge.modules.aid.internal.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VoiceAidRequestResponse {
    private String rawTranscript;
    private String normalizedTranscript;
    private List<VoiceExtractedItem> extractedItems;
    private AidRequestResponse aidRequest;
}
