package com.drc.aidbridge.data.remote.dto.response.victim;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class SosRequestResponse {

    @Nullable
    @SerializedName("id")
    private String id;

    @Nullable
    public String getId() {
        return id;
    }
}
