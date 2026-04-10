package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class VolunteerHistoryItemDto {

    @Nullable
    @SerializedName("missionId")
    private String missionId;

    @Nullable
    @SerializedName("type")
    private String type;

    @Nullable
    @SerializedName("completedAt")
    private String completedAt;

    @Nullable
    @SerializedName("location")
    private String location;

    @Nullable
    public String getMissionId() {
        return missionId;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getCompletedAt() {
        return completedAt;
    }

    @Nullable
    public String getLocation() {
        return location;
    }
}
