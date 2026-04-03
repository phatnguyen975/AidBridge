package com.drc.aidbridge.data.remote.dto.response;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class AvatarUploadResponse {

    @Nullable
    @SerializedName("avatarUrl")
    private String avatarUrl;

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }
}
