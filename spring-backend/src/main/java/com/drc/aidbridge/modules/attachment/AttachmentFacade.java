package com.drc.aidbridge.modules.attachment;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AttachmentFacade {

    AttachmentDTO upload(UUID uploadedBy, MultipartFile file);

    AttachmentDTO upload(UUID uploadedBy, MultipartFile file, String referenceType, UUID referenceId);
}
