package com.drc.aidbridge.modules.user;

import com.drc.aidbridge.modules.shared.enums.UserRole;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        String fullName,
        String email,
        String phoneNumber,
        String password,
        UserRole role,
        boolean isVerified,
        boolean isActive
) {
}
