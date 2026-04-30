package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;
import com.drc.aidbridge.data.remote.dto.response.MissionDto;
import com.google.gson.annotations.SerializedName;

public class CancelMissionResponseDto {

    @SerializedName("success")
    private boolean success;

    @Nullable
    @SerializedName("message")
    private String message;

    @Nullable
    @SerializedName("data")
    private MissionDto data;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public MissionDto getData() {
        return data;
    }
}
