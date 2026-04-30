package com.drc.aidbridge.data.remote.dto.request.volunteer;

import com.google.gson.annotations.SerializedName;

public class CancelMissionRequestDto {

    @SerializedName("missionId")
    private String missionId;

    @SerializedName("cancellationReason")
    private String cancellationReason;

    public CancelMissionRequestDto(String missionId, String cancellationReason) {
        this.missionId = missionId;
        this.cancellationReason = cancellationReason;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
