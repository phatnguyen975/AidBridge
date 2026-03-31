package com.drc.aidbridge.modules.attachment.internal.web;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.internal.usecase.UploadAttachmentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttachmentControllerTest {

    private MockMvc mockMvc;
    private UploadAttachmentUseCase uploadAttachmentUseCase;

    @BeforeEach
    void setUp() {
        uploadAttachmentUseCase = mock(UploadAttachmentUseCase.class);
        AttachmentController controller = new AttachmentController(uploadAttachmentUseCase);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void uploadAttachmentReturnsCreatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.png",
                MediaType.IMAGE_PNG_VALUE,
                "image-bytes".getBytes());

        AttachmentDTO attachment = AttachmentDTO.builder()
                .id(UUID.randomUUID())
                .url("https://cdn.example.com/evidence.png")
                .fileName("evidence.png")
                .fileSize(file.getSize())
                .mimeType(MediaType.IMAGE_PNG_VALUE)
                .uploadedBy(userId)
                .createdAt(Instant.parse("2026-04-01T10:15:30Z"))
                .build();

        when(uploadAttachmentUseCase.execute(eq(userId), any())).thenReturn(attachment);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_VICTIM")));

        mockMvc.perform(multipart("/attachments")
                        .file(file)
                        .principal(authentication))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data.id").value(attachment.getId().toString()))
                .andExpect(jsonPath("$.data.url").value(attachment.getUrl()))
                .andExpect(jsonPath("$.data.file_name").value(attachment.getFileName()))
                .andExpect(jsonPath("$.data.file_size").value((int) attachment.getFileSize()))
                .andExpect(jsonPath("$.data.mime_type").value(attachment.getMimeType()))
                .andExpect(jsonPath("$.data.uploaded_by").value(userId.toString()))
                .andExpect(jsonPath("$.data.created_at", startsWith("2026-04-01T10:15:30")));
    }
}
