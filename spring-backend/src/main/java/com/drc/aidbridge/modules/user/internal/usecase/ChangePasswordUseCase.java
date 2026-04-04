package com.drc.aidbridge.modules.user.internal.usecase;

import com.drc.aidbridge.modules.shared.exception.BadRequestException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import com.drc.aidbridge.modules.user.internal.web.dto.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }
}
