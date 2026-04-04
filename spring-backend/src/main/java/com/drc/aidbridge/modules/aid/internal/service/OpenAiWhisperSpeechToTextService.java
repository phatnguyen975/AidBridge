package com.drc.aidbridge.modules.aid.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "openai.method", havingValue = "remote", matchIfMissing = true)
public class OpenAiWhisperSpeechToTextService implements SpeechToTextService {

    private final RestTemplate restTemplate;
    private final String openAiUrl;
    private final String model;
    private final String apiKey;

    public OpenAiWhisperSpeechToTextService(@Value("${openai.speech-url:https://api.openai.com/v1/audio/transcriptions}") String openAiUrl,
                                            @Value("${openai.model:whisper-1}") String model,
                                            @Value("${openai.api-key:}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.openAiUrl = openAiUrl;
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public String transcribe(MultipartFile audioFile) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        try {
            ByteArrayResource byteArrayResource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    String originalName = audioFile.getOriginalFilename();
                    return originalName != null ? originalName : "voice.wav";
                }

                @Override
                public long contentLength() {
                    return audioFile.getSize();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model", model);
            body.add("file", byteArrayResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);
            headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<OpenAiTranscriptionResponse> response = restTemplate.postForEntity(openAiUrl, requestEntity, OpenAiTranscriptionResponse.class);
            OpenAiTranscriptionResponse bodyResp = response.getBody();
            if (bodyResp == null || bodyResp.getText() == null) {
                throw new IllegalStateException("OpenAI Whisper transcription failed: empty response");
            }
            return bodyResp.getText();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to transcribe audio", ex);
        }
    }

    private static class OpenAiTranscriptionResponse {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
