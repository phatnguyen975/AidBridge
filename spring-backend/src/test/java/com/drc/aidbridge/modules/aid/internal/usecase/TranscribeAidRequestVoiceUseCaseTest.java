package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.internal.service.SpeechToTextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TranscribeAidRequestVoiceUseCaseTest {

    private SpeechToTextService speechToTextService;
    private TranscribeAidRequestVoiceUseCase useCase;

    @BeforeEach
    void setUp() {
        speechToTextService = Mockito.mock(SpeechToTextService.class);
        useCase = new TranscribeAidRequestVoiceUseCase(speechToTextService);
    }

    @Test
    void execute_ShouldReturnTranscript_WhenFileIsValid() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(speechToTextService.transcribe(file)).thenReturn("hello world");

        String result = useCase.execute(file);

        assertEquals("hello world", result);
        verify(speechToTextService, times(1)).transcribe(file);
    }

    @Test
    void execute_ShouldThrow_WhenFileIsEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> useCase.execute(file));
        assertEquals("Audio file must not be empty", ex.getMessage());
        verify(speechToTextService, never()).transcribe(any());
    }
}
