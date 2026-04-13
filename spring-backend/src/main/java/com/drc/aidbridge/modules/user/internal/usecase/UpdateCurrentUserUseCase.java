package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UpdateProfileRequest;
import com.drc.aidbridge.modules.user.internal.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateCurrentUserUseCase {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse execute(UUID userId, UpdateProfileRequest request) {
        log.debug("Updating user profile: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for update: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        if (!user.isActive()) {
            log.warn("Cannot update profile for deactivated user: {}", userId);
            throw new ResourceNotFoundException("User account is no longer active");
        }

        // Update fullName if provided
        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
            log.debug("Updated fullName for user: {}", userId);
        }

        // Update avatarUrl if provided
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
            log.debug("Updated avatarUrl for user: {}", userId);
        }

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated user profile: {}", userId);

        return userMapper.toResponse(updatedUser);
    }
}
