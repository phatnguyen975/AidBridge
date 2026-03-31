package com.drc.aidbridge.modules.attachment.internal.web.dto;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private UUID id;
    private String url;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_size")
    private long fileSize;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("uploaded_by")
    private UUID uploadedBy;

    @JsonProperty("created_at")
    private Instant createdAt;

    public static AttachmentResponse from(AttachmentDTO attachmentDTO) {
        return AttachmentResponse.builder()
                .id(attachmentDTO.getId())
                .url(attachmentDTO.getUrl())
                .fileName(attachmentDTO.getFileName())
                .fileSize(attachmentDTO.getFileSize())
                .mimeType(attachmentDTO.getMimeType())
                .uploadedBy(attachmentDTO.getUploadedBy())
                .createdAt(attachmentDTO.getCreatedAt())
                .build();
    }
}
