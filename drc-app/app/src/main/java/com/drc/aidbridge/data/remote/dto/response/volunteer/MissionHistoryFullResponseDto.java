package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class MissionHistoryFullResponseDto {
    @SerializedName("success")
    private boolean success;

    @Nullable
    @SerializedName("message")
    private String message;

    @Nullable
    @SerializedName("data")
    private MissionHistoryFullDataDto data;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public MissionHistoryFullDataDto getData() {
        return data;
    }
}
