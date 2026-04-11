package com.drc.aidbridge.modules.aid.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class LocalCliAidRequestVoiceLlmService implements AidRequestVoiceLlmService {

    private static final String COMMAND = "ollama";
    private static final String MODEL = "llama3";
    private static final long TIMEOUT_SECONDS = 300;

    private static final String ROOT_CATEGORIES_JSON = """
                        [
                            {"name":"Nước uống"},
                            {"name":"Nhu yếu phẩm khác"},
                            {"name":"Quần áo"},
                            {"name":"Thuốc"},
                            {"name":"Thực phẩm"}
                        ]
                        """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ExtractionResult extractItems(String rawTranscript) {
        if (rawTranscript == null || rawTranscript.isBlank()) {
            throw new IllegalArgumentException("Transcript is empty");
        }

        String prompt = buildPrompt(rawTranscript);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(COMMAND, "run", MODEL);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // ✅ Send prompt
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    process.getOutputStream(),
                    StandardCharsets.UTF_8)) {
                writer.write(prompt);
                writer.flush();
            } // auto close stdin

            // ✅ Wait for process to complete FIRST
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Local LLM CLI timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            int code = process.exitValue();

            // ✅ Now safely read output
            String output = readProcessOutput(process);

            System.out.println("=== RAW OUTPUT ===");
            System.out.println(output);

            if (code != 0) {
                throw new IllegalStateException("Local LLM CLI failed (code=" + code + "): " + output);
            }

            // ✅ Sanitize
            String sanitized = sanitizeCliOutput(output);

            System.out.println("=== SANITIZED OUTPUT ===");
            System.out.println(sanitized);

            JsonNode parsed = parseBestJsonNode(sanitized);

            String normalized = parsed.path("normalizedTranscript").asText(rawTranscript);
            JsonNode categoryNamesNode = parsed.path("categoryNames");
            JsonNode itemsNode = parsed.path("items");
            if (!itemsNode.isArray() && parsed.has("data") && parsed.path("data").isArray()) {
                itemsNode = parsed.path("data");
            }
            if (!itemsNode.isArray() && parsed.isArray()) {
                itemsNode = parsed;
            }

            List<ExtractedItem> extractedItems = new ArrayList<>();

            if (categoryNamesNode.isArray()) {
                for (JsonNode categoryNode : categoryNamesNode) {
                    String categoryName = categoryNode.asText("").trim();
                    if (categoryName.isEmpty()) {
                        continue;
                    }
                    extractedItems.add(new ExtractedItem(categoryName, 1, "classified_category"));
                }
            }

            if (extractedItems.isEmpty() && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    String name = itemNode.path("name").asText("").trim();
                    if (name.isEmpty()) {
                        name = itemNode.path("item").asText("").trim();
                    }
                    if (name.isEmpty()) continue;

                    int quantity = Math.max(1, itemNode.path("quantity").asInt(1));
                    String note = itemNode.path("note").asText("");

                    extractedItems.add(new ExtractedItem(name, quantity, note));
                }
            }

            if (extractedItems.isEmpty()) {
                throw new IllegalStateException("No items extracted from local LLM output");
            }

            return new ExtractionResult(normalized, extractedItems);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting local LLM CLI", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to extract aid items using local LLM CLI", ex);
        }
    }

    /**
     * Read process output AFTER process finished
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        return output.toString();
    }

    private String buildPrompt(String transcript) {
        return """
                                Bạn là bộ phân loại yêu cầu cứu trợ bằng tiếng Việt.

                                Có đúng 5 loại sản phẩm gốc như sau:
                                %s

                                Yêu cầu:
                                1) Sửa lỗi chính tả transcript.
                                2) Chỉ xác định loại sản phẩm nào được nhắc tới trong transcript.
                                3) Trả về danh sách TÊN category gốc tương ứng trong trường categoryNames.
                                4) Không thêm loại ngoài danh sách.

                                Trả về DUY NHẤT JSON hợp lệ theo schema:
                                {
                                    "normalizedTranscript":"string",
                                                    "categoryNames":["Cat 1","Cat 2"]
                                }

                                Không markdown, không giải thích.

                                Transcript:
                                %s
                                """.formatted(ROOT_CATEGORIES_JSON, transcript);
    }

    private JsonNode parseBestJsonNode(String output) throws Exception {
        if (output == null || output.isBlank()) {
            throw new IllegalStateException("Local LLM returned empty output");
        }

        String trimmed = output.trim();
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);

        String fenced = extractFencedJson(trimmed);
        if (fenced != null) {
            candidates.add(fenced);
        }

        String objectSlice = sliceBetween(trimmed, '{', '}');
        if (objectSlice != null) {
            candidates.add(objectSlice);
        }

        String arraySlice = sliceBetween(trimmed, '[', ']');
        if (arraySlice != null) {
            candidates.add(arraySlice);
        }

        Exception lastException = null;
        for (String candidate : candidates) {
            try {
                JsonNode node = objectMapper.readTree(candidate);
                if (isUsableRoot(node)) {
                    return node;
                }
            } catch (Exception ex) {
                lastException = ex;
            }
        }

        throw new IllegalStateException("Invalid JSON structure from LLM: " + trimmed, lastException);
    }

    private boolean isUsableRoot(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.isObject()) {
            return node.has("categoryNames") || node.has("items") || node.has("normalizedTranscript") || node.has("data");
        }
        return node.isArray();
    }

    private String extractFencedJson(String value) {
        int firstFence = value.indexOf("```");
        if (firstFence < 0) {
            return null;
        }
        int secondFence = value.indexOf("```", firstFence + 3);
        if (secondFence < 0) {
            return null;
        }
        String inside = value.substring(firstFence + 3, secondFence).trim();
        if (inside.startsWith("json")) {
            inside = inside.substring(4).trim();
        }
        return inside;
    }

    private String sliceBetween(String value, char open, char close) {
        int start = value.indexOf(open);
        int end = value.lastIndexOf(close);
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return null;
    }

    private String sanitizeCliOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return rawOutput;
        }

        // Remove ANSI escape sequences
        String cleaned = rawOutput
                .replaceAll("\\u001B\\[[;?0-9]*[ -/]*[@-~]", "")
                .replaceAll("\\u001B\\][^\\u0007]*\\u0007", "")
                .replace("\u001B", "");

        // Remove control characters
        StringBuilder result = new StringBuilder(cleaned.length());
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t' || !Character.isISOControl(ch)) {
                result.append(ch);
            }
        }

        return result.toString();
    }
}