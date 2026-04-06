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
}
