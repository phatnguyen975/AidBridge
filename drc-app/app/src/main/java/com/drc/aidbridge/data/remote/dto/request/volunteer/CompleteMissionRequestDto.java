package com.drc.aidbridge.data.remote.dto.request.volunteer;

import com.google.gson.annotations.SerializedName;

public class CompleteMissionRequestDto {

    @SerializedName("missionId")
    private String missionId;

    @SerializedName("notes")
    private String notes;

    public CompleteMissionRequestDto(String missionId, String notes) {
        this.missionId = missionId;
        this.notes = notes;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
