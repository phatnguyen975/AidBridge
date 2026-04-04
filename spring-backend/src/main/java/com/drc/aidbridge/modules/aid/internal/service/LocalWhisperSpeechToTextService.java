package com.drc.aidbridge.modules.aid.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "openai.method", havingValue = "local")
public class LocalWhisperSpeechToTextService implements SpeechToTextService {

    private final String command;
    private final String model;
    private final String language;

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
            tempFile = Files.createTempFile("aidbridge-whisper-", "-" + audioFile.getOriginalFilename());
            Files.write(tempFile, audioFile.getBytes());

            outputDir = Files.createTempDirectory("aidbridge-whisper-output");

            ProcessBuilder pb = new ProcessBuilder(
                    command,
                    tempFile.toAbsolutePath().toString(),
                    "--model", model,
                    "--language", language,
                    "--task", "transcribe",
                    "--output_dir", outputDir.toAbsolutePath().toString(),
                    "--output_format", "txt"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            int code = process.waitFor();
            if (code != 0) {
                throw new IllegalStateException("Local Whisper command failed (code=" + code + "): " + output);
            }

            List<Path> transcripts = Files.list(outputDir).filter(p -> p.toString().endsWith(".txt")).collect(Collectors.toList());
            if (transcripts.isEmpty()) {
                throw new IllegalStateException("No transcription output found in " + outputDir);
            }

            return Files.readString(transcripts.get(0)).trim();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Failed to transcribe audio using local Whisper", ex);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
            if (outputDir != null) {
                try {
                    Files.walk(outputDir)
                            .sorted((a,b) -> b.compareTo(a))
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {}; });
                } catch (IOException ignored) {
                }
            }
        }
    }
}
