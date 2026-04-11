package com.drc.aidbridge.data.remote.dto.response.volunteer;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VolunteerHistoryResponseDto {

    @SerializedName("success")
    private boolean success;

    @Nullable
    @SerializedName("message")
    private String message;

    @Nullable
    @SerializedName("data")
    private List<VolunteerHistoryItemDto> data;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public List<VolunteerHistoryItemDto> getData() {
        return data;
    }
}
