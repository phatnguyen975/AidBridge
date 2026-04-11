package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.DuplicateResourceException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UpdateUserProfileRequest;
import com.drc.aidbridge.modules.user.internal.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case cập nhật thông tin hồ sơ cơ bản của user.
 * Hiện tại hệ thống lưu trữ full name và phone number trong bảng users.
 * Trường address được nhận để tương thích client contract và sẽ được mở rộng khi có schema tương ứng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserProfileUseCase {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse execute(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedName = request.getName().trim();
        String normalizedPhone = request.getPhone().trim();

        userRepository.findByPhoneNumber(normalizedPhone)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException("Phone number is already in use");
                });

        user.setFullName(normalizedName);
        user.setPhoneNumber(normalizedPhone);

        User updatedUser = userRepository.save(user);

        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            log.debug("Address provided for user {} but not persisted because users.address is not available yet", userId);
        }

        log.info("Updated profile for user: {}", userId);
        return userMapper.toResponse(updatedUser);
    }
}
