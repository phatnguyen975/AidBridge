package com.drc.aidbridge.domain.model;

import com.drc.aidbridge.domain.enums.UserRole;

/**
 * User — domain model representing an authenticated user.
 *
 * This is a pure data class (no Android or framework dependencies) used
 * in the domain and UI layers. The data layer maps UserDto → User via a mapper.
 */
public class User {

    private final String id;
    private final String name;
    private final String email;
    private final String phone;
    private final UserRole role;
    private final String avatarUrl;

    public User(String id, String name, String email, String phone, UserRole role, String avatarUrl) {
        this.id    = id;
        this.name  = name;
        this.email = email;
        this.phone = phone;
        this.role  = role;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }
}
