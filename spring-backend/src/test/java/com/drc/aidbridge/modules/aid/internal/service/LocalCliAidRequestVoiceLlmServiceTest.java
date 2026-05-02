package com.drc.aidbridge.modules.aid.internal.service;

import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

/**
 * Tests user-facing error handling for local CLI LLM extraction.
 */
class LocalCliAidRequestVoiceLlmServiceTest {

    @Test
    void extractItems_shouldReturnCategories_whenCliReturnsValidJson() throws Exception {
        LocalCliAidRequestVoiceLlmService service = Mockito.spy(new LocalCliAidRequestVoiceLlmService());
        FakeProcess process = new FakeProcess(0,
                "{\"normalizedTranscript\":\"Toi can nuoc\",\"categoryNames\":[\"Nước uống\"]}",
                true);
        doReturn(process).when(service).startProcess();

        AidRequestVoiceLlmService.ExtractionResult result = service.extractItems("toi can nuoc");

        assertNotNull(result);
        assertEquals("Toi can nuoc", result.normalizedTranscript());
        assertEquals(1, result.items().size());
        assertEquals("Nước uống", result.items().get(0).name());
    }

    @Test
    void extractItems_shouldThrowBadRequest_whenCliTimesOut() throws Exception {
        LocalCliAidRequestVoiceLlmService service = Mockito.spy(new LocalCliAidRequestVoiceLlmService());
        FakeProcess process = new FakeProcess(0, "", false);
        doReturn(process).when(service).startProcess();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.extractItems("toi can nuoc"));

        assertEquals("Hệ thống xử lý giọng nói đang bận. Vui lòng thử lại.", ex.getMessage());
    }

    @Test
    void extractItems_shouldThrowBadRequest_whenCliReturnsInvalidJson() throws Exception {
        LocalCliAidRequestVoiceLlmService service = Mockito.spy(new LocalCliAidRequestVoiceLlmService());
        FakeProcess process = new FakeProcess(0, "NOT JSON", true);
        doReturn(process).when(service).startProcess();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.extractItems("toi can nuoc"));

        assertEquals("Không thể phân tích nội dung giọng nói. Vui lòng thử lại.", ex.getMessage());
    }

    private static final class FakeProcess extends Process {
        private final int exitCode;
        private final boolean waitForResult;
        private final InputStream inputStream;
        private final OutputStream outputStream = new ByteArrayOutputStream();

        private FakeProcess(int exitCode, String output, boolean waitForResult) {
            this.exitCode = exitCode;
            this.waitForResult = waitForResult;
            this.inputStream = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() throws InterruptedException {
            if (!waitForResult) {
                Thread.sleep(1);
            }
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return waitForResult;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
        }

        @Override
        public Process destroyForcibly() {
            return this;
        }

        @Override
        public boolean isAlive() {
            return !waitForResult;
        }
    }
}
