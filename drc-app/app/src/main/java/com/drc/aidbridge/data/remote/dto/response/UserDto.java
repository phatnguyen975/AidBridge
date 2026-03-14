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

    @SerializedName("role")
    private String role;

    @Nullable
    @SerializedName("avatarUrl")
    private String avatarUrl;

    public UserDto() {
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

    public String getRole() {
        return role;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }
}
