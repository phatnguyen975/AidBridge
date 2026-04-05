package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class VolunteerUserDto {

    @Nullable
    @SerializedName("name")
    private String name;

    @Nullable
    public String getName() {
        return name;
    }
}
