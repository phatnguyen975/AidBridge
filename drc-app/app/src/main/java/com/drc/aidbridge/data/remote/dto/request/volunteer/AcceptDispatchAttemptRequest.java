package com.drc.aidbridge.data.remote.dto.request.volunteer;

import com.google.gson.annotations.SerializedName;

public class AcceptDispatchAttemptRequest {

    @SerializedName("dispatchAttemptId")
    private String dispatchAttemptId;

    public AcceptDispatchAttemptRequest(String dispatchAttemptId) {
        this.dispatchAttemptId = dispatchAttemptId;
    }

    public String getDispatchAttemptId() {
        return dispatchAttemptId;
    }

    public void setDispatchAttemptId(String dispatchAttemptId) {
        this.dispatchAttemptId = dispatchAttemptId;
    }
}
