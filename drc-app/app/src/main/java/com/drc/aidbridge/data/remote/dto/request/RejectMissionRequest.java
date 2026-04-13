package com.drc.aidbridge.data.remote.dto.request;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class RejectMissionRequest {

    @SerializedName("dispatchAttemptId")
    private final String dispatchAttemptId;

    @SerializedName("reason")
    private final String reason;

    @Nullable
    @SerializedName("reasonDetail")
    private final String reasonDetail;

    public RejectMissionRequest(String dispatchAttemptId, String reason, @Nullable String reasonDetail) {
        this.dispatchAttemptId = dispatchAttemptId;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
    }

    public String getDispatchAttemptId() {
        return dispatchAttemptId;
    }

    public String getReason() {
        return reason;
    }

    @Nullable
    public String getReasonDetail() {
        return reasonDetail;
    }
}
