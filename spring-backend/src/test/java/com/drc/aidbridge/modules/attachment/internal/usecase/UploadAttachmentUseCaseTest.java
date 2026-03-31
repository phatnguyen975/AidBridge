package com.drc.aidbridge.modules.attachment.internal.usecase;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.internal.entity.Attachment;
import com.drc.aidbridge.modules.attachment.internal.repository.AttachmentJpaRepository;
import com.drc.aidbridge.modules.attachment.internal.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadAttachmentUseCaseTest {

    @Mock
    private AttachmentJpaRepository attachmentRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private UploadAttachmentUseCase uploadAttachmentUseCase;

    @Test
    void executePersistsAttachmentAndReturnsDto() {
        UUID uploadedBy = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        MultipartFile file = new MockMultipartFile(
                "file",
                "report.png",
                "image/png",
                "payload".getBytes());

        when(cloudinaryService.uploadImage(file))
                .thenReturn(new CloudinaryService.UploadedImage(
                        "https://cdn.example.com/report.png",
                        "attachment_public_id"));
        when(attachmentRepository.save(any(Attachment.class)))
                .thenAnswer(invocation -> {
                    Attachment attachment = invocation.getArgument(0);
                    attachment.setId(attachmentId);
                    attachment.setCreatedAt(Instant.parse("2026-04-01T08:30:00Z"));
                    return attachment;
                });

        AttachmentDTO response = uploadAttachmentUseCase.execute(uploadedBy, file);

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());

        Attachment savedAttachment = attachmentCaptor.getValue();
        assertEquals("https://cdn.example.com/report.png", savedAttachment.getUrl());
        assertEquals("report.png", savedAttachment.getFileName());
        assertEquals(file.getSize(), savedAttachment.getFileSize());
        assertEquals("image/png", savedAttachment.getMimeType());
        assertEquals(uploadedBy, savedAttachment.getUploadedBy());
        assertEquals("attachment_public_id", savedAttachment.getCloudinaryPublicId());

        assertEquals(attachmentId, response.getId());
        assertEquals(savedAttachment.getUrl(), response.getUrl());
        assertEquals(savedAttachment.getFileName(), response.getFileName());
        assertEquals(savedAttachment.getFileSize(), response.getFileSize());
        assertEquals(savedAttachment.getMimeType(), response.getMimeType());
        assertEquals(savedAttachment.getUploadedBy(), response.getUploadedBy());
        assertEquals(Instant.parse("2026-04-01T08:30:00Z"), response.getCreatedAt());
    }

    @Test
    void executeRejectsNullUploadedBy() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "report.png",
                "image/png",
                "payload".getBytes());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> uploadAttachmentUseCase.execute(null, file));

        assertEquals("uploadedBy must not be null", exception.getMessage());
    }
}
