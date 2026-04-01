package com.drc.aidbridge.modules.attachment.internal.usecase;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.internal.entity.Attachment;
import com.drc.aidbridge.modules.attachment.internal.repository.AttachmentJpaRepository;
import com.drc.aidbridge.modules.attachment.internal.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UploadAttachmentUseCase {

    private final AttachmentJpaRepository attachmentRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public AttachmentDTO execute(UUID uploadedBy, MultipartFile file) {
        if (uploadedBy == null) {
            throw new IllegalArgumentException("uploadedBy must not be null");
        }

        CloudinaryService.UploadedImage uploadedImage = cloudinaryService.uploadImage(file);

        Attachment attachment = Attachment.builder()
                .url(uploadedImage.url())
                .fileName(normalizeFileName(file))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .uploadedBy(uploadedBy)
                .cloudinaryPublicId(uploadedImage.publicId())
                .build();

        Attachment savedAttachment = attachmentRepository.save(attachment);
        return mapToDto(savedAttachment);
    }

    private String normalizeFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            return "image";
        }
        originalFilename = StringUtils.cleanPath(originalFilename);
        return StringUtils.hasText(originalFilename) ? originalFilename : "image";
    }

    private AttachmentDTO mapToDto(Attachment attachment) {
        return AttachmentDTO.builder()
                .id(attachment.getId())
                .url(attachment.getUrl())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .uploadedBy(attachment.getUploadedBy())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
