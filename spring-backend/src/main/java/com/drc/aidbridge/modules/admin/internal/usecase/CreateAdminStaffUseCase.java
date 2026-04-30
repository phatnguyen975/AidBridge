package com.drc.aidbridge.modules.admin.internal.usecase;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminStaffResponse;
import com.drc.aidbridge.modules.admin.internal.web.dto.CreateAdminStaffRequest;
import com.drc.aidbridge.modules.hub.internal.entity.Hub;
import com.drc.aidbridge.modules.hub.internal.entity.HubStaff;
import com.drc.aidbridge.modules.hub.internal.repository.HubRepository;
import com.drc.aidbridge.modules.hub.internal.repository.HubStaffRepository;
import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.shared.exception.DuplicateResourceException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.internal.entity.User;
import com.drc.aidbridge.modules.user.internal.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CreateAdminStaffUseCase {

    private final UserJpaRepository userRepository;
    private final HubRepository hubRepository;
    private final HubStaffRepository hubStaffRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminStaffResponse execute(CreateAdminStaffRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }

        String fullName = normalizeText(request.fullName());
        String email = normalizeEmail(request.email());
        String phoneNumber = normalizeText(request.phoneNumber());

        if (!StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!StringUtils.hasText(phoneNumber)) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Password is required");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        Hub hub = hubRepository.findById(request.hubId())
                .orElseThrow(() -> new ResourceNotFoundException("Hub not found: " + request.hubId()));

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .phoneNumber(phoneNumber)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.STAFF)
                .isVerified(true)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        HubStaff assignment = HubStaff.builder()
                .hubId(hub.getId())
                .userId(user.getId())
                .isAvailable(true)
                .assignedAt(Instant.now())
                .build();
        hubStaffRepository.save(assignment);

        return new AdminStaffResponse(
                user.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                hub.getId(),
                hub.getName()
        );
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeText(email);
        return normalized.toLowerCase(Locale.ROOT);
    }
}
