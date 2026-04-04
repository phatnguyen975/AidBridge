package com.drc.aidbridge.modules.user;



import lombok.Data;
import lombok.AllArgsConstructor;
@Data
@AllArgsConstructor

/*
    Flow:
    VerifyOtpUseCase -> volunteer listener
                     -> sponsor listener
                     -> victim listener
*/
public class UserRoleCreatedEvent {
    private final String role;
    private final String userId;
}
