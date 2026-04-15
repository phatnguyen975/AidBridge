package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class VolunteerHistoryItemDto {

    @Nullable
    @SerializedName("missionType")
    private String missionType;

    @Nullable
    @SerializedName("completedAt")
    private String completedAt;

    @Nullable
    @SerializedName("address")
    private String address;

    @Nullable
    public String getMissionType() {
        return missionType;
    }

    @Nullable
    public String getCompletedAt() {
        return completedAt;
    }

    @Nullable
    public String getAddress() {
        return address;
    }
}
