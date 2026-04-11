package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.attachment.AttachmentDTO;
import com.drc.aidbridge.modules.attachment.AttachmentFacade;
import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UserAvatarUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Use case upload avatar cho user và cập nhật URL ảnh đại diện trong bảng users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserAvatarUseCase {

    private static final String USER_AVATAR_REFERENCE_TYPE = "USER_AVATAR";

    private final UserJpaRepository userRepository;
    private final AttachmentFacade attachmentFacade;

    @Transactional
    public UserAvatarUploadResponse execute(UUID userId, MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AttachmentDTO attachment = attachmentFacade.upload(userId, avatarFile, USER_AVATAR_REFERENCE_TYPE, userId);
        String avatarUrl = attachment.getUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalStateException("Avatar upload succeeded but URL is empty");
        }

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("Updated avatar for user: {}", userId);

        return UserAvatarUploadResponse.builder()
                .avatarUrl(avatarUrl)
                .build();
    }
}
