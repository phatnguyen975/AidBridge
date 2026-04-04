package com.drc.aidbridge.modules.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/*
    Flow:
    VerifyOtpUseCase -> volunteer listener
                     -> sponsor listener
                     -> victim listener
*/
@Getter
@Builder
@AllArgsConstructor
public class UserRoleCreatedEvent {
    private final String role;
    private final String userId;
}
