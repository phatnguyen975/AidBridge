package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.service.AidRequestVoiceLlmService;
import com.drc.aidbridge.modules.aid.internal.service.SpeechToTextService;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidItemInput;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequest;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequestVoiceInput;
import com.drc.aidbridge.modules.aid.internal.web.dto.VoiceAidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.VoiceExtractedItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TranscribeAidRequestVoiceUseCase {

    private final SpeechToTextService speechToTextService;
    private final AidRequestVoiceLlmService aidRequestVoiceLlmService;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private final CreateAidRequestUseCase createAidRequestUseCase;

    public VoiceAidRequestResponse execute(UUID requesterId, MultipartFile audioFile, CreateAidRequestVoiceInput input) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }

        String rawTranscript = speechToTextService.transcribe(audioFile);
        System.out.println("=== RAW TRANSCRIPT ===");
        System.out.println(rawTranscript);
        AidRequestVoiceLlmService.ExtractionResult extractionResult = aidRequestVoiceLlmService.extractItems(rawTranscript);

        List<AidItemCategory> categories = aidItemCategoryJpaRepository.findByIsLeafTrue();
        if (categories.isEmpty()) {
            throw new IllegalStateException("Inventory categories are empty");
        }

        List<VoiceExtractedItem> extractedItems = new ArrayList<>();
        Map<UUID, Integer> matchedQuantities = new HashMap<>();

        for (AidRequestVoiceLlmService.ExtractedItem extracted : extractionResult.items()) {
            AidItemCategory bestMatch = findBestCategory(extracted.name(), categories);
            int quantity = extracted.quantity() == null ? 1 : Math.max(1, extracted.quantity());

            if (bestMatch != null) {
                matchedQuantities.merge(bestMatch.getId(), quantity, Integer::sum);
            }

            extractedItems.add(VoiceExtractedItem.builder()
                    .name(extracted.name())
                    .quantity(quantity)
                    .matched(bestMatch != null)
                    .matchedCategoryId(bestMatch != null ? bestMatch.getId() : null)
                    .matchedCategoryName(bestMatch != null ? bestMatch.getName() : null)
                    .build());
        }

        if (matchedQuantities.isEmpty()) {
            throw new IllegalArgumentException("No extracted items matched inventory categories");
        }

        List<AidItemInput> itemInputs = matchedQuantities.entrySet().stream()
                .map(entry -> AidItemInput.builder()
                        .itemCategoryId(entry.getKey())
                        .quantity(entry.getValue())
                        .description("Generated from voice transcript")
                        .build())
                .toList();

        String combinedNotes = (input.getNotes() == null ? "" : input.getNotes().trim());
        if (!combinedNotes.isEmpty()) {
            combinedNotes = combinedNotes + "\n";
        }
        combinedNotes = combinedNotes + extractionResult.normalizedTranscript();

        CreateAidRequest createAidRequest = CreateAidRequest.builder()
                .lat(input.getLat())
                .lng(input.getLng())
                .address(input.getAddress())
                .adultsCount(1)
                .elderlyCount(1)
                .childrenCount(1)
                .notes(combinedNotes)
                .urgencyLevel(input.getUrgencyLevel())
                .items(itemInputs)
                .build();

        AidRequestResponse aidRequestResponse = createAidRequestUseCase.execute(requesterId, createAidRequest);

        return VoiceAidRequestResponse.builder()
                .rawTranscript(rawTranscript)
                .normalizedTranscript(extractionResult.normalizedTranscript())
                .extractedItems(extractedItems)
                .aidRequest(aidRequestResponse)
                .build();
    }

    private AidItemCategory findBestCategory(String itemName, List<AidItemCategory> categories) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }

        String normalizedItem = normalize(itemName);

        for (AidItemCategory category : categories) {
            if (normalize(category.getName()).equals(normalizedItem)) {
                return category;
            }
        }

        return categories.stream()
                .filter(category -> {
                    String normalizedCategory = normalize(category.getName());
                    return normalizedCategory.contains(normalizedItem) || normalizedItem.contains(normalizedCategory);
                })
                .max(Comparator.comparingInt(category -> normalize(category.getName()).length()))
                .orElse(null);
    }

    private String normalize(String value) {
        String withoutAccent = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccent
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
