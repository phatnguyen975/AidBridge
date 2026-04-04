package com.drc.aidbridge.data.remote.dto.response;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class UserDto {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @Nullable
    @SerializedName("address")
    private String address;

    @SerializedName("role")
    private String role;

    @Nullable
    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("verified")
    private boolean verified;

    public UserDto() {
    }

    public UserDto(String id,
                   String name,
                   String email,
                   String phone,
                   @Nullable String address,
                   String role,
                   @Nullable String avatarUrl,
                   boolean verified) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.verified = verified;
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

    @Nullable
    public String getAddress() {
        return address;
    }

    public String getRole() {
        return role;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isVerified() {
        return verified;
    }
}
