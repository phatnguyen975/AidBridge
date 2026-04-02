package com.drc.aidbridge.modules.attachment.internal.web;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.internal.usecase.UploadAttachmentUseCase;
import com.drc.aidbridge.modules.attachment.internal.web.dto.AttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final UploadAttachmentUseCase uploadAttachmentUseCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "reference_type", required = false) String referenceType,
            @RequestParam(value = "reference_id", required = false) UUID referenceId) {
        UUID userId = resolveUserId(authentication);
        AttachmentDTO attachment = uploadAttachmentUseCase.execute(userId, file, referenceType, referenceId);
        AttachmentResponse response = AttachmentResponse.from(attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        return UUID.fromString(principal.toString());
    }
}
