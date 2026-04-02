package com.drc.aidbridge.modules.attachment.internal;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.AttachmentFacade;
import com.drc.aidbridge.modules.attachment.internal.usecase.UploadAttachmentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentFacadeImpl implements AttachmentFacade {

    private final UploadAttachmentUseCase uploadAttachmentUseCase;

    @Override
    public AttachmentDTO upload(UUID uploadedBy, MultipartFile file) {
        return uploadAttachmentUseCase.execute(uploadedBy, file, null, null);
    }

    @Override
    public AttachmentDTO upload(UUID uploadedBy, MultipartFile file, String referenceType, UUID referenceId) {
        return uploadAttachmentUseCase.execute(uploadedBy, file, referenceType, referenceId);
    }
}
