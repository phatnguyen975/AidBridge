package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.mapper.UserMapper;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse execute(UUID userId) {
        log.debug("Fetching current user profile: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });

        if (!user.isActive()) {
            log.warn("User account is deactivated: {}", userId);
            throw new ResourceNotFoundException("User account is no longer active");
        }

        return userMapper.toResponse(user);
    }
}
