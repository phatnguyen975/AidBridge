package com.drc.aidbridge.modules.aid.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "openai.method", havingValue = "local")
public class LocalWhisperSpeechToTextService implements SpeechToTextService {

    private final String command;
    private final String model;
    private final String language;
    private final long timeoutSeconds = 300;

    public LocalWhisperSpeechToTextService(
            @Value("${openai.local-whisper.command:whisper}") String command,
            @Value("${openai.local-whisper.model:small}") String model,
            @Value("${openai.local-whisper.language:en}") String language
    ) {
        this.command = command;
        this.model = model;
        this.language = language;
    }

    @Override
    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }

        Path tempFile = null;
        Path outputDir = null;

        try {
            // Save temp audio
            tempFile = Files.createTempFile("aidbridge-whisper-", "-" + audioFile.getOriginalFilename());
            Files.write(tempFile, audioFile.getBytes());

            outputDir = Files.createTempDirectory("aidbridge-whisper-output");

            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "-X", "utf8", // force UTF-8 mode
                    "-m", "whisper",
                    tempFile.toAbsolutePath().toString(),
                    "--model", model,
                    "--language", language,
                    "--task", "transcribe",
                    "--output_dir", outputDir.toAbsolutePath().toString(),
                    "--output_format", "txt"
            );

            // ✅ Fix encoding environment
            pb.environment().put("PYTHONIOENCODING", "UTF-8");
            pb.environment().put("PYTHONUTF8", "1");
            pb.environment().put("LANG", "en_US.UTF-8");
            pb.environment().put("LC_ALL", "en_US.UTF-8");
            pb.environment().put("LC_CTYPE", "en_US.UTF-8");

            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output safely as UTF-8
            StringBuilder outputBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                }
            }

            // Wait with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Whisper timed out after " + timeoutSeconds + " seconds");
            }

            int code = process.exitValue();
            String output = outputBuilder.toString();

            if (code != 0) {
                throw new IllegalStateException("Whisper failed (code=" + code + "): " + output);
            }

            // Find transcript file
            List<Path> transcripts = Files.list(outputDir)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .collect(Collectors.toList());

            if (transcripts.isEmpty()) {
                throw new IllegalStateException("No transcription output found in " + outputDir);
            }

            return Files.readString(transcripts.get(0), StandardCharsets.UTF_8).trim();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to transcribe audio using local Whisper", ex);
        } finally {
            try {
                if (tempFile != null) Files.deleteIfExists(tempFile);
                if (outputDir != null) {
                    Files.walk(outputDir)
                            .sorted((a, b) -> b.compareTo(a))
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                            });
                }
            } catch (Exception ignored) {}
        }
    }
}