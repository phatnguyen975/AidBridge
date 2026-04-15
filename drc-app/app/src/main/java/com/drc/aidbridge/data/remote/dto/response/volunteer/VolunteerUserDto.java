package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class VolunteerUserDto {

    @Nullable
    @SerializedName("name")
    private String name;

    @Nullable
    @SerializedName(value = "phone")
    private String phoneNumber;

    @Nullable
    @SerializedName("email")
    private String email;

    @Nullable
    @SerializedName(value = "avatarUrl", alternate = { "avatar_url", "avatar" })
    private String avatarUrl;

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }
}
