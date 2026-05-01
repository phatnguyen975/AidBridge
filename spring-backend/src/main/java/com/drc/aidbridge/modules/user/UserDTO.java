package com.drc.aidbridge.modules.user;

import com.drc.aidbridge.modules.shared.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private boolean isVerified;
    private boolean isActive;
    private String fcmToken;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;

    // Backward compatibility for other modules
    public String getName() {
        return fullName;
    }

    public String getPhone() {
        return phoneNumber;
    }
}
