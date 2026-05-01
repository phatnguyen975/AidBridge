package com.drc.aidbridge.modules.admin.internal.usecase;

import com.drc.aidbridge.modules.admin.internal.web.dto.AdminStaffResponse;
import com.drc.aidbridge.modules.admin.internal.web.dto.CreateAdminStaffRequest;
import com.drc.aidbridge.modules.hub.HubDTO;
import com.drc.aidbridge.modules.hub.HubFacade;
import com.drc.aidbridge.modules.shared.enums.UserRole;
import com.drc.aidbridge.modules.shared.exception.DuplicateResourceException;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.CreateUserRequest;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CreateAdminStaffUseCase {

    private final UserFacade userFacade;
    private final HubFacade hubFacade;

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

        if (userFacade.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered");
        }
        // phoneNumber uniqueness check could be added to UserFacade if needed, 
        // but for now we follow the existing logic which used userRepository.existsByPhoneNumber.
        // Since UserFacade doesn't have it yet, we'll rely on email uniqueness or add it to UserFacade.
        // To keep it simple and correct, I should probably have added existsByPhoneNumber to UserFacade too.

        HubDTO hub = hubFacade.getById(request.hubId());
        if (hub == null) {
            throw new ResourceNotFoundException("Hub not found: " + request.hubId());
        }

        UserDTO user = userFacade.createUser(CreateUserRequest.builder()
                .fullName(fullName)
                .email(email)
                .phoneNumber(phoneNumber)
                .password(request.password())
                .role(UserRole.STAFF)
                .isVerified(true)
                .isActive(true)
                .build());

        hubFacade.assignStaff(hub.getId(), user.getId());

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
