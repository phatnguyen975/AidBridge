package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.entity.AidItemCategory;
import com.drc.aidbridge.modules.aid.internal.repository.AidItemCategoryJpaRepository;
import com.drc.aidbridge.modules.aid.internal.service.AidRequestVoiceLlmService;
import com.drc.aidbridge.modules.aid.internal.service.SpeechToTextService;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequest;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequestVoiceInput;
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
        UUID matchedCategoryId = UUID.fromString("9a1ae288-fa90-439c-ba37-30f61527da9c");

        when(file.isEmpty()).thenReturn(false);
        when(speechToTextService.transcribe(file)).thenReturn("toi can 2 thung nuoc sach");
        when(llmService.extractItems("toi can 2 thung nuoc sach"))
                .thenReturn(new AidRequestVoiceLlmService.ExtractionResult(
                        "Tôi cần 2 thùng nước sạch",
                List.of(new AidRequestVoiceLlmService.ExtractedItem("Nước uống", 1, "classified_category"))
                ));

        when(categoryRepository.findByIsLeafFalse()).thenReturn(List.of(
            rootCategory("9a1ae288-fa90-439c-ba37-30f61527da9c", "Nước uống"),
            rootCategory("b72f09ae-ba51-4d45-9834-daa6b6c11381", "Nhu yếu phẩm khác"),
            rootCategory("ba53bc22-68c7-435b-8c45-34f158462f10", "Quần áo"),
            rootCategory("e0127e1d-7b3e-4018-bd4b-b5f6df1bae9d", "Thuốc"),
            rootCategory("f708a3fc-53f9-429b-b9dc-a17223905a63", "Thực phẩm")
        ));

        AidRequestResponse aidRequestResponse = AidRequestResponse.builder().id(UUID.randomUUID()).build();
        when(createAidRequestUseCase.execute(any(), any(CreateAidRequest.class))).thenReturn(aidRequestResponse);

        CreateAidRequestVoiceInput input = new CreateAidRequestVoiceInput();
        input.setLat(BigDecimal.valueOf(10.77));
        input.setLng(BigDecimal.valueOf(106.69));
        input.setAdultsCount(1);

        AidRequestResponse result = useCase.execute(requesterId, file, input);

        assertNotNull(result);
        assertEquals(aidRequestResponse.getId(), result.getId());

        ArgumentCaptor<CreateAidRequest> requestCaptor = ArgumentCaptor.forClass(CreateAidRequest.class);
        verify(createAidRequestUseCase, times(1)).execute(any(), requestCaptor.capture());
        assertEquals(1, requestCaptor.getValue().getItems().size());
        assertEquals(matchedCategoryId, requestCaptor.getValue().getItems().get(0).getItemCategoryId());
        assertEquals(1, requestCaptor.getValue().getAdultsCount());
        assertEquals(0, requestCaptor.getValue().getElderlyCount());
        assertEquals(0, requestCaptor.getValue().getChildrenCount());
    }

    @Test
    void execute_ShouldThrow_WhenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.randomUUID(), file, new CreateAidRequestVoiceInput()));

        assertEquals("Audio file must not be empty", ex.getMessage());
    }

    private AidItemCategory rootCategory(String id, String name) {
        AidItemCategory category = new AidItemCategory();
        ReflectionTestUtils.setField(category, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(category, "name", name);
        ReflectionTestUtils.setField(category, "isLeaf", false);
        return category;
    }
}
