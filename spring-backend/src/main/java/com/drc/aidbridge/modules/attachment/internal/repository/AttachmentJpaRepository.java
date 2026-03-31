package com.drc.aidbridge.modules.attachment.internal.repository;

import com.drc.aidbridge.modules.attachment.internal.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttachmentJpaRepository extends JpaRepository<Attachment, UUID> {
}
