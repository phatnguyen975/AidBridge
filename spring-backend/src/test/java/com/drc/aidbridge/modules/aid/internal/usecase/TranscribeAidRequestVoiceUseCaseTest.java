package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.service.AidRequestVoiceLlmService;
import com.drc.aidbridge.modules.aid.internal.service.SpeechToTextService;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequest;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequestVoiceInput;
import com.drc.aidbridge.modules.aid.internal.web.dto.VoiceAidRequestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranscribeAidRequestVoiceUseCaseTest {

    private SpeechToTextService speechToTextService;
    private AidRequestVoiceLlmService llmService;
    private AidItemCategoryJpaRepository categoryRepository;
    private CreateAidRequestUseCase createAidRequestUseCase;
    private TranscribeAidRequestVoiceUseCase useCase;

    @BeforeEach
    void setUp() {
        speechToTextService = mock(SpeechToTextService.class);
        llmService = mock(AidRequestVoiceLlmService.class);
        categoryRepository = mock(AidItemCategoryJpaRepository.class);
        createAidRequestUseCase = mock(CreateAidRequestUseCase.class);
        useCase = new TranscribeAidRequestVoiceUseCase(speechToTextService, llmService, categoryRepository, createAidRequestUseCase);
    }

    @Test
    void execute_ShouldCreateAidRequest_WhenItemsMatched() {
        MultipartFile file = mock(MultipartFile.class);
        UUID requesterId = UUID.randomUUID();
        UUID matchedCategoryId = UUID.randomUUID();

        when(file.isEmpty()).thenReturn(false);
        when(speechToTextService.transcribe(file)).thenReturn("toi can 2 thung nuoc sach");
        when(llmService.extractItems("toi can 2 thung nuoc sach"))
                .thenReturn(new AidRequestVoiceLlmService.ExtractionResult(
                        "Tôi cần 2 thùng nước sạch",
                        List.of(new AidRequestVoiceLlmService.ExtractedItem("nuoc", 2, ""))
                ));

        AidItemCategory category = new AidItemCategory();
        ReflectionTestUtils.setField(category, "id", matchedCategoryId);
        ReflectionTestUtils.setField(category, "name", "Nước sạch");
        ReflectionTestUtils.setField(category, "unit", "thung");
        ReflectionTestUtils.setField(category, "isLeaf", true);
        when(categoryRepository.findByIsLeafTrue()).thenReturn(List.of(category));

        AidRequestResponse aidRequestResponse = AidRequestResponse.builder().id(UUID.randomUUID()).build();
        when(createAidRequestUseCase.execute(any(), any(CreateAidRequest.class))).thenReturn(aidRequestResponse);

        CreateAidRequestVoiceInput input = new CreateAidRequestVoiceInput();
        input.setLat(BigDecimal.valueOf(10.77));
        input.setLng(BigDecimal.valueOf(106.69));
        input.setAdultsCount(1);

        VoiceAidRequestResponse result = useCase.execute(requesterId, file, input);

        assertNotNull(result);
        assertEquals("toi can 2 thung nuoc sach", result.getRawTranscript());
        assertEquals("Tôi cần 2 thùng nước sạch", result.getNormalizedTranscript());
        assertEquals(1, result.getExtractedItems().size());
        assertEquals(true, result.getExtractedItems().get(0).isMatched());

        ArgumentCaptor<CreateAidRequest> requestCaptor = ArgumentCaptor.forClass(CreateAidRequest.class);
        verify(createAidRequestUseCase, times(1)).execute(any(), requestCaptor.capture());
        assertEquals(1, requestCaptor.getValue().getItems().size());
        assertEquals(matchedCategoryId, requestCaptor.getValue().getItems().get(0).getItemCategoryId());
        assertEquals(2, requestCaptor.getValue().getItems().get(0).getQuantity());
    }

    @Test
    void execute_ShouldThrow_WhenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.randomUUID(), file, new CreateAidRequestVoiceInput()));

        assertEquals("Audio file must not be empty", ex.getMessage());
    }
}
