package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class CurrentMissionResponseDto {

    @SerializedName("success")
    private boolean success;

    @Nullable
    @SerializedName("message")
    private String message;

    @Nullable
    @SerializedName("data")
    private MissionHistoryFullItemDto data;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public MissionHistoryFullItemDto getData() {
        return data;
    }
}
