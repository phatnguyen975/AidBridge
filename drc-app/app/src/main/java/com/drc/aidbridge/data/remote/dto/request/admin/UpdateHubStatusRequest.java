package com.drc.aidbridge.data.remote.dto.request.admin;

import com.google.gson.annotations.SerializedName;

public class UpdateHubStatusRequest {

    @SerializedName("status")
    private final String status;

    public UpdateHubStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
