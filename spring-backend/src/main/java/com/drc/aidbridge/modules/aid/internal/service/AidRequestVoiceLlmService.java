package com.drc.aidbridge.modules.aid.internal.service;

import java.util.List;

public interface AidRequestVoiceLlmService {

    ExtractionResult extractItems(String rawTranscript);

    record ExtractionResult(String normalizedTranscript, List<ExtractedItem> items) {
    }

    record ExtractedItem(String name, Integer quantity, String note) {
    }
}
