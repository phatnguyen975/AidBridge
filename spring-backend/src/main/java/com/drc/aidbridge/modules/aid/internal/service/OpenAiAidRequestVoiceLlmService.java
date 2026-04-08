package com.drc.aidbridge.modules.aid.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openai.llm.mode", havingValue = "remote", matchIfMissing = true)
public class OpenAiAidRequestVoiceLlmService implements AidRequestVoiceLlmService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.llm.remote-url:https://api.openai.com/v1/chat/completions}")
    private String llmUrl;

    @Value("${openai.llm.remote-model:gpt-4o-mini}")
    private String llmModel;

    @Override
    public ExtractionResult extractItems(String rawTranscript) {
        if (rawTranscript == null || rawTranscript.isBlank()) {
            throw new IllegalArgumentException("Transcript is empty");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for LLM extraction");
        }

        try {
            String systemPrompt = "You are a Vietnamese disaster-relief parser. Correct spelling errors and extract only essential aid items. Return strict JSON with schema: {\"normalizedTranscript\":\"string\",\"items\":[{\"name\":\"string\",\"quantity\":number,\"note\":\"string\"}]}. No markdown.";

            Map<String, Object> payload = Map.of(
                    "model", llmModel,
                    "temperature", 0,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", rawTranscript)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(llmUrl, requestEntity, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new IllegalStateException("LLM returned empty response");
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new IllegalStateException("LLM response does not contain message content");
            }

            String content = contentNode.asText();
            System.out.println("=== CONTENT ===");
            System.out.println(content);

            String jsonContent = extractJsonContent(content);
            JsonNode parsed = objectMapper.readTree(jsonContent);

            String normalized = parsed.path("normalizedTranscript").asText(rawTranscript);
            List<ExtractedItem> extractedItems = new ArrayList<>();
            JsonNode itemsNode = parsed.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String name = item.path("name").asText("").trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    int quantity = Math.max(1, item.path("quantity").asInt(1));
                    String note = item.path("note").asText("");
                    extractedItems.add(new ExtractedItem(name, quantity, note));
                }
            }

            if (extractedItems.isEmpty()) {
                throw new IllegalStateException("No items extracted from transcript");
            }

            return new ExtractionResult(normalized, extractedItems);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to extract aid items from transcript using LLM", ex);
        }
    }

    private String extractJsonContent(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
