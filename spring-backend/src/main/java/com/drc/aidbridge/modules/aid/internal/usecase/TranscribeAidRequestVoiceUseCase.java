package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.service.AidRequestVoiceLlmService;
import com.drc.aidbridge.modules.aid.internal.service.SpeechToTextService;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidItemInput;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequest;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequestVoiceInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TranscribeAidRequestVoiceUseCase {

    private final SpeechToTextService speechToTextService;
    private final AidRequestVoiceLlmService aidRequestVoiceLlmService;
    private final AidItemCategoryJpaRepository aidItemCategoryJpaRepository;
    private final CreateAidRequestUseCase createAidRequestUseCase;

    public AidRequestResponse execute(UUID requesterId, MultipartFile audioFile, CreateAidRequestVoiceInput input) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }

        String rawTranscript = speechToTextService.transcribe(audioFile);
        System.out.println("Raw transcript: " + rawTranscript);
        AidRequestVoiceLlmService.ExtractionResult extractionResult = aidRequestVoiceLlmService.extractItems(rawTranscript);

        List<AidItemCategory> categories = aidItemCategoryJpaRepository.findByIsLeafFalse();
        if (categories.isEmpty()) {
            throw new IllegalStateException("Root item categories are empty");
        }

        Set<UUID> matchedCategoryIds = new LinkedHashSet<>();

        for (AidRequestVoiceLlmService.ExtractedItem extracted : extractionResult.items()) {
            AidItemCategory bestMatch = findBestCategory(extracted.name(), categories);

            if (bestMatch != null) {
                matchedCategoryIds.add(bestMatch.getId());
            }
        }

        if (matchedCategoryIds.isEmpty()) {
            throw new IllegalArgumentException("No extracted items matched inventory categories");
        }

        List<AidItemInput> itemInputs = matchedCategoryIds.stream()
                .map(categoryId -> AidItemInput.builder()
                        .itemCategoryId(categoryId)

                        .build())
                .toList();

        String combinedNotes = (input.getNotes() == null ? "" : input.getNotes().trim());
        if (!combinedNotes.isEmpty()) {
            combinedNotes = combinedNotes + "\n";
        }
        combinedNotes = combinedNotes + extractionResult.normalizedTranscript();
        System.out.println("Combined notes: " + combinedNotes);
        CreateAidRequest createAidRequest = CreateAidRequest.builder()
                .lat(input.getLat())
                .lng(input.getLng())
                .address(input.getAddress())
                .adultsCount(input.getAdultsCount() != 0 ? input.getAdultsCount() : 1)
                .elderlyCount(input.getElderlyCount() != 0 ? input.getElderlyCount() : 0)
                .childrenCount(input.getChildrenCount() != 0 ? input.getChildrenCount() : 0)
                .notes(combinedNotes)
                .urgencyLevel(input.getUrgencyLevel())
                .items(itemInputs)
                .build();

        return createAidRequestUseCase.execute(requesterId, createAidRequest);
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
