package com.drc.aidbridge.modules.attachment;

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
public class AttachmentDTO {

    private UUID id;
    private String url;
    private String fileName;
    private long fileSize;
    private String mimeType;
    private UUID uploadedBy;
    private String referenceType;
    private UUID referenceId;
    private Instant createdAt;
}
