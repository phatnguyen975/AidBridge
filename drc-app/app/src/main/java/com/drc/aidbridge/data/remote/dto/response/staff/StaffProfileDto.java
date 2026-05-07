package com.drc.aidbridge.data.remote.dto.response.staff;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class StaffProfileDto {

    @Nullable
    @SerializedName("id")
    private String id;

    @Nullable
    @SerializedName(value = "user_id", alternate = {"userId"})
    private String userId;

    @Nullable
    @SerializedName(value = "start_date", alternate = {"startDate"})
    private String startDate;

    @Nullable
    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;

    @Nullable
    @SerializedName(value = "updated_at", alternate = {"updatedAt"})
    private String updatedAt;

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getStartDate() {
        return startDate;
    }

    @Nullable
    public String getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public String getUpdatedAt() {
        return updatedAt;
    }
}
