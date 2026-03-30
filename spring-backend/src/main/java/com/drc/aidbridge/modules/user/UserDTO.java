package com.drc.aidbridge.modules.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private boolean isVerified;
}
